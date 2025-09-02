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

public class DetermineEvoLevels extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void findAverageEvoStrength(String romName) {
        loadROM(romName);
        SpeciesSet allSpecies = romHandler.getSpeciesSet();

        List<Species> levelUpEvos = new ArrayList<>();
        List<Species> nonLevelUpEvos = new ArrayList<>();
        findLevelUpAndNonLevelUpEvos(allSpecies, levelUpEvos, nonLevelUpEvos);

        List<int[]> fromBST_toBST_evoLevel = new ArrayList<>();
        List<int[]> first2second = new ArrayList<>();
        List<int[]> second2final = new ArrayList<>();
        List<int[]> first2final = new ArrayList<>();
        int latestLevelUpEvo = getLevelUpTriplets(levelUpEvos, fromBST_toBST_evoLevel, first2second, second2final, first2final, 0);
        first2second.sort(Comparator.comparingInt(r->r[0]));
        second2final.sort(Comparator.comparingInt(r->r[0]));
        first2final.sort(Comparator.comparingInt(r->r[0]));
        fromBST_toBST_evoLevel.sort(Comparator.comparingInt(r->r[0]));

        int[] levelUpBSTaverage = new int[100];
        int[] levelUpBSTmin = new int[100];
        int[] levelUpBSTmax = new int[100];
        getLevelUpInformation(levelUpEvos, levelUpBSTaverage, levelUpBSTmin, levelUpBSTmax);

        System.out.println(romName + " - latest level up evolution: " + latestLevelUpEvo);
//        System.out.println("minimum BST at level: " + Arrays.toString(levelUpBSTmin));
//        System.out.println("average BST at level: " + Arrays.toString(levelUpBSTaverage));
//        System.out.println("maximum BST at level: " + Arrays.toString(levelUpBSTmax));
        System.out.println("First to second:");
        printRowList(first2second);
        System.out.println("Second to final:");
        printRowList(second2final);
        System.out.println("First to final:");
        printRowList(first2final);
        System.out.println("All:");
        printRowList(fromBST_toBST_evoLevel);

