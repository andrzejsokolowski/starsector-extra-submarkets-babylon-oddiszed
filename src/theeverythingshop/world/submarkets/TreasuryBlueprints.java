package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.impl.items.BlueprintProviderItem;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import theeverythingshop.compat.StationAugmentsCompat;
import theeverythingshop.items.DataItemStock;
import theeverythingshop.items.SpecialItemRules;

import java.util.List;
import java.util.Set;

/**
 * Blueprints: one of each blueprint the player doesn't know yet (rare ship, weapon and
 * fighter blueprints, blueprint packages that still teach something, Station Augment
 * blueprints and AoTD Citadel Schematics). What the player knows or bought here never comes
 * back. Blueprint items that are used up (RAT industry blueprints, Exiled Sector
 * schematics) are sold 999 at a time.
 */
public class TreasuryBlueprints extends TreasuryShopBase {

    @Override
    protected void addStock(CargoAPI cargo) {
        FactionAPI player = Global.getSector().getPlayerFaction();

        for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs()) {
            if (SpecialItemRules.isBlueprintPackage(spec) && !SpecialItemRules.isExcluded(spec.getId())
                    && teachesSomething(spec.getId(), player)) {
                addOne(cargo, spec.getId(), null);
            }
        }
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (isRareBlueprint(spec.hasTag("rare_bp"), spec.hasTag(Tags.NO_DROP), spec.hasTag(Tags.NO_BP_DROP))
                    && !player.knowsShip(spec.getHullId())) {
                addOne(cargo, Items.SHIP_BP, spec.getHullId());
            }
        }
        for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
            if (isRareBlueprint(spec.hasTag("rare_bp"), spec.hasTag(Tags.NO_DROP), spec.hasTag(Tags.NO_BP_DROP))
                    && !player.knowsWeapon(spec.getWeaponId())) {
                addOne(cargo, Items.WEAPON_BP, spec.getWeaponId());
            }
        }
        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
            if (isRareBlueprint(spec.hasTag("rare_bp"), spec.hasTag(Tags.NO_DROP), spec.hasTag(Tags.NO_BP_DROP))
                    && !player.knowsFighter(spec.getId())) {
                addOne(cargo, Items.FIGHTER_BP, spec.getId());
            }
        }

        Set<String> knownAugments = StationAugmentsCompat.playerKnownAugments();
        for (String[] entry : DataItemStock.learnableBlueprints()) {
            if (!knownAugments.contains(entry[1])) {
                addOne(cargo, entry[0], entry[1]);
            }
        }
        for (String id : SpecialItemRules.CITADEL_SCHEMATICS) {
            if (hasItem(id) && !SpecialItemRules.isExcluded(id)) {
                addOne(cargo, id, null);
            }
        }

        for (String[] entry : DataItemStock.consumableBlueprints(STOCK)) {
            addSpecial(cargo, entry[0], entry[1], Integer.parseInt(entry[2]));
        }
    }

    private static boolean isRareBlueprint(boolean rare, boolean noDrop, boolean noBlueprintDrop) {
        return rare && !noDrop && !noBlueprintDrop;
    }

    // A package stays on sale while at least one ship, weapon, wing or industry in it is unknown
    private static boolean teachesSomething(String id, FactionAPI player) {
        try {
            CargoStackAPI stack = Global.getFactory().createCargoStack(CargoItemType.SPECIAL,
                    new SpecialItemData(id, null), null);
            SpecialItemPlugin plugin = stack.getPlugin();
            if (!(plugin instanceof BlueprintProviderItem)) {
                return true;
            }
            BlueprintProviderItem provider = (BlueprintProviderItem) plugin;
            if (anyUnknown(provider.getProvidedShips(), player, 0)
                    || anyUnknown(provider.getProvidedWeapons(), player, 1)
                    || anyUnknown(provider.getProvidedFighters(), player, 2)
                    || anyUnknown(provider.getProvidedIndustries(), player, 3)) {
                return true;
            }
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

    private static boolean anyUnknown(List<String> ids, FactionAPI player, int kind) {
        if (ids == null) {
            return false;
        }
        for (String id : ids) {
            boolean known;
            switch (kind) {
                case 0:
                    known = player.knowsShip(id);
                    break;
                case 1:
                    known = player.knowsWeapon(id);
                    break;
                case 2:
                    known = player.knowsFighter(id);
                    break;
                default:
                    known = player.knowsIndustry(id);
                    break;
            }
            if (!known) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasItem(String id) {
        try {
            return Global.getSettings().getSpecialItemSpec(id) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    protected boolean oneCopyEach() {
        return true;
    }

    // Rebuilt on every visit, so a blueprint leaves the shelf as soon as it is known
    @Override
    protected boolean restockEveryVisit() {
        return true;
    }

    @Override
    protected String getDisplayName() {
        return "Blueprints";
    }
}
