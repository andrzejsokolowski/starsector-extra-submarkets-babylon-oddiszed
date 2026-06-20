package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicSettings;

import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.impl.campaign.ids.Items.*;
import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

public class ExtraStoreXIV extends BaseSubmarketPlugin {

    private final RepLevel MIN_STANDING = RepLevel.COOPERATIVE;

    public static boolean MARKET_SELL_BLUEPRINTS =
            MagicSettings.getBoolean(ESM_MOD_ID, "eliteShipyardSellBlueprints");
    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "eliteShipyardTariff");

    public ExtraStoreXIV() {
    }

    private FactionAPI getHegemony() {
        return Global.getSector().getFaction(Factions.HEGEMONY);
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

        int shipCount = MathUtils.getRandomNumberInRange(20, 25);
        int shipBpCount = MathUtils.getRandomNumberInRange(2, 3);
        int wingCount = MathUtils.getRandomNumberInRange(9, 11);
        int wingBpCount = MathUtils.getRandomNumberInRange(1, 2);
        int weaponCount = MathUtils.getRandomNumberInRange(25, 27);
        int weaponBpCount = MathUtils.getRandomNumberInRange(2, 3);

        FactionAPI stockFaction = getHegemony();
        if (stockFaction == null) return;

        addShips(stockFaction, MARKET_SELL_BLUEPRINTS, shipCount, shipBpCount);
        addRandomWings(stockFaction, MARKET_SELL_BLUEPRINTS, wingCount, wingBpCount);
        addRandomWeapons(stockFaction, MARKET_SELL_BLUEPRINTS, weaponCount, weaponBpCount);

        cargo.sort();
    }

    private boolean isShipAllowed(ShipHullSpecAPI spec) {
        if (spec.hasTag(Tags.RESTRICTED)) return false;
        if (spec.hasTag(Tags.UNRECOVERABLE)) return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE) && !spec.hasTag(Tags.AUTOMATED_RECOVERABLE))
            return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) return false;

        return true;
    }

    private void addShips(FactionAPI faction, boolean sellBlueprints, int shipCount, int shipBpCount) {
        WeightedRandomPicker<String> selectedShips = new WeightedRandomPicker<>(itemGenRandom);
        WeightedRandomPicker<String> selectedShipBps = new WeightedRandomPicker<>(itemGenRandom);

        Set<String> knownShips = faction.getKnownShips();

        if (!knownShips.isEmpty()) {
            for (String shipId : knownShips) {
                ShipHullSpecAPI spec = Global.getSettings().getHullSpec(shipId);
                if (spec == null || !isShipAllowed(spec)) continue;

                if (spec.getManufacturer().equals("XIV Battlegroup")) {
                    selectedShips.add(shipId, getWeight(spec));

                    if (!Global.getSector().getPlayerFaction().getKnownShips().contains(shipId)) {
                        selectedShipBps.add(shipId, getWeight(spec));
                    }
                }
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

            for (int i = 0; i < shipBpCount; i++) {
                if (!sellBlueprints) break;
                if (selectedShipBps.isEmpty()) break;
                String shipId = selectedShipBps.pickAndRemove();
                cargo.addSpecial(new SpecialItemData(SHIP_BP, shipId), 1);
            }
        }
    }

    private void addRandomWings(FactionAPI faction, boolean sellBlueprints, int wingCount, int wingBpCount) {
        CargoAPI cargo = getCargo();
        Set<String> knownFighters = faction.getKnownFighters();

        WeightedRandomPicker<String> pickedWings = new WeightedRandomPicker<>(itemGenRandom);
        WeightedRandomPicker<String> pickedBlueprints = new WeightedRandomPicker<>(itemGenRandom);

        if (!knownFighters.isEmpty()) {
            for (String wingId : knownFighters) {
                FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wingId);
                float weight = 1.0f + (spec.getTier() + 1) * 3.0f;

                if (spec.hasTag(Tags.NO_DROP))
                    continue;

                if (spec.getVariant() != null) {
                    ShipHullSpecAPI fighterHullSpec = spec.getVariant().getHullSpec();

                    if (fighterHullSpec.getManufacturer().equals("XIV Battlegroup") || fighterHullSpec.getManufacturer().equals("Iron Shell"))
                        weight = weight * 3f;
                    else if (fighterHullSpec.getManufacturer().equals("Hegemony"))
                        weight = weight * 2f;

                }

                pickedWings.add(wingId, weight);
                pickedWings.add(wingId, weight);

                if (!Global.getSector().getPlayerFaction().getKnownFighters().contains(wingId)) {
                    pickedBlueprints.add(wingId);
                }
            }

            for (int i = 0; i < wingCount; i++) {
                if (pickedWings.isEmpty()) break;
                String wingId = pickedWings.pickAndRemove();
                int quantity = 2;
                cargo.addItems(CargoAPI.CargoItemType.FIGHTER_CHIP, wingId, quantity);
            }

            for (int i = 0; i < wingBpCount; i++) {
                if (!sellBlueprints) break;
                if (pickedBlueprints.isEmpty()) break;
                String weaponId = pickedBlueprints.pickAndRemove();
                cargo.addSpecial(new SpecialItemData(FIGHTER_BP, weaponId), 1);
            }
        }
    }

    private void addRandomWeapons(FactionAPI faction, boolean sellBlueprints, int weaponCount, int weaponBpCount) {
        CargoAPI cargo = getCargo();
        List<String> weaponIds = Global.getSector().getAllWeaponIds();

        WeightedRandomPicker<String> pickedWeapons = new WeightedRandomPicker<>(itemGenRandom);
        WeightedRandomPicker<String> pickedBlueprints = new WeightedRandomPicker<>(itemGenRandom);

        for (String id : weaponIds) {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            if (isWeaponAllowed(spec, faction)) {
                float weight = 1.0f + (spec.getTier() + 1) * 3.0f;

                pickedWeapons.add(spec.getWeaponId(), weight);
                pickedWeapons.add(spec.getWeaponId(), weight);

                if (!Global.getSector().getPlayerFaction().getKnownWeapons().contains(spec.getWeaponId())) {
                    pickedBlueprints.add(spec.getWeaponId());
                }
            }
        }

        for (int i = 0; i < weaponCount; i++) {
            if (pickedWeapons.isEmpty()) break;
            String weaponId = pickedWeapons.pickAndRemove();
            int quantity = 2;
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(weaponId);
            if (spec.getSize() == WeaponAPI.WeaponSize.SMALL) quantity = 8;
            else if (spec.getSize() == WeaponAPI.WeaponSize.MEDIUM) quantity = 4;
            cargo.addWeapons(weaponId, quantity);
        }

        for (int i = 0; i < weaponBpCount; i++) {
            if (!sellBlueprints) break;
            if (pickedBlueprints.isEmpty()) break;
            String weaponId = pickedBlueprints.pickAndRemove();
            cargo.addSpecial(new SpecialItemData(WEAPON_BP, weaponId), 1);
        }
    }

    private boolean isWeaponAllowed(WeaponSpecAPI spec, FactionAPI faction) {
        if (spec.hasTag(Tags.NO_DROP)) return false;
        if (spec.hasTag(Tags.RESTRICTED)) return false;
        if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) return false;
        if (!faction.getKnownWeapons().contains(spec.getWeaponId())) return false;
        return spec.getTier() >= 2;
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        FactionAPI hegemony = getHegemony();
        if (hegemony == null) return false;

        //boolean transponderOn = Global.getSector().getPlayerFleet().isTransponderOn();
        RepLevel level = hegemony.getRelationshipLevel(Global.getSector().getPlayerFaction());

        boolean hasCommission = hegemony.getId().equals(Misc.getCommissionFactionId());
        boolean hasGoodStanding = level.isAtWorst(MIN_STANDING);

        //return transponderOn && (hasCommission || hasGoodStanding);
        return hasCommission || hasGoodStanding;
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        FactionAPI hegemony = getHegemony();
        if (hegemony == null) return "";

        //boolean transponderOn = Global.getSector().getPlayerFleet().isTransponderOn();
        RepLevel level = hegemony.getRelationshipLevel(Global.getSector().getPlayerFaction());
        boolean hasCommission = hegemony.getId().equals(Misc.getCommissionFactionId());
        boolean hasGoodStanding = level.isAtWorst(MIN_STANDING);

        //if (transponderOn && (hasCommission || hasGoodStanding)) {
        //    return super.getTooltipAppendix(ui);
        //}

        if (hasCommission || hasGoodStanding) {
            return super.getTooltipAppendix(ui);
        }

        String text = "";
        //if (!transponderOn) {
        //    text += "Requires: Transponder On\n";
        //}
        if (!hasCommission || !hasGoodStanding) {
            text += "Requires: " + hegemony.getDisplayName() + " Commission OR " + MIN_STANDING.getDisplayName().toLowerCase() + " reputation";
        }
        return text.trim();
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "No refunds!";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "No refunds!";
    }

    @Override
    public float getTariff() {
        return MARKET_TARIFF;
    }

    @Override
    public String getName() {
        return "XIV BattleGroup" + "\n" + "Elite Shipyard";
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