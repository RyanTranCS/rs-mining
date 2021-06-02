/**
 * The RSRock class represents a RuneScape Rock entity.
 *
 * @author Ryan Tran
 * */

package scripts.mining;

import org.tribot.api.DynamicClicking;
import org.tribot.api.General;
import org.tribot.api2007.Objects;
import org.tribot.api2007.Walking;
import org.tribot.api2007.types.RSObject;
import org.tribot.api2007.types.RSTile;

public class RSRock {

    private Rock rockType;  // the type of the rock to mine
    private int distance;   // the distance the rock must be in for it to be mined
    private RSObject rock;  // the RSObject representation of the rock

    /**
     * The following three methods act as the constructor for an RSRock object. Due to the dynamic environment,
     * a boolean value must be returned during an object's construction to ensure proper functionality and recovery.
     *
     * @return: True if the RSRock was successfully constructed; false otherwise.
     * */
    public boolean setNearestRock(Rock type, int distance) {
        rockType = type;

        RSObject[] rocks = Objects.findNearest(distance, "Rocks");
        if (rocks.length == 0) return false;

        rock = MiningHelper.getSuitableMineableRock(rocks, type);
        if (rock == null) return false;

        this.distance = distance;

        return true;
    }

    public boolean setSinglePredeterminedRock(Rock type, RSTile position) {
        if(!Objects.isAt(position, "Rocks")) return false;

        rockType = type;

        RSObject[] rocks = Objects.getAt(position);
        rock = rocks[0];

        return true;
    }

    public boolean setNearestPredeterminedRock(Rock type, RSObject[] sortedNearestPredeterminedRocks) {
       rockType = type;

       rock = MiningHelper.getSuitableMineableRock(sortedNearestPredeterminedRocks, type);
       if (rock == null) return false;

        return true;
    }

    /**
     * This method walks the character to the rock if it is not on the screen. It then ensures the rock is on the screen
     * and minable before clicking it. Once clicked, it waits for the ore to be extracted from the rock.
     *
     * @return: True if the rock's ore is extracted; false otherwise.
     * */
    public boolean mine() {
        if (!rock.isOnScreen()) {
            if (!Walking.walkPath(Walking.generateStraightPath(rock))) return false;
        }
        General.println("Walking to Rock");

        if (!MiningHelper.isRockOnScreenAndMineable(rock, rockType)) {
            General.println("Rock Not on Screen / Mineable");
            return false;
        }

        if (!DynamicClicking.clickRSObject(rock, "Mine")) {
            General.println("Unable to Click Mine on Rock");
            return false;
        }
        General.println("Clicked Rock");

        if (!MiningHelper.isCharacterMining()) {
            General.println("Character is Not Mining");
            return false;
        }
        General.println("Mining");

        if (!MiningHelper.isRockDoneBeingMined(rock, rockType, distance)) return false;

        return true;
    }

}