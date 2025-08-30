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

public class DetermineEvoLevels extends RomHandlerTest{

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void findAverageEvoStrength(String romName) {
        loadROM(romName);
        SpeciesSet allSpecies = romHandler.getSpeciesSet();

        List<Species> levelUpEvos = new ArrayList<>();
        List<Species> nonLevelUpEvos = new ArrayList<>();
        findLevelUpAndNonLevelUpEvos(allSpecies, levelUpEvos, nonLevelUpEvos);

        int[] levelUpBSTaverage = new int[100];
        int[] levelUpBSTmin = new int[100];
        int[] levelUpBSTmax = new int[100];
        int latestLevelUpEvo = writeBSTlists(levelUpEvos, levelUpBSTaverage, levelUpBSTmin, levelUpBSTmax);
        // TODO likely not good enough, get list of evo triples (BSTold, BSTnew, evo level) instead for all levelUpEvos and work with that somehow

        System.out.println(romName + " - latest level up evolution: " + latestLevelUpEvo);
        System.out.println("minimum BST at level: " + Arrays.toString(levelUpBSTmin));
        System.out.println("average BST at level: " + Arrays.toString(levelUpBSTaverage));
        System.out.println("maximum BST at level: " + Arrays.toString(levelUpBSTmax));
        for (Species pk : nonLevelUpEvos) {
            int evoLevelFirstStage = 0;
            if (pk.getEvolutionsFrom().get(0).getType().usesLevel()) { // evolve first if level up evo
                String pkOldName = pk.getName();
                evoLevelFirstStage = pk.getEvolutionsFrom().get(0).getExtraInfo();
                pk = pk.getEvolutionsFrom().get(0).getTo();
                System.out.println(pkOldName + " --> " + pk.getName() + " (level up evo at level " + evoLevelFirstStage + ")");
            }
            int bstPk = pk.getHp() + pk.getAttack() + pk.getDefense() + pk.getSpatk() + pk.getSpdef()
                    + pk.getSpeed() + pk.getSpecial();
            for (Evolution evo : pk.getEvolutionsFrom()) {
                Species evoOfPk = evo.getTo();
                int bstEvoOfPk = evoOfPk.getHp() + evoOfPk.getAttack() + evoOfPk.getDefense() + evoOfPk.getSpatk()
                        + evoOfPk.getSpdef() + evoOfPk.getSpeed() + evoOfPk.getSpecial();
                boolean evoBeforeLatestLvl = false;
                for (int m = evoLevelFirstStage; m < latestLevelUpEvo; m++) {
                    // TODO the following is the thing that has to be figured out
                    if (bstEvoOfPk <= levelUpBSTmax[m] && bstPk <= levelUpBSTaverage[m] ) {//&& bstPk <= levelUpBSTmin[m]) {
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

    private void findLevelUpAndNonLevelUpEvos(SpeciesSet allSpecies, List<Species> levelUpEvos, List<Species> nonLevelUpEvos) {
        for (Species pk : allSpecies) {
            if (!pk.getEvolutionsTo().isEmpty() || // Skip if not base stage
                    pk.getEvolutionsFrom().isEmpty()) {
                continue;
            } // Skip if it does not have an evolution
            List<Evolution> evolutions = pk.getEvolutionsFrom();
            boolean isNotLevelEvo = false;
            for (Evolution evo : evolutions) {
                if (!evo.getType().usesLevel()) {
                    isNotLevelEvo = true;
                    break;
                } else {
                    for (Evolution secondEvo : evo.getTo().getEvolutionsFrom()) {
                        if (!secondEvo.getType().usesLevel()) {
                            isNotLevelEvo = true;
                            break;
                        }
                    }
                }
            }
            if (isNotLevelEvo) {
                nonLevelUpEvos.add(pk);
            } else {
                levelUpEvos.add(pk);
            }
        }
    }

    private int writeBSTlists(List<Species> levelUpEvos, int[] levelUpBSTaverage, int[] levelUpBSTmin, int[] levelUpBSTmax) {
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
                    if (bst < levelUpBSTmin[j] || levelUpBSTmin[j] == 0) {
                        levelUpBSTmin[j] = bst;
                    }
                    if (bst > levelUpBSTmax[j] || levelUpBSTmax[j] == 0) {
                        levelUpBSTmax[j] = bst;
                    }
                    sumBSTs += bst;
                }
                levelUpBSTaverage[j] = sumBSTs / levelUpEvos.size();
            } else {
                levelUpBSTaverage[j] = levelUpBSTaverage[j - 1];
                levelUpBSTmin[j] = levelUpBSTmin[j - 1];
                levelUpBSTmax[j] = levelUpBSTmax[j - 1];
            }
            atLeastOneEvoHappend = false;
        }
        return latestLevelUpEvo;
    }
}

