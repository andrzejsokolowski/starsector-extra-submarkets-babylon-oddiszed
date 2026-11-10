package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import data.scripts.chicken.SpecialItemRules;

/**
 * Chicken's Tech - Blueprints: every blueprint package, and every rare ship, weapon
 * and fighter blueprint (the ones tagged rare_bp that can drop).
 */
public class ExtraStoreChickenBlueprints extends ExtraStoreChickenBase {

    @Override
    protected void addStock(CargoAPI cargo) {
        for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs()) {
            if (SpecialItemRules.isBlueprintPackage(spec) && !SpecialItemRules.isExcluded(spec.getId())) {
                cargo.addSpecial(new SpecialItemData(spec.getId(), null), STOCK);
            }
        }
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (spec.hasTag("rare_bp") && !spec.hasTag(Tags.NO_DROP) && !spec.hasTag(Tags.NO_BP_DROP)) {
                cargo.addSpecial(new SpecialItemData(Items.SHIP_BP, spec.getHullId()), STOCK);
            }
        }
        for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
            if (spec.hasTag("rare_bp") && !spec.hasTag(Tags.NO_DROP) && !spec.hasTag(Tags.NO_BP_DROP)) {
                cargo.addSpecial(new SpecialItemData(Items.WEAPON_BP, spec.getWeaponId()), STOCK);
            }
        }
        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
            if (spec.hasTag("rare_bp") && !spec.hasTag(Tags.NO_DROP) && !spec.hasTag(Tags.NO_BP_DROP)) {
                cargo.addSpecial(new SpecialItemData(Items.FIGHTER_BP, spec.getId()), STOCK);
            }
        }
    }

    @Override
    protected String getDisplayName() {
        return "Chicken's Tech - Blueprints";
    }
}
