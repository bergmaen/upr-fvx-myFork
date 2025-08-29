package test.com.dabomstew.pkromio.romhandlers;

import com.dabomstew.pkromio.constants.SpeciesIDs;
import com.dabomstew.pkromio.gamedata.*;
import com.dabomstew.pkromio.romhandlers.AbstractGBRomHandler;
import com.dabomstew.pkromio.romhandlers.AbstractRomHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class RomHandlerEvolutionTest extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void findAverageEvoStrength(String romName) {
        loadROM(romName);
        boolean hasEvolutions = false;
        SpeciesSet allSpecies = romHandler.getSpeciesSet();
        List<Species> levelUpEvos = new ArrayList<>();
        List<Species> nonLevelUpEvos = new ArrayList<>();

        for (Species pk : allSpecies) {
            if (!pk.getEvolutionsTo().isEmpty() || // Skip if not base stage
                    pk.getEvolutionsFrom().isEmpty()) {continue;} // Skip if it does not have an evolution
            List<Evolution> evolutions = pk.getEvolutionsFrom();
            boolean isNotLevelEvo = false;
            for (Evolution evo : evolutions) {
                if (!evo.getType().usesLevel()) {
                    isNotLevelEvo = true;
                    break;
                }
                else {
                    for (Evolution secondEvo : evo.getTo().getEvolutionsFrom()) {
                        if (!secondEvo.getType().usesLevel()) {
                            isNotLevelEvo = true;
                            break;
                        }
                    }
                }
            }
            if (isNotLevelEvo) {nonLevelUpEvos.add(pk);}
            else {levelUpEvos.add(pk);}
        }

        int[] levelUpBSTaverage = new int[100];
        int[] levelUpBSTmin = new int[100];
        int[] levelUpBSTmax = new int[100];
        int latestLevelUpEvo = 0;
        boolean atLeastOneEvoHappend = true; // first time, update bst arrays
        for (int j = 0; j < levelUpBSTaverage.length; j++) {
            boolean evolutionHappened = true;
            while (evolutionHappened) {
                evolutionHappened = false;
                // Evolve every level up evo as far as the level allows
                for (int k = 0; k < levelUpEvos.size(); k++) {
                    Species pk = levelUpEvos.get(k);
                    if (!pk.getEvolutionsFrom().isEmpty() && // has evolution
                            pk.getEvolutionsFrom().get(0).getExtraInfo() <= j + 1) { // currently analyzed level (j+1) is at least evo level
                        if (pk.getEvolutionsFrom().get(0).getExtraInfo() > latestLevelUpEvo) {
                            latestLevelUpEvo = pk.getEvolutionsFrom().get(0).getExtraInfo();
                        }
                        levelUpEvos.set(k, pk.getEvolutionsFrom().get(0).getTo());
                        evolutionHappened = true;
                        atLeastOneEvoHappend = true;
                    }
                }
            }
            if (atLeastOneEvoHappend) { // will enter this for j==0 always
                int sumBSTs = 0;
                for (int l = 0; l < levelUpEvos.size(); l++) {
                    Species pk = levelUpEvos.get(l);
                    int bst = pk.getHp() + pk.getAttack() + pk.getDefense() + pk.getSpatk() + pk.getSpdef()
                            + pk.getSpeed() + pk.getSpecial();
                    if (bst < levelUpBSTmin[j] || levelUpBSTmin[j] == 0) {levelUpBSTmin[j] = bst;}
                    if (bst > levelUpBSTmax[j] || levelUpBSTmax[j] == 0) {levelUpBSTmax[j] = bst;}
                    sumBSTs += bst;
                }
                levelUpBSTaverage[j] = sumBSTs/levelUpEvos.size();
            } else {
                levelUpBSTaverage[j] = levelUpBSTaverage[j - 1];
                levelUpBSTmin[j] = levelUpBSTmin[j - 1];
                levelUpBSTmax[j] = levelUpBSTmax[j - 1];
            }
            atLeastOneEvoHappend = false;
        }
        System.out.println(romName + " - latest level up evolution: " + latestLevelUpEvo);
        System.out.println("minimum BST at level: " + Arrays.toString(levelUpBSTmin));
        System.out.println("average BST at level: " + Arrays.toString(levelUpBSTaverage));
        System.out.println("maximum BST at level: " + Arrays.toString(levelUpBSTmax));
        for (Species pk : nonLevelUpEvos) {
            int bstPk = pk.getHp() + pk.getAttack() + pk.getDefense() + pk.getSpatk() + pk.getSpdef()
                    + pk.getSpeed() + pk.getSpecial();
            for (Evolution evo : pk.getEvolutionsFrom()) {
                Species evoOfPk = evo.getTo();
                int bstEvoOfPk = evoOfPk.getHp() + evoOfPk.getAttack() + evoOfPk.getDefense() + evoOfPk.getSpatk()
                        + evoOfPk.getSpdef() + evoOfPk.getSpeed() + evoOfPk.getSpecial();
                boolean evoBeforeLatestLvl = false;
                // TODO at this point, first get the level if there is a level up evo first and start int m = level - 1 and adapt the msg for the whole line
                for (int m = 0; m < latestLevelUpEvo; m++) {
                    // TODO the following is the thing that has to be figured out
                    if (bstEvoOfPk <= levelUpBSTmax[m] && bstPk <= levelUpBSTaverage[m] && bstPk <= levelUpBSTmin[m]) {
                        System.out.println(pk.getName() + " (BST: " + bstPk + ") --> "
                                + evoOfPk.getName() + " (BST: " + bstEvoOfPk + ") at level " + (m + 1));
                        evoBeforeLatestLvl = true;
                        break;
                    }
                }
                if (!evoBeforeLatestLvl) {
                    System.out.println(pk.getName() + " (BST: " + bstPk + ") --> "
                            + evoOfPk.getName() + " (BST: " + bstEvoOfPk + ") at latest evo level " + latestLevelUpEvo);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void speciesHaveEvolutions(String romName) {
        loadROM(romName);
        boolean hasEvolutions = false;
        for (Species pk : romHandler.getSpeciesSet()) {
            if (!pk.getEvolutionsFrom().isEmpty()) {
                hasEvolutions = true;
                break;
            }
        }
        assertTrue(hasEvolutions);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void noEvolutionUsesEvoTypeNone(String romName) {
        loadROM(romName);
        for (Species pk : romHandler.getSpeciesSet()) {
            System.out.println(pk.getFullName());
            for (Evolution evo : pk.getEvolutionsFrom()) {
                System.out.println(evo);
                assertNotEquals(EvolutionType.NONE, evo.getType());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void noSpeciesHasDuplicateEvolutions(String romName) {
        // The games actually allow this internally,
        // e.g. Feebas evolves into Milotic using both beauty and prism scale+trade.
        // For now the Randomizer doesn't play well with that though,
        // so we expect the RomHandlers to remove duplicate Evolutions.
        loadROM(romName);

        Set<Species> withDuplicateEvos = new HashSet<>();
        for (Species pk : romHandler.getSpeciesSetInclFormes()) {
            Set<Species> evolved = new HashSet<>();
            System.out.println(pk.getEvolutionsFrom());
            for (Evolution evo : pk.getEvolutionsFrom()) {
                // LEVEL_FEMALE_ESPURR is an exception since it implies a forme difference
                if (evolved.contains(evo.getTo()) && evo.getType() != EvolutionType.LEVEL_FEMALE_ESPURR) {
                    withDuplicateEvos.add(pk);
                }
                evolved.add(evo.getTo());
            }
        }

        System.out.println("------");
        withDuplicateEvos.forEach(pk -> System.out.println(pk.getEvolutionsFrom()));
        assertTrue(withDuplicateEvos.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void evolutionsDoNotChangeWithSaveAndLoad(String romName) {
        loadROM(romName);

        Map<Species, List<Evolution>> evosToBefore = new HashMap<>();
        Map<Species, List<Evolution>> evosFromBefore = new HashMap<>();

        for (Species pk : romHandler.getSpeciesSetInclFormes()) {
            evosToBefore.put(pk, pk.getEvolutionsTo().stream().map(Evolution::new).collect(Collectors.toList()));
            evosFromBefore.put(pk, pk.getEvolutionsFrom().stream().map(Evolution::new).collect(Collectors.toList()));
        }

        ((AbstractRomHandler) romHandler).savePokemonStats();
        ((AbstractRomHandler) romHandler).loadPokemonStats();
        if (romHandler instanceof AbstractGBRomHandler) {
            // TODO: not pleasant that GB romhandler load evolutions separately from stats,
            //  when they don't even save them separately.
            ((AbstractGBRomHandler) romHandler).loadEvolutions();
        }

        for (Species pk : romHandler.getSpeciesSetInclFormes()) {
            List<Evolution> toBefore = evosToBefore.get(pk);
            List<Evolution> fromBefore = evosFromBefore.get(pk);
            List<Evolution> toAfter = pk.getEvolutionsTo();
            List<Evolution> fromAfter = pk.getEvolutionsFrom();

            System.out.println(pk.getFullName());
            System.out.println("Evos To");
            System.out.println("Before: " + toBefore);
            System.out.println("After: " + toAfter);
            assertEquals(toBefore, toAfter);
            System.out.println("Evos From");
            System.out.println("Before: " + fromBefore);
            System.out.println("After: " + fromAfter);
            assertEquals(fromBefore, fromAfter);
            System.out.println();
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void allSpeciesCanBeGivenExactlyOneEvolutionAndSaved(String romName) {
        // for testing whether "Random Every Level" evolutions would work
        loadROM(romName);
        assumeTrue(romHandler.canGiveEverySpeciesOneEvolutionEach());
        Species universalTo = romHandler.getSpecies().get(SpeciesIDs.krabby); // lol
        universalTo.getEvolutionsTo().clear();
        for (Species pk : romHandler.getSpecies()) {
            if (pk == null || pk == universalTo) {
                continue;
            }
            pk.getEvolutionsTo().clear();
            pk.getEvolutionsFrom().clear();
            // evolution type and extra should not matter
            Evolution evo = new Evolution(pk, universalTo, EvolutionType.LEVEL, 1);
            pk.getEvolutionsFrom().add(evo);
            universalTo.getEvolutionsTo().add(evo);
        }
        ((AbstractRomHandler) romHandler).savePokemonStats();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void evolutionsHaveSensibleItems(String romName) {
        loadROM(romName);

        List<Item> allItems = romHandler.getItems();
        Set<Item> evolutionItems = romHandler.getEvolutionItems();

        for (Species pk : romHandler.getSpeciesSetInclFormes()) {
            for (Evolution evo : pk.getEvolutionsFrom()) {
                // In Gens 2+3, TRADE_ITEM items are not counted as evo items,
                // as the player is not expected to trade.
                // The player is not expected to trade in other games either,
                // but as Gen 4 introduces LEVEL_ITEM, TRADE_ITEM items
                // become relevant evo items within that context.
                if (evo.getType().usesItem() &&
                        !(romHandler.generationOfPokemon() < 4 && evo.getType() == EvolutionType.TRADE_ITEM)) {
                    System.out.println(evo);
                    Item evoItem = allItems.get(evo.getExtraInfo());
                    System.out.println(evoItem.getName());
                    assertTrue(evolutionItems.contains(evoItem));
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void removeTimeEvosGivesSensibleEvoItems(String romName) {
        loadROM(romName);

        romHandler.removeTimeBasedEvolutions();

        List<Item> allItems = romHandler.getItems();
        Set<Item> evolutionItems = romHandler.getEvolutionItems();

        for (Species pk : romHandler.getSpeciesSetInclFormes()) {
            for (Evolution evo : pk.getEvolutionsFrom()) {
                // In Gens 2+3, TRADE_ITEM items are not counted as evo items,
                // as the player is not expected to trade.
                // The player is not expected to trade in other games either,
                // but as Gen 4 introduces LEVEL_ITEM, TRADE_ITEM items
                // become relevant evo items within that context.
                if (evo.getType().usesItem() &&
                        !(romHandler.generationOfPokemon() < 4 && evo.getType() == EvolutionType.TRADE_ITEM)) {
                    System.out.println(evo);
                    Item evoItem = allItems.get(evo.getExtraInfo());
                    System.out.println(evoItem.getName());
                    assertTrue(evolutionItems.contains(evoItem));
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void printAllEvoTypesByUsage(String romName) {
        // not really a test since it makes no assertions, but still useful when debugging
        loadROM(romName);

        Map<EvolutionType, List<Evolution>> allEvos = new EnumMap<>(EvolutionType.class);
        for (EvolutionType et : EvolutionType.values()) {
            allEvos.put(et, new ArrayList<>());
        }

        for (Species pk : romHandler.getSpeciesSetInclFormes()) {
            for (Evolution evo : pk.getEvolutionsFrom()) {
                allEvos.get(evo.getType()).add(evo);
            }
        }

        for (Map.Entry<EvolutionType, List<Evolution>> entry : allEvos.entrySet()) {
            System.out.println(entry.getValue().size() + "\t" + entry.getKey());
            for (Evolution evo : entry.getValue()) {
                System.out.println("\t" + evo);
            }
        }
    }
}
