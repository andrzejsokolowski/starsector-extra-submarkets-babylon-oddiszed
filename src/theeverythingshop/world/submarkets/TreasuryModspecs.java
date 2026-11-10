package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import theeverythingshop.items.SpecialItemRules;

/**
 * Treasury - Modspecs: a modspec for every hull mod the player can learn and
 * doesn't know yet. Restocks on every visit, so a learned hull mod leaves the shelf at once.
 */
public class TreasuryModspecs extends TreasuryShopBase {

    @Override
    protected void addStock(CargoAPI cargo) {
        FactionAPI player = Global.getSector().getPlayerFaction();
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            if (!SpecialItemRules.isLearnableHullMod(spec) || player.knowsHullMod(spec.getId())) {
                continue;
            }
            cargo.addSpecial(new SpecialItemData(SpecialItemRules.MODSPEC_ID, spec.getId()), STOCK);
        }
    }

    @Override
    protected boolean restockEveryVisit() {
        return true;
    }

    @Override
    protected String getDisplayName() {
        return "Treasury - Modspecs";
    }
}