        for (Species pk : nonLevelUpEvos) {
            int evoLevelFirstStage = 0;

            // Handle first level-up evolution, if it exists
            Evolution firstEvo = pk.getEvolutionsFrom().get(0);
            if (firstEvo.getType().usesLevel()) {
                evoLevelFirstStage = firstEvo.getExtraInfo();
                String pkOldName = pk.getName();
                pk = firstEvo.getTo();
                System.out.println(pkOldName + " --> " + pk.getName()
                        + " (level up evo at level " + evoLevelFirstStage + ")");
            }

             //getEvoLevelsUsingMinMaxAvg(pk, evoLevelFirstStage, latestLevelUpEvo, levelUpBSTmax, levelUpBSTaverage);
            if (evoLevelFirstStage > 0) {
                getEvoLevelsUsingTriplet(pk, evoLevelFirstStage, latestLevelUpEvo, fromBST_toBST_evoLevel,
                        first2second, second2final, first2final, 1);
            } else {
                getEvoLevelsUsingTriplet(pk, evoLevelFirstStage, latestLevelUpEvo, fromBST_toBST_evoLevel,
                        first2second, second2final, first2final, 0);
            }

        }
    }

    private static void printRowList(List<int[]> rowList) {
        for (int k = 0; k < rowList.size(); k++) {
            int[] row = rowList.get(k);
            System.out.print(Arrays.toString(row) + " ");
            if ((k + 1) % 9 == 0) System.out.println();
        }
        System.out.println();
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
    private int getLevelUpTriplets(List<Species> levelUpEvos, List<int[]> fromBST_toBST_evoLevel,
                                   List<int[]> first2second, List<int[]> second2final, List<int[]> first2final,
                                   int depth) {
        int latestLevelUpEvo = 0; // TODO there is still a bug in here

        for (Species pk : levelUpEvos) {
            for (Evolution evo : pk.getEvolutionsFrom()) {
                Species evoOfPk = evo.getTo();
                int evoLevel = evo.getExtraInfo();
                int[] triplet = {getBST(pk), getBST(evoOfPk), evoLevel};
                fromBST_toBST_evoLevel.add(triplet);
                if (depth == 0 && evoOfPk.getEvolutionsFrom().isEmpty()) {
                    first2final.add(triplet);
                } else if (depth == 1 && evoOfPk.getEvolutionsFrom().isEmpty()) {
                    second2final.add(triplet);
                } else {
                    first2second.add(triplet);
                    latestLevelUpEvo = getLevelUpTriplets(Collections.singletonList(evoOfPk), fromBST_toBST_evoLevel,
                            first2second, second2final, first2final, 1);
                }
                if (evoLevel > latestLevelUpEvo) {
                    latestLevelUpEvo = evoLevel;
                }
            }
        }
        return latestLevelUpEvo;
    }

    private int getLevelUpInformation(List<Species> levelUpEvos, int[] levelUpBSTaverage, int[] levelUpBSTmin, int[] levelUpBSTmax) {
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
                        int evoLevel = pk.getEvolutionsFrom().get(0).getExtraInfo();
                        if (evoLevel > latestLevelUpEvo) {
                            latestLevelUpEvo = evoLevel;
                        }
                        int pkBST = getBST(pk);
                        Species evoOfPk = pk.getEvolutionsFrom().get(0).getTo();
                        int evoOfPkBST = getBST(evoOfPk);
                        levelUpEvos.set(k, evoOfPk);
                        evolutionHappened = true;
                        atLeastOneEvoHappend = true;
                    }
                }
            }
            if (atLeastOneEvoHappend) { // will enter this for j==0 always
                int sumBSTs = 0;
                for (int l = 0; l < levelUpEvos.size(); l++) {
                    Species pk = levelUpEvos.get(l);
                    int bst = getBST(pk);
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

    private int getBST(Species pk) {
        return pk.getHp() + pk.getAttack() + pk.getDefense() + pk.getSpatk() + pk.getSpdef() + pk.getSpeed() + pk.getSpecial();
    }

    private void getEvoLevelsUsingMinMaxAvg(Species pk, int evoLevelFirstStage, int latestLevelUpEvo,
                                            int[] levelUpBSTmax, int[] levelUpBSTaverage) {
        int bstPk = getBST(pk);

        for (Evolution evo : pk.getEvolutionsFrom()) {
            Species evoOfPk = evo.getTo();
            int bstEvoOfPk = getBST(evoOfPk);

            int chosenLevel = findEvolutionLevelUsingMinMaxAvg(bstPk, bstEvoOfPk, evoLevelFirstStage, latestLevelUpEvo,
                    levelUpBSTmax, levelUpBSTaverage);

            System.out.println(pk.getName() + " (BST: " + bstPk + ") --> "
                    + evoOfPk.getName() + " (BST: " + bstEvoOfPk + ") at level " + chosenLevel);

            // Handle possible second-stage evolution
            for (Evolution evoOfEvo : evoOfPk.getEvolutionsFrom()) {
                Species evoOfEvoOfPk = evoOfEvo.getTo();
                int bstEvoOfEvoOfPk = getBST(evoOfEvoOfPk);

                int chosenLevel2 = findEvolutionLevelUsingMinMaxAvg(bstEvoOfPk, bstEvoOfEvoOfPk, chosenLevel, latestLevelUpEvo,
                        levelUpBSTmax, levelUpBSTaverage);

                System.out.println(evoOfPk.getName() + " (BST: " + bstEvoOfPk + ") --> "
                        + evoOfEvoOfPk.getName() + " (BST: " + bstEvoOfEvoOfPk + ") at level " + chosenLevel2);
            }
        }
    }

    private int findEvolutionLevelUsingMinMaxAvg(int bstFrom, int bstTo,
                                   int minLevel, int latestLevelUpEvo,
                                   int[] levelUpBSTmax, int[] levelUpBSTaverage) {
        for (int lvl = minLevel; lvl < latestLevelUpEvo; lvl++) {
            if (bstTo <= levelUpBSTmax[lvl] && bstFrom <= levelUpBSTaverage[lvl]) {
                return lvl + 1; // levels are 1-based
            }
        }
        return latestLevelUpEvo; // Fallback to latest evo level
    }

    private void getEvoLevelsUsingTriplet(Species pk, int evoLevelFirstStage, int latestLevelUpEvo, List<int[]> fromBST_toBST_evoLevel,
                                          List<int[]> first2second, List<int[]> second2final, List<int[]> first2final, int depth) {
        int bstPk = getBST(pk);

        for (Evolution evo : pk.getEvolutionsFrom()) {
            Species evoOfPk = evo.getTo();
            int bstEvoOfPk = getBST(evoOfPk);
            int chosenLevel = 0;
            if (depth == 0 && evoOfPk.getEvolutionsFrom().isEmpty()) {
                chosenLevel = findEvolutionLevel(first2final, bstPk, bstEvoOfPk);
            } else if (depth == 1 && evoOfPk.getEvolutionsFrom().isEmpty()) {
                chosenLevel = findEvolutionLevel(second2final, bstPk, bstEvoOfPk);
            } else {
                chosenLevel = findEvolutionLevel(second2final, bstPk, bstEvoOfPk);
            }

            System.out.println(pk.getName() + " (BST: " + bstPk + ") --> "
                    + evoOfPk.getName() + " (BST: " + bstEvoOfPk + ") at level " + chosenLevel);

            // Handle possible second-stage evolution
            for (Evolution evoOfEvo : evoOfPk.getEvolutionsFrom()) {
                Species evoOfEvoOfPk = evoOfEvo.getTo();
                int bstEvoOfEvoOfPk = getBST(evoOfEvoOfPk);

                int chosenLevel2 = findEvolutionLevel(second2final, bstEvoOfPk, bstEvoOfEvoOfPk);

                System.out.println(evoOfPk.getName() + " (BST: " + bstEvoOfPk + ") --> "
                        + evoOfEvoOfPk.getName() + " (BST: " + bstEvoOfEvoOfPk + ") at level " + chosenLevel2);
            }
        }
    }


    public static int findEvolutionLevel(List<int[]> triplets, int targetPre, int targetPost) {
        double weightedSum = 0.0;
        double weightTotal = 0.0;

        // TODO pre evo weaker than comparison and post evo stronger than comparison should 'increase' the distance (decrease the weight)

        for (int[] row : triplets) {
            int preBST = row[0];
            int postBST = row[1];
            int level = row[2];

            // Euclidean distance
            double dx = preBST - targetPre;
            double dy = postBST - targetPost;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Avoid division by zero
            double weight = (distance == 0) ? 1.5 : 1.0 / distance;

            weightedSum += weight * level;
            weightTotal += weight;
        }
        return (int) Math.round(weightedSum / weightTotal);
    }


//    private int findEvolutionLevel(
//            int bstFrom,
//            int bstTo,
//            int minLevel,
//            int latestLevelUpEvo,
//            List<int[]> fromBST_toBST_evoLevel
//    ) {
//        int k = 10;
//        // PriorityQueue keeps k best matches (smallest diffs)
//        PriorityQueue<int[]> pq = new PriorityQueue<>(
//                (a, b) -> Integer.compare(
//                        Math.abs(b[0] - bstFrom),
//                        Math.abs(a[0] - bstFrom)
//                )
//        );
//
//        for (int[] row : fromBST_toBST_evoLevel) {
//            int diff = Math.abs(row[0] - bstFrom);
//            // Add row into PQ
//            pq.offer(row);
//            // Keep only k closest
//            if (pq.size() > k) {
//                pq.poll();
//            }
//        }
//
//        // Now pq contains the k closest rows
//        int sumLevels = 0;
//        int count = 0;
//        for (int[] row : pq) {
//            sumLevels += row[2]; // evo level
//            count++;
//        }
//
//        // Use average of k closest as sensible evo level
//        int estimatedLevel = sumLevels / count;
//
//        // Clamp to [minLevel, latestLevelUpEvo]
//        if (estimatedLevel < minLevel) {
//            estimatedLevel = minLevel;
//        }
//        if (estimatedLevel > latestLevelUpEvo) {
//            estimatedLevel = latestLevelUpEvo;
//        }
//
//        return estimatedLevel;
//    }


//    private int findEvolutionLevel(int bstFrom, int bstTo, int minLevel, int latestLevelUpEvo, List<int[]> fromBST_toBST_evoLevel) {
//        // Step 1: find the entry with closest bstFrom to the given bstFrom
//        int[] closest = null;
//        int closestDiff = Integer.MAX_VALUE;
//
//        for (int[] row : fromBST_toBST_evoLevel) {
//            int diff = Math.abs(row[0] - bstFrom);
//            if (diff < closestDiff) {
//                closestDiff = diff;
//                closest = row;
//
//                if (closestDiff == 0) {
//                    // Exact match, no need to continue
//                    break;
//                }
//            } else if (diff > closestDiff) {
//                // Since the list is sorted, diffs will only increase
//                break;
//            }
//        }
//
//        if (closest == null) {
//            // Fallback if the list is empty
//            return latestLevelUpEvo;
//        }
//
//        // Step 2: take evoLevel from closest as baseline
//        int candidateLevel = closest[2];
//
//        // Step 3: adjust slightly depending on bstTo compared to closest
//        if (bstTo > closest[1]) {
//            // If our target evo is stronger, push level up a bit
//            candidateLevel += 1;
//        } else if (bstTo < closest[1]) {
//            // If weaker, evolve slightly earlier
//            candidateLevel -= 1;
//        }
//
//        // Step 4: respect constraints
//        if (candidateLevel < minLevel) {
//            candidateLevel = minLevel;
//        }
//        if (candidateLevel > latestLevelUpEvo) {
//            candidateLevel = latestLevelUpEvo;
//        }
//
//        return candidateLevel;
//    }
}