package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import theeverythingshop.items.DataItemStock;
import theeverythingshop.items.SpecialItemRules;

/**
 * Specials: every special item the player can use, from every installed mod, 999 of each.
 * Blueprints, hullmod specs and Exotica items have their own shops.
 */
public class TreasurySpecials extends TreasuryShopBase {

    @Override
    protected void addStock(CargoAPI cargo) {
        for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs()) {
            if (!SpecialItemRules.isPlainShopItem(spec)) {
                continue;
            }
            try {
                cargo.addSpecial(new SpecialItemData(spec.getId(), null), STOCK);
            } catch (Throwable t) {
                log.warn("Skipping special item " + spec.getId(), t);
                cargo.removeEmptyStacks();
            }
        }
        for (String[] entry : DataItemStock.specials(STOCK)) {
            addSpecial(cargo, entry[0], entry[1], Integer.parseInt(entry[2]));
        }
    }

    @Override
    protected String getDisplayName() {
        return "Specials";
    }
}
