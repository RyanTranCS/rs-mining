/**
 * This class contains methods useful for banking not contained in the TRiBot Banking class and methods suitable for
 * replacing methods with the same name in the TRiBot Banking class.
 *
 * @author Ryan Tran
 * */

import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api2007.Banking;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.NPCs;
import org.tribot.api2007.Objects;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSObject;

import java.util.function.BooleanSupplier;

public class CustomBanking {

    /**
     * Checks if the player is located within a bank.
     *
     * @return True if the player is in a bank, false otherwise.
     */
    public static boolean isInBank() {
        RSObject[] booths = Objects.findNearest(20, "Bank booth");
        if (booths.length != 0) {
            if (booths[0].isOnScreen()) return true;
        }

        final RSNPC[] bankers = NPCs.findNearest("Banker");
        if (bankers.length == 0) return false;

        return bankers[0].isOnScreen();
    }

    /**
     * Banks all inventory items.
     *
     * @return True if inventory is successfully banked, false otherwise.
     */
    public static boolean bank() {
        if (!Banking.isBankScreenOpen()) {
            if (!Banking.openBank()) return false;
        }

        // inventory is full at this point
        if (Banking.depositAll() == 0) return false;

        BooleanSupplier isInventoryDeposited = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return !Inventory.isFull();
            }
        };
        if (Timing.waitCondition(isInventoryDeposited, General.random(2000, 3000))) {
            Banking.close();
        } else {
            return false;
        }

        return true;
    }

}
