package data.scripts.world.submarkets;

import com.fs.starfarer.api.campaign.CargoAPI;
import data.scripts.compat.ExoticaCompat;

/**
 * Chicken's Tech - Upgrades (Exotica Technologies only): every upgrade at every level it has.
 */
public class ExtraStoreChickenUpgrades extends ExtraStoreChickenExotics {

    @Override
    protected void addStock(CargoAPI cargo) {
        for (String[] entry : ExoticaCompat.listUpgrades()) {
            addSafely(cargo, entry[0], entry[1]);
        }
    }

    @Override
    protected String getDisplayName() {
        return "Chicken's Tech - Upgrades";
    }
}
