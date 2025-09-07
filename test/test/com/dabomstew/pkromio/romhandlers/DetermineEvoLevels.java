package test.com.dabomstew.pkromio.romhandlers;

import com.dabomstew.pkromio.gamedata.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

public class DetermineEvoLevels extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void findAverageEvoStrength(String romName) {
        loadROM(romName);
        long start = System.nanoTime();
        SpeciesSet allSpecies = romHandler.getSpeciesSet();

        List<Species> levelUpEvos = new ArrayList<>();
        List<Species> nonLevelUpEvos = new ArrayList<>();
        findLevelUpAndNonLevelUpEvos(allSpecies, levelUpEvos, nonLevelUpEvos);

        List<int[]> preEvo2postEvo = new ArrayList<>();
        int latestLevelUpEvo = getLevelUpTriplets(levelUpEvos, preEvo2postEvo);

        // Display the level-up triplets (sorted for better readability)
        preEvo2postEvo.sort(Comparator.comparingInt(r->r[0]));
        System.out.println();
        System.out.print("=====================================================================================");
        System.out.println("=====================================================================================");
        System.out.println();
        System.out.println(romName + " - latest level up evolution: " + latestLevelUpEvo);
        System.out.println();
        System.out.println("All level-up evolutions [preEvoBST, postEvoBST, evoLevel]:");
        printRowList(preEvo2postEvo);
        System.out.println();

        getNonLevelUpEvoLevels(nonLevelUpEvos, latestLevelUpEvo, preEvo2postEvo);
        long end = System.nanoTime();
        System.out.println("Elapsed time: " + (end - start) / 1_000_000 + " ms");
    }

    private void findLevelUpAndNonLevelUpEvos(SpeciesSet allSpecies, List<Species> levelUpEvos, List<Species> nonLevelUpEvos) {
        // TODO if split evos where one line gets to the end with only level-up, count that path as level up
        // make the lists evolution centric (List<Evolution>)
        for (Species pk : allSpecies) {
            if (!pk.getEvolutionsTo().isEmpty() || // Skip if not base stage
                    pk.getEvolutionsFrom().isEmpty()) { // Skip if it does not have an evolution
                continue;
            }
            List<Evolution> evolutions = pk.getEvolutionsFrom();
            boolean isNotLevelEvo = false;
            for (Evolution evo : evolutions) {
                if (!evo.getType().usesLevel()) {
                    isNotLevelEvo = true;
                } else {
                    for (Evolution secondEvo : evo.getTo().getEvolutionsFrom()) {
                        if (!secondEvo.getType().usesLevel()) {
                            isNotLevelEvo = true;
                        } else {
                            isNotLevelEvo = false;
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

    private int getLevelUpTriplets(List<Species> levelUpEvos, List<int[]> preEvo2postEvo) {
        int latestLevelUpEvo = 0;
        for (Species pk : levelUpEvos) {
            for (Evolution evo : pk.getEvolutionsFrom()) {
                Species evoOfPk = evo.getTo();
                int evoLevel = evo.getExtraInfo();
                int[] triplet = {getBST(pk), getBST(evoOfPk), evoLevel};
                preEvo2postEvo.add(triplet);
                // Handle potential second evolution
                if (!evoOfPk.getEvolutionsFrom().isEmpty()) {
                    latestLevelUpEvo = Math.max(latestLevelUpEvo, getLevelUpTriplets(Collections.singletonList(evoOfPk), preEvo2postEvo));
                }
                latestLevelUpEvo = Math.max(evoLevel, latestLevelUpEvo);
            }
        }
        return latestLevelUpEvo;
    }

    private static int getBST(Species pk) {
        return pk.getHp() + pk.getAttack() + pk.getDefense() + pk.getSpatk() + pk.getSpdef() + pk.getSpeed() + pk.getSpecial();
    }

    private static void printRowList(List<int[]> rowList) {
        for (int k = 0; k < rowList.size(); k++) {
            int[] row = rowList.get(k);
            System.out.print(Arrays.toString(row) + " ");
            if ((k + 1) % 10 == 0) System.out.println();
        }
        System.out.println();
    }

    private static void getNonLevelUpEvoLevels(List<Species> nonLevelUpEvos, int latestLevelUpEvo, List<int[]> preEvo2postEvo) {
        for (Species pk : nonLevelUpEvos) {
            for (Evolution firstEvo : pk.getEvolutionsFrom()) {
                int evoLevelFirstStage = 0;
                // Handle if first evo is level-up evo
                if (firstEvo.getType().usesLevel()) {
                    evoLevelFirstStage = firstEvo.getExtraInfo();
                    String pkOldName = pk.getName();
                    pk = firstEvo.getTo();
                    System.out.print(pkOldName + " --(Lv" + evoLevelFirstStage + ")--> ");
                    getEvoLevelsUsingTriplet(pk, pk.getEvolutionsFrom(), evoLevelFirstStage, latestLevelUpEvo, preEvo2postEvo);
                } else {
                    getEvoLevelsUsingTriplet(pk, Collections.singletonList(firstEvo), evoLevelFirstStage, latestLevelUpEvo, preEvo2postEvo);
                }
            }
        }
    }

    private static void getEvoLevelsUsingTriplet(Species pk, List<Evolution> evosToCheck, int evoLevelFirstStage,
                                                 int latestLevelUpEvo, List<int[]> preEvo2postEvo) {
        int bstPk = getBST(pk);

        for (Evolution evo : evosToCheck) {
            Species evoOfPk = evo.getTo();
            int bstEvoOfPk = getBST(evoOfPk);
            int chosenLevel = findEvolutionLevel(preEvo2postEvo, bstPk, bstEvoOfPk);

            System.out.println(pk.getName() + " (BST: " + bstPk + ") --> "
                    + evoOfPk.getName() + " (BST: " + bstEvoOfPk + ")   AT LEVEL   " + chosenLevel);

            // Handle possible second-stage evolution
            // TODO make such that is at least 25% higher than the first evo, e.g., Rhydon appears at 42, so Rhyperior shall only appear at level 52.5~=53
            for (Evolution evoOfEvo : evoOfPk.getEvolutionsFrom()) {
                Species evoOfEvoOfPk = evoOfEvo.getTo();
                int bstEvoOfEvoOfPk = getBST(evoOfEvoOfPk);

                chosenLevel = findEvolutionLevel(preEvo2postEvo, bstEvoOfPk, bstEvoOfEvoOfPk);

                System.out.println(evoOfPk.getName() + " (BST: " + bstEvoOfPk + ") --> "
                        + evoOfEvoOfPk.getName() + " (BST: " + bstEvoOfEvoOfPk + ")   AT LEVEL   " + chosenLevel);
            }
        }
    }

    public static int findEvolutionLevel(List<int[]> samples, int targetPreBST, int targetPostBST) {

        // ==== CONFIGURATION PARAMETERS ====
        double p = 2;                // distance weighting exponent: 1/d^p
        double preFactor = 1.5;          // scaling factor for preBST
        double postFactor = 3;         // scaling factor for postBST
        double largeWeightForZero = 1; // weight to use if distance is zero
        // ==================================

        double weightedSum = 0.0;
        double weightSum = 0.0;

        for (int[] sample : samples) {
            int samplePre = sample[0];
            int samplePost = sample[1];
            int sampleLevel = sample[2];

            // Compute Euclidean distance
            double dx = targetPreBST - samplePre;
            double dy = targetPostBST - samplePost;
            double dist = Math.sqrt(dx * dx + dy * dy);

            // Weight = 1 / d^p, use largeWeightForZero if distance is zero
            double weight = dist == 0 ? largeWeightForZero : 1.0 / Math.pow(dist, p);

            // Pre/post scaling
//            double scaledPre = 1 + preFactor * ((double) targetPreBST / samplePre - 1);
//            double scaledPost = 1 + postFactor * ((double) targetPostBST / samplePost - 1);
            double scaledPre = Math.pow((double) targetPreBST / samplePre, preFactor);
            double scaledPost = Math.pow((double) targetPostBST / samplePost, postFactor);
//            double scaledTotal = Math.pow((double) (targetPreBST + targetPostBST) / (samplePre + samplePost), postFactor);

            double adjustedLevel = sampleLevel * (scaledPre + scaledPost) / 2.0;
//            double adjustedLevel = sampleLevel * scaledTotal;

            weightedSum += adjustedLevel * weight;
            weightSum += weight;
        }

        // Return weighted average
        return (int) Math.round(weightedSum / weightSum);
    }
}