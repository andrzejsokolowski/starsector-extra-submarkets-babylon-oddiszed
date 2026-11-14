package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import theeverythingshop.compat.ExoticaCompat;

/**
 * Exotics (Exotica Technologies only): every exotic chip in every form (normal, corrupted,
 * pure and the fork's guerilla). Exotics that install from an AI core get no chip.
 */
public class TreasuryExotics extends TreasuryShopBase {

    @Override
    protected void addStock(CargoAPI cargo) {
        for (String[] entry : ExoticaCompat.listExotics()) {
            addSafely(cargo, entry[0], entry[1]);
        }
    }

    protected void addSafely(CargoAPI cargo, String id, String data) {
        try {
            cargo.addSpecial(new SpecialItemData(id, data), STOCK);
        } catch (Throwable t) {
            log.warn("Skipping Exotica item " + id + " " + data, t);
            cargo.removeEmptyStacks();
        }
    }

    @Override
    protected String getDisplayName() {
        return "Exotics";
    }
}
