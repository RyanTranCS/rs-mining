/**
 * This class contains methods that help with determining what rocks are suitable for mining, with determining if
 * a rock has been mined, with determining if the character is mining, and with implementing a rock hovering feature.
 *
 * @author Ryan Tran
 * */

package scripts.mining;

import org.tribot.api.Clicking;
import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api2007.Objects;
import org.tribot.api2007.Player;
import org.tribot.api2007.types.RSObject;
import org.tribot.api2007.types.RSObjectDefinition;
import org.tribot.api2007.types.RSTile;

import java.util.function.BooleanSupplier;

public class MiningHelper {

    private static boolean isRockMineableWhileWalkingToIt = true;
    private static RSTile rockToHoverTile = new RSTile(0, 0, 0);

    /**
     * Checks an array of rocks for one that matches the desired rock type to be mined.
     *
     * @return If one is found, return that rock object. Otherwise, return null.
     */
    public static RSObject getSuitableMineableRock(RSObject[] rocks, Rock type) {
        RSObject mineableRock = null;
        for (int i = 0; i < rocks.length; i++) {
            if (isRockCorrectColor(rocks[i], type)) {
                mineableRock = rocks[i];
                break;
            }
        }
        return mineableRock;
    }

    /**
     * Compares a rock's color to a set of pre-defined desired colors (a rock can have a different color as another of
     * the same type).
     *
     * @return True if the rock's color matches; false otherwise.
     */
    public static boolean isRockCorrectColor(RSObject rock, Rock rockEnum) {
        RSObjectDefinition rockDef = rock.getDefinition();
        if (rockDef == null) return false;

        short[] validColors = rockEnum.getModifiedColours();
        short[] rockColors = rockDef.getModifiedColors();

        if (rockColors == null || rockColors.length == 0) return false;

        for (short rockColor : rockColors) {
            if (doesConstantColorsContain(validColors, rockColor)) return true;
        }

        return false;
    }

    /**
     * Helper method to compare a rock's constant colors to the color(s) contained within a rock's modified colors.
     *
     * @return True if it matches; false otherwise.
     */
    public static boolean doesConstantColorsContain(short[] constantColors, short rockColor) {
        for (short constantColor : constantColors) {
            if (constantColor == rockColor) return true;
        }

        return false;
    }

