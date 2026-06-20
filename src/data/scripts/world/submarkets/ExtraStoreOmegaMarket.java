package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import lunalib.lunaSettings.LunaSettings;
import org.magiclib.util.MagicSettings;

import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

public class ExtraStoreOmegaMarket extends BaseSubmarketPlugin {

    public static int PLAYER_LEVEL_UNLOCK = MagicSettings.getInteger(ESM_MOD_ID, "omegaMarketPlayerLevelUnlock");
    public static boolean MARKET_SELL_SHIPS = MagicSettings.getBoolean(ESM_MOD_ID, "omegaMarketSellShips");
    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "omegaMarketTariff");

    public ExtraStoreOmegaMarket() {
    }

    public static boolean playerLevelIsReached() {
        int level = PLAYER_LEVEL_UNLOCK;

        return Global.getSector().getPlayerStats().getLevel() >= level;
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        int max = Global.getSettings().getLevelupPlugin().getMaxLevel();
        if (!playerLevelIsReached()) {
            return "Unlocks after getting Level: " + max;
        }
        return super.getTooltipAppendix(ui);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return playerLevelIsReached();
    }

    @Override
    public boolean isHidden() {
        return !playerLevelIsReached();
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        //log.info("Days since update: " + sinceLastCargoUpdate);
        if (sinceLastCargoUpdate < 30) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();

        // Empty Store
        for (CargoStackAPI s : cargo.getStacksCopy()) {
            float qty = s.getSize();
            cargo.removeItems(s.getType(), s.getData(), qty);
        }

        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();

        FactionAPI omegaFaction = Global.getSector().getFaction("omega");

        if (MARKET_SELL_SHIPS)
            addShips(omegaFaction);

        addRandomWeapons(12, omegaFaction);

        cargo.sort();
    }

    public boolean isShipAllowed(FleetMemberAPI member, float requiredFP, FactionAPI faction) {
        if (member.getHullSpec().isDHull()) return false;
        if (member.getFleetPointCost() < requiredFP) return false;
        if (member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) return false;
        if (!faction.getKnownShips().contains(member.getHullSpec().getHullId())) return false;
        return member.getHullSpec().hasTag("omega");
    }

    public boolean isWeaponAllowed(WeaponSpecAPI spec, FactionAPI faction) {
        if (spec.hasTag(Tags.NO_DROP)) return false;
        if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) return false;
        if (!faction.getKnownWeapons().contains(spec.getWeaponId())) return false;
        if (!spec.getManufacturer().equals("Unknown"))
            return false; //Prune SuperWeapons which isn't Omega actually but Omega knows.

        return (faction.getId().equals("omega") && spec.hasTag("omega"));
    }

    protected void addRandomWeapons(int max, FactionAPI faction) {
        CargoAPI cargo = getCargo();
        java.util.List<String> weaponIds = Global.getSector().getAllWeaponIds();

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(itemGenRandom);

        for (String id : weaponIds) {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            if (isWeaponAllowed(spec, faction)) {
                float weight = 10 - spec.getTier();    // so 6-8

                weight *= spec.getRarity();

                // chance to be picked twice
                picker.add(spec.getWeaponId(), weight);
                picker.add(spec.getWeaponId(), weight);
            }
        }
        for (int i = 0; i < max; i++) {
            if (picker.isEmpty()) break;
            String weaponId = picker.pickAndRemove();
            int quantity = 2;
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(weaponId);
            if (spec.getSize() == WeaponAPI.WeaponSize.SMALL)
                quantity = 8;
            else if (spec.getSize() == WeaponAPI.WeaponSize.MEDIUM)
                quantity = 4;
            cargo.addWeapons(weaponId, quantity);
        }
    }

    protected void addWeapons(FactionAPI faction) {
        addRandomWeapons(10, faction);
    }

    protected void addShips(FactionAPI faction) {

        CargoAPI cargo = getCargo();
        FleetDataAPI data = cargo.getMothballedShips();

        WeightedRandomPicker<String> rolePicker = new WeightedRandomPicker<>(itemGenRandom);
        rolePicker.add(ShipRoles.COMBAT_SMALL, 50f);
        rolePicker.add(ShipRoles.COMBAT_MEDIUM, 40f);
        rolePicker.add(ShipRoles.COMBAT_LARGE, 20f);
        rolePicker.add(ShipRoles.COMBAT_CAPITAL, 20f);

        //renew the stock
        float variation = (float) itemGenRandom.nextFloat() * 0.5f + 0.75f;
        int tries = 0;
        int target = Math.round(20 * variation);
        for (int i = 0; i < target; i = cargo.getMothballedShips().getNumMembers()) {
            //pick the role and faction
            java.util.List<ShipRolePick> picks = null;
            do {
                String role = rolePicker.pick();
                //pick the random ship
                try {
                    picks = faction.pickShip(role, FactionAPI.ShipPickParams.priority());
                } catch (NullPointerException npex) {
                    // likely picker picked a role when faction has no ships for that role; do nothing
                }
                //log.info("Add ship role try " + tries2 + ": " + (picks == null));
            } while (picks == null);

            for (ShipRolePick pick : picks) {
                FleetMemberType type = FleetMemberType.SHIP;
                String variantId = pick.variantId;
                //set the ID
                FleetMemberAPI member = Global.getFactory().createFleetMember(type, variantId);
                variantId = member.getHullId() + "_Hull";
                member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);

                // Fleet point cost threshold
                int FP;
                if (member.isCapital()) {
                    FP = 20;
                } else if (member.isCruiser()) {
                    FP = 14;
                } else if (member.isDestroyer()) {
                    FP = 10;
                } else if (member.isFrigate()) {
                    FP = 5;
                } else {
                    FP = 6;
                }
                FP = 0;

                //if the variant is not degraded and high end, add it. Else start over
                if (isShipAllowed(member, FP, faction)) {
                    member.getRepairTracker().setMothballed(true);
                    member.getRepairTracker().setCR(0.5f);

                    //REMOVE UNRECOVERABLE THING
                    if (member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE)) {
                        member.getVariant().addTag(Tags.AUTOMATED_RECOVERABLE);
                    }

                    getCargo().getMothballedShips().addFleetMember(member);
                } else {
                    i -= 1;
                }
            }
            tries++;
            //log.info("Add ship try " + tries);
            if (tries > 40 + target) break;
        }
    }

    //==========================================================================
    //==========================================================================

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return true;
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return true;
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return true;
        return false;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "No sales/returns";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "No sales/returns";
    }

    @Override
    public float getTariff() {
        return MARKET_TARIFF;
    }

    @Override
    public boolean isBlackMarket() {
        return false;
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }
}