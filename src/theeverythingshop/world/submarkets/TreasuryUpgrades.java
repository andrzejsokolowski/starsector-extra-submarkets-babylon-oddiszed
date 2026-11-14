package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.campaign.CargoAPI;
import theeverythingshop.compat.ExoticaCompat;

/**
 * Upgrades (Exotica Technologies only): every upgrade at every level it has.
 */
public class TreasuryUpgrades extends TreasuryExotics {

    @Override
    protected void addStock(CargoAPI cargo) {
        for (String[] entry : ExoticaCompat.listUpgrades()) {
            addSafely(cargo, entry[0], entry[1]);
        }
    }

    @Override
    protected String getDisplayName() {
        return "Upgrades";
    }
}