    /**
     * Checks if a rock is on-screen and if it is still mineable while walking to it.
     *
     * @return True if the rock is present; false otherwise.
     */
    public static boolean isRockOnScreenAndMineable(RSObject rock, Rock type) {
        BooleanSupplier rockOnScreenOrLostItsColor = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                General.sleep(200, 250);

                if (hasRockLostItsColor(rock, type)) {
                    isRockMineableWhileWalkingToIt = false;
                    return true;
                }
                return rock.isOnScreen();
            }
        };
        if (Timing.waitCondition(rockOnScreenOrLostItsColor, General.random(11000, 12000))) {
            if (isRockMineableWhileWalkingToIt) return true;
        }
        isRockMineableWhileWalkingToIt = true;
        return false;
    }

    /**
     * Updates rock and checks if it has lost its color/ore.
     *
     * @return True if it has; false otherwise.
     */
    public static boolean hasRockLostItsColor(RSObject rock, Rock type) {
        // refreshes rock object
        RSObject[] rocks = Objects.getAt(rock.getPosition());

        if (rocks.length == 0) return false;

        // checks its color
        return !isRockCorrectColor(rocks[0], type);
    }

    /**
     * Checks if the player is mining a rock.
     *
     * @return True if the player is mining a rock, false otherwise.
     */
    public static boolean isMining() {
        General.sleep(250);

        if (Player.getAnimation() > 0) return true;

        return false;
    }

    /**
     *  Checks if the character is mining through an animation check and waits until they are.
     *
     * @return True if mining, false otherwise.
     */
    public static boolean isCharacterMining() {
        BooleanSupplier isMining = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                General.sleep(250);
                return isMining();
            }
        };
        return Timing.waitCondition(isMining, General.random(5000, 6000));
    }


    /**
     * Checks and waits for a rock's ore to be extracted after clicking its "mine" option.
     *
     * @return True if the rock has lost its ore/color, false otherwise.
     */
    public static boolean isRockDoneBeingMined(RSObject rockBeingMined, Rock typeToMine, int distance) {
        BooleanSupplier isRockMined = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                General.sleep(600);

                if(distance > 0) {
                    hoverNextNearestRock(rockBeingMined, typeToMine, distance);
                } else if (MiningScriptOOP.getPredeterminedRocks().length == 1) {
                    // do nothing == don't hover
                } else {
                    hoverNextPredeterminedRock(rockBeingMined, typeToMine);
                }

                return hasRockLostItsColor(rockBeingMined, typeToMine);
            }
        };
        return Timing.waitCondition(isRockMined, General.random(35000, 40000));
    }

    /**
     * Hovers the cursor over the next nearest rock.
     *
     * @return True if a rock is successfully being hovered over, false otherwise.
     * */
    public static boolean hoverNextNearestRock(RSObject rockBeingMined, Rock type, int distance) {
        // refreshes nearest RSObject rocks for further processing
        RSObject[] nearestRocks = Objects.findNearest(distance, "Rocks");
        return hover(generateHoverableRocks(rockBeingMined, nearestRocks), type);
    }

    /**
     * Hovers cursor over the next nearest rock from a predetermined set of rocks.
     *
     * @return True if a rock is successfully being hovered over, false otherwise.
     * */
    public static boolean hoverNextPredeterminedRock(RSObject rockBeingMined, Rock type) {
        // refreshes predetermined RSObject rocks and sorts them for further processing
        RSObject[] sortedPredeterminedRocks = Objects.sortByDistance(Player.getPosition(),
                MiningScriptOOP.createRocks(MiningScriptOOP.getRockPositions()));

        return hover(generateHoverableRocks(rockBeingMined, sortedPredeterminedRocks), type);
    }

    /**
     * Determines a rock suitable for mining from a list of hoverable rocks and hovers the cursor over it.
     *
     * @return True if a rock is successfully being hovered over, false otherwise.
     * */
    public static boolean hover(RSObject[] rocksToHover, Rock type) {
        RSObject rockToHover = getSuitableMineableRock(rocksToHover, type);
        if (rockToHover == null) return false;

        General.sleep(500);

        if (rockToHoverTile.equals(rockToHover.getPosition())) {
            General.println("Same Rock: Not Moving Cursor");
            return false;
        }

        rockToHoverTile = rockToHover.getPosition();
        if (!Clicking.hover(rockToHover)) return false;
        General.println("Hovering");

        return true;
    }

    /**
     * Generates an array of hoverable rocks (one that excludes the rock currently being mined).
     *
     * @param rockBeingMined: the rock currently being mined
     * @param unfilteredRocks: an array of rocks that can be mined, including the rock currently being mined
     *
     * @return Array of hoverable rocks.
     * */
    public static RSObject[] generateHoverableRocks(RSObject rockBeingMined, RSObject[] unfilteredRocks) {
        RSTile rockBeingMinedPosition = rockBeingMined.getPosition();
        int rockBeingMinedIndex = 0;

        for (int i = 0; i < unfilteredRocks.length; i++) {
            if (unfilteredRocks[i].getPosition().equals(rockBeingMinedPosition)) {
                rockBeingMinedIndex = i;
                break;
            }
        }

        RSObject[] rocksForHovering = new RSObject[unfilteredRocks.length - 1];
        boolean isRockRemoved = false;

        for (int i = 0; i < unfilteredRocks.length; i++) {
            if (rockBeingMinedIndex == i) {
                // rock being mined encountered -> skip copying over to new array
                isRockRemoved = true;
            } else {
                if (isRockRemoved) {
                    // rock has been skipped being copied -> index now 1 behind for the new array
                    rocksForHovering[i - 1] = unfilteredRocks[i];
                } else {
                    rocksForHovering[i] = unfilteredRocks[i];
                }
            }
        }

        return rocksForHovering;
    }

}
