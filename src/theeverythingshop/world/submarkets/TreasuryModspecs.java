package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import theeverythingshop.items.SpecialItemRules;

/**
 * Hullmod Specs: one spec for every hull mod the player can learn and doesn't know yet.
 * Rebuilt on every visit, so a learned hull mod leaves the shelf at once, and a spec bought
 * here never comes back.
 */
public class TreasuryModspecs extends TreasuryShopBase {

    @Override
    protected void addStock(CargoAPI cargo) {
        FactionAPI player = Global.getSector().getPlayerFaction();
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            if (!SpecialItemRules.isLearnableHullMod(spec) || player.knowsHullMod(spec.getId())) {
                continue;
            }
            addOne(cargo, SpecialItemRules.MODSPEC_ID, spec.getId());
        }
    }

    @Override
    protected boolean oneCopyEach() {
        return true;
    }

    @Override
    protected boolean restockEveryVisit() {
        return true;
    }

    @Override
    protected String getDisplayName() {
        return "Hullmod Specs";
    }
}
