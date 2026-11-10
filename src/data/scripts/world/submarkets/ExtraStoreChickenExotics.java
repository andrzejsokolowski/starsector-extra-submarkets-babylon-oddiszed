package data.scripts.world.submarkets;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import data.scripts.compat.ExoticaCompat;

/**
 * Chicken's Tech - Exotics (Exotica Technologies only): every exotic in every form
 * (normal, corrupted, pure and the fork's guerilla), and the items that install an exotic.
 */
public class ExtraStoreChickenExotics extends ExtraStoreChickenBase {

    @Override
    protected void addStock(CargoAPI cargo) {
        for (String[] entry : ExoticaCompat.listExotics()) {
            addSafely(cargo, entry[0], entry[1]);
        }
        for (String id : ExoticaCompat.listInstallItems()) {
            addSafely(cargo, id, null);
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
        return "Chicken's Tech - Exotics";
    }
}
