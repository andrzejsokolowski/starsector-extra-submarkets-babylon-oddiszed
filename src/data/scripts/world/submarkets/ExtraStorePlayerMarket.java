package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicSettings;

import java.util.Set;

import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

public class ExtraStorePlayerMarket extends BaseSubmarketPlugin {

    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "playerMarketTariff");

    public ExtraStorePlayerMarket() {
    }

    private float getWeight(ShipHullSpecAPI spec) {
        float fp = spec.getFleetPoints();
        if (fp <= 0) fp = 1f;
        return 100f / (float) Math.sqrt(fp);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        if (sinceLastCargoUpdate < 30) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();
        cargo.clear();
        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();

        FactionAPI playerFaction = Global.getSector().getPlayerFaction();

        int shipCount = MathUtils.getRandomNumberInRange(18, 24);
        int wingCount = MathUtils.getRandomNumberInRange(10, 15);
        int weaponCount = MathUtils.getRandomNumberInRange(20, 40);

        if (playerFaction != null) {
            addShips(playerFaction, shipCount);
            addRandomWings(playerFaction, wingCount);
            addRandomWeapons(playerFaction, weaponCount);
        }

        cargo.sort();
    }

    private boolean isShipAllowed(ShipHullSpecAPI spec) {
        //if (spec.hasTag(Tags.RESTRICTED)) return false;
        if (spec.hasTag(Tags.UNRECOVERABLE)) return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE) && !spec.hasTag(Tags.AUTOMATED_RECOVERABLE))
            return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) return false;

        return true;
    }

    private void addShips(FactionAPI faction, int shipCount) {
        WeightedRandomPicker<String> selectedShips = new WeightedRandomPicker<>(itemGenRandom);

        Set<String> knownShips = faction.getKnownShips();
        Set<String> priorityShips = faction.getPriorityShips();

        if (!knownShips.isEmpty()) {
            for (String shipId : knownShips) {
                ShipHullSpecAPI spec = Global.getSettings().getHullSpec(shipId);
                if (spec == null || !isShipAllowed(spec)) continue;

                float weight = getWeight(spec);

                //Prioritize Priorities
                if (priorityShips.contains(shipId)) {
                    weight = weight * 10;
                } else {
                    weight = weight * 0.33f;
                }

                selectedShips.add(shipId, weight);
            }

            //log.info("faction picked: " + faction);
            if (!selectedShips.isEmpty()) {
                for (int i = 0; i < shipCount; i++) {
                    FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, selectedShips.pick(itemGenRandom) + "_Hull");
                    member.getRepairTracker().setMothballed(true);
                    member.getRepairTracker().setCR(0.5f);

                    cargo.getMothballedShips().addFleetMember(member);
                }
            }
        }
    }

    protected void addRandomWings(FactionAPI faction, int wingCount) {
        CargoAPI cargo = getCargo();
        Set<String> knownFighters = faction.getKnownFighters();
        Set<String> priorityFighters = faction.getKnownFighters();
        WeightedRandomPicker<String> pickedWings = new WeightedRandomPicker<>(itemGenRandom);

        if (!knownFighters.isEmpty()) {
            for (String wingId : knownFighters) {
                FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wingId);
                if (spec.hasTag(Tags.NO_DROP)) continue;

                float weight = 10 - spec.getTier();

                //Prioritize Priorities
                if (priorityFighters.contains(wingId)) {
                    weight = weight * 10;
                } else {
                    weight = weight * 0.33f;
                }

                pickedWings.add(wingId, weight);
            }

            for (int i = 0; i < wingCount; i++) {
                if (pickedWings.isEmpty()) break;
                cargo.addItems(CargoAPI.CargoItemType.FIGHTER_CHIP, pickedWings.pickAndRemove(), 2);
            }
        }
    }

    protected void addRandomWeapons(FactionAPI faction, int weaponCount) {
        CargoAPI cargo = getCargo();
        WeightedRandomPicker<String> pickedWeapons = new WeightedRandomPicker<>(itemGenRandom);
        Set<String> priorityWeapons = faction.getPriorityWeapons();

        for (String id : faction.getKnownWeapons()) {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            if (isWeaponAllowed(spec, faction)) {
                float weight = 10 - spec.getTier();

                //Prioritize Priorities
                if (priorityWeapons.contains(id)) {
                    weight = weight * 10;
                } else {
                    weight = weight * 0.33f;
                }

                pickedWeapons.add(id, weight);
            }
        }

        for (int i = 0; i < weaponCount; i++) {
            if (pickedWeapons.isEmpty()) break;
            String weaponId = pickedWeapons.pickAndRemove();
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(weaponId);
            int quantity = 2;
            if (spec.getSize() == WeaponAPI.WeaponSize.SMALL) quantity = 8;
            else if (spec.getSize() == WeaponAPI.WeaponSize.MEDIUM) quantity = 4;
            cargo.addWeapons(weaponId, quantity);
        }
    }

    public boolean isWeaponAllowed(WeaponSpecAPI spec, FactionAPI faction) {
        if (spec.hasTag(Tags.NO_DROP)) return false;
        //if (spec.hasTag(Tags.RESTRICTED)) return false;
        if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) return false;
        if (!faction.getKnownWeapons().contains(spec.getWeaponId())) return false;
        return true;
    }

    //==========================================================================
    //==========================================================================

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        if (action == SubmarketPlugin.TransferAction.PLAYER_SELL) return true;
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, SubmarketPlugin.TransferAction action) {
        if (action == SubmarketPlugin.TransferAction.PLAYER_SELL) return true;
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
        if (action == SubmarketPlugin.TransferAction.PLAYER_SELL) return true;
        return false;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
        return "No refunds!";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        return "No refunds!";
    }

    @Override
    public float getTariff() {
        return MARKET_TARIFF;
    }

    @Override
    public String getName() {
        return "We'll Build" + "\n" + "Your Shit";
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