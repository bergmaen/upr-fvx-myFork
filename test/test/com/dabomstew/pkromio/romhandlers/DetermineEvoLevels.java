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

        List<Evolution> levelUpEvos = new ArrayList<>();
        List<Evolution> nonLevelUpEvos = new ArrayList<>();
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

        getNonLevelUpEvoLevels(nonLevelUpEvos, preEvo2postEvo);

        long end = System.nanoTime();
        System.out.println("Elapsed time: " + (end - start) / 1_000_000 + " ms");
    }

    private void findLevelUpAndNonLevelUpEvos(SpeciesSet allSpecies, List<Evolution> levelUpEvos, List<Evolution> nonLevelUpEvos) {
        for (Species pk : allSpecies) {
            for (Evolution evoTo : pk.getEvolutionsTo()) {
                if (evoTo.getType().usesLevel()) {
                    levelUpEvos.add(evoTo);
                } else {
                    nonLevelUpEvos.add(evoTo);
                }
            }
        }
    }

    private int getLevelUpTriplets(List<Evolution> levelUpEvos, List<int[]> preEvo2postEvo) {
        int latestLevelUpEvo = 0;
        for (Evolution evo : levelUpEvos) {
            int evoLevel = evo.getExtraInfo();
            int[] triplet = {getBST(evo.getFrom()), getBST(evo.getTo()), evoLevel};
            preEvo2postEvo.add(triplet);
            latestLevelUpEvo = Math.max(evoLevel, latestLevelUpEvo);
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

    private static void getNonLevelUpEvoLevels(List<Evolution> nonLevelUpEvos, List<int[]> preEvo2postEvo) {
        for (Evolution evo : nonLevelUpEvos) {
            int evoLevelFirstStage = 0;
            // Handle if pre evo is level up evolution of another Pokemon exists and is level-up evo
            List<Evolution> previousEvo = evo.getFrom().getEvolutionsTo();
            if (!previousEvo.isEmpty() && previousEvo.get(0).getType().usesLevel()) {
                evoLevelFirstStage = previousEvo.get(0).getExtraInfo();
                System.out.print(previousEvo.get(0).getFrom().getName() + " --(Lv" + evoLevelFirstStage + ")--> ");
            }
            getEvoLevelsUsingTriplet(evo, evoLevelFirstStage, preEvo2postEvo);
        }
    }

    private static void getEvoLevelsUsingTriplet(Evolution evo, int evoLevelFirstStage, List<int[]> preEvo2postEvo) {
        Species pk = evo.getFrom();
        int bstPk = getBST(pk);
        Species evoOfPk = evo.getTo();
        int bstEvoOfPk = getBST(evoOfPk);

        int chosenLevel = findEvolutionLevel(preEvo2postEvo, bstPk, bstEvoOfPk);

        if (!pk.getEvolutionsTo().isEmpty()) {
            Evolution evoToPk = pk.getEvolutionsTo().get(0);
            if (evoToPk.getType().usesLevel()) {
                chosenLevel = Math.max(chosenLevel, (int) Math.ceil(1.25 * evoToPk.getExtraInfo()));
            }
        }

        System.out.println(pk.getName() + " (BST: " + bstPk + ") --> "
                + evoOfPk.getName() + " (BST: " + bstEvoOfPk + ")   AT LEVEL   " + chosenLevel);
    }

    public static int findEvolutionLevel(List<int[]> samples, int targetPreBST, int targetPostBST) {

        // ==== CONFIGURATION PARAMETERS ====
        double p = 1;                // distance weighting exponent: 1/d^p
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
            double scaledPre = Math.pow((double) targetPreBST / samplePre, preFactor);
            double scaledPost = Math.pow((double) targetPostBST / samplePost, postFactor);

            double adjustedLevel = sampleLevel * (scaledPre + scaledPost) / 2.0;

            weightedSum += adjustedLevel * weight;
            weightSum += weight;
        }

        // Return weighted average
        return (int) Math.round(weightedSum / weightSum);
    }
}