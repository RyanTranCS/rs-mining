/**
 * This script allows characters to mine rocks at user-specified predetermined positions or within a user-specified
 * range from their character. The script also allows the user to choose whether the character should bank or drop
 * their mined ores when their inventory is full.
 *
 * @author Ryan Tran
 */

package scripts.mining;

import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.Objects;
import org.tribot.api2007.Player;
import org.tribot.api2007.types.RSArea;
import org.tribot.api2007.types.RSObject;
import org.tribot.api2007.types.RSTile;
import org.tribot.script.Script;
import scripts.CustomBanking;
import scripts.dax_api.api_lib.DaxWalker;
import scripts.dax_api.api_lib.models.DaxCredentials;
import scripts.dax_api.api_lib.models.DaxCredentialsProvider;

import java.util.function.BooleanSupplier;

public class MiningScriptOOP extends Script {

    // Variables to Change ---------------------------------------------------------------------------------------------
    // if true, character will bank; if set, character will drop
    private boolean banking = true;

    // desired rock type to mine
    private Rock rockType = Rock.TIN;
    // desired rock type to mine in string format
    private String rockTypeStr = "Tin ore";

    // FOR MINING NEAREST ROCKS
    private int miningRange = 5;

    // CHANGE ONLY 1 OF THESE (miningRange OR rockPositions)

    // FOR MINING SPECIFIC ROCKS
    private static RSTile[] rockPositions = null;
    // private static RSTile[] rockPositions = new RSTile[] {new RSTile(3223, 3146, 0), new RSTile(3222, 3147, 0)}

    // area to mine -> Lumbridge Swamp Mine
    private final RSArea MINE = new RSArea(new RSTile(3219, 3144, 0), new RSTile(3230, 3153, 0));
    private final RSArea MINE_ENTRANCE = new RSArea(new RSTile(3224, 3149, 0), new RSTile(3228, 3150, 0));
    // End of Variables to Change --------------------------------------------------------------------------------------

    private int rockPositionsLength = 0;
    private RSObject[] predeterminedRocks = null;
    private RSObject[] sortedPredeterminedRocks = null;

    @Override
    public void run() {
        DaxWalker.setCredentials(new DaxCredentialsProvider() {
            @Override
            public DaxCredentials getDaxCredentials() {
                return new DaxCredentials("sub_DPjXXzL5DeSiPf", "PUBLIC-KEY");
            }
        });

        General.println("Starting OO Script v1.0");
        /*start = System.currentTimeMillis();*/

        if (rockPositions != null) {
            rockPositionsLength = rockPositions.length;
            predeterminedRocks = new RSObject[rockPositionsLength];
        }

        while (true) {
            General.sleep(100);

            if (isInMine()) {
                General.println("In Mine");
                if (Inventory.isFull()) {
                    if (banking) {
                        General.println("Inventory Full: Banking");
                        DaxWalker.walkToBank();
                        CustomBanking.bank();
                        General.sleep(50, 100);
                    } else {
                        General.println("Inventory Full: Dropping");
                        Inventory.drop(new String[]{rockTypeStr});
                        General.sleep(50, 100);
                    }
                } else {
                    if (rockPositions == null) {
                        General.println("Mining Nearest Rock");
                        mineNearestRock();
                    }
                    else {
                        if (rockPositionsLength == 1) {
                            General.println("Mining Single Predetermined Rock");
                            mineSinglePredeterminedRock();
                        } else {
                            General.println("Mining Nearest Predetermined Rock");
                            mineNearestPredeterminedRock();
                        }
                    }
                }
            } else if (CustomBanking.isInBank()) {
                General.println("In Bank");
                if (Inventory.getAll().length > 0) {
                    General.println("Items in Inventory: Banking");
                    CustomBanking.bank();
                    General.sleep(50, 100);
                } else {
                    General.println("Walking to Mine");
                    DaxWalker.walkTo(MINE_ENTRANCE.getRandomTile());
                    walkToMine();
                    General.sleep(50, 100);
                }
            } else {
                General.println("Neither in Mine or in Bank");
                if (Inventory.isFull() && banking) {
                    General.println("Inventory Full: Banking");
                    DaxWalker.walkToBank();
                    CustomBanking.bank();
                    General.sleep(50, 100);
                } else {
                    General.println("Walking to Mine");
                    walkToMine();
                    General.sleep(50, 100);
                }
            }
        }
    }

    /**
     * Checks if the player is located within the mine.
     *
     * @return True if the player is in the mine, false otherwise.
     */
    public boolean isInMine() {
        if (!MINE.contains(Player.getPosition())) return false;
        return true;
    }

    /**
     * Generates a path to the mine and walks the player there until they reach it.
     *
     * @return True if the player is in the mine within 10-12 seconds of walking, false otherwise.
     */
    public boolean walkToMine() {
        //if (!WebWalking.walkTo(tilesSurroundingClayRock[0 + (int)(Math.random() * ((2 - 0) + 1))])) return false;
        if (!DaxWalker.walkTo(MINE_ENTRANCE.getRandomTile())) return false;

        BooleanSupplier atMine = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                General.sleep(200, 300);
                return isInMine();
            }
        };
        return Timing.waitCondition(atMine, General.random(10000, 12000));
    }

    /**
     * Mines the nearest rock.
     *
     * @return True if the rock is successfully mined; false otherwise.
     */
    public boolean mineNearestRock() {
        RSRock rock = new RSRock();
        if(!rock.setNearestRock(rockType, miningRange)) return false;

        if (!rock.mine()) return false;

        return true;
    }

    /**
     * Mines a single, predetermined rock.
     *
     * @return True if the rock is successfully mined; false otherwise.
     */
    public boolean mineSinglePredeterminedRock() {
        predeterminedRocks = createRocks(rockPositions);

        RSRock rock = new RSRock();
        if (!rock.setSinglePredeterminedRock(rockType, predeterminedRocks[0].getPosition())) return false;

        if (!rock.mine()) return false;

        return true;
    }

    /**
     * Mines the nearest rock out of a set of predetermined rocks.
     *
     * @return True if the rock is successfully mined; false otherwise.
     */
    public boolean mineNearestPredeterminedRock() {
        sortedPredeterminedRocks = Objects.sortByDistance(Player.getPosition(), createRocks(rockPositions));

        RSRock rock = new RSRock();
        if(!rock.setNearestPredeterminedRock(rockType, sortedPredeterminedRocks)) return false;

        if (!rock.mine()) return false;

        return true;
    }

    public static RSObject[] createRocks(RSTile[] rockPositions) {
        for (int i = 0; i < rockPositionsLength; i++) {
            predeterminedRocks[i] = Objects.getAt(rockPositions[i])[0];
        }
        return predeterminedRocks;
    }

    public static RSTile[] getRockPositions() {
        return rockPositions;
    }

    public static RSObject[] getPredeterminedRocks() {
        return predeterminedRocks;
    }

}
