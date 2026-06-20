package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicSettings;

import java.util.HashSet;
import java.util.Set;

import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

public class ExtraStorePhaseEmporium extends BaseSubmarketPlugin {

    private static final boolean MARKET_SELL_BLUEPRINTS =
            MagicSettings.getBoolean(ESM_MOD_ID, "phaseEmporiumSellBlueprints");
    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "phaseEmporiumTariff");

    public ExtraStorePhaseEmporium() {
    }

    //private float getWeight(ShipHullSpecAPI spec) {
    //    float fp = spec.getFleetPoints();
    //    if (fp <= 0) fp = 1f;
    //
    //    float weight = 100f / (float) Math.sqrt(fp);
    //
    //    return weight;
    //}

    private float getWeight(ShipHullSpecAPI spec) {
        float fp = spec.getFleetPoints();
        if (fp <= 0) fp = 1f;

        return 100f / (float) Math.log1p(fp);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        //log.info("Days since update: " + sinceLastCargoUpdate);
        if (sinceLastCargoUpdate < 30) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();

        // Empty Store
        cargo.clear();
        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();

        int shipCount = MathUtils.getRandomNumberInRange(24, 28);
        int shipBpCount = MathUtils.getRandomNumberInRange(1, 2);
        this.addShips(MARKET_SELL_BLUEPRINTS, shipCount, shipBpCount);

        cargo.sort();
    }

    private boolean hasPhaseCloakSystem(ShipHullSpecAPI ship) {
        String systemId = ship.getShipSystemId();
        if (systemId == null || systemId.isEmpty()) return false;

        try {
            return Global.getSettings().getShipSystemSpec(systemId).isPhaseCloak();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isShipAllowed(ShipHullSpecAPI spec) {
        if (spec.hasTag(Tags.RESTRICTED)) return false;
        if (spec.hasTag(Tags.UNRECOVERABLE)) return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE) && !spec.hasTag(Tags.AUTOMATED_RECOVERABLE))
            return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) return false;
        if (spec.getHullSize().equals(ShipAPI.HullSize.DEFAULT)) return false; //This is needed apparently
        if (spec.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) return false;

        return spec.isPhase() && !hasPhaseCloakSystem(spec);
    }

    private void addShips(boolean sellBlueprints, int shipCount, int shipBpCount) {
        WeightedRandomPicker<String> selectedShips = new WeightedRandomPicker<>(this.itemGenRandom);
        WeightedRandomPicker<String> selectedShipBps = new WeightedRandomPicker<>(this.itemGenRandom);

        Set<String> factionsWithMarkets = new HashSet<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getFactionId() != null) factionsWithMarkets.add(market.getFactionId());
        }

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction.getKnownShips().isEmpty() || !faction.isShowInIntelTab() || !factionsWithMarkets.contains(faction.getId()) || faction.isPlayerFaction())
                continue;

            for (String shipId : faction.getKnownShips()) {
                ShipHullSpecAPI spec = Global.getSettings().getHullSpec(shipId);
                if (spec == null || !isShipAllowed(spec)) continue;

                float weight = getWeight(spec);

                if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.CIVILIAN) ||
                        spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.TANKER) ||
                        spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.FREIGHTER)) {
                    weight *= 0.5f;
                }

                selectedShips.add(shipId, weight);

                if (!Global.getSector().getPlayerFaction().getKnownShips().contains(shipId)) {
                    selectedShipBps.add(shipId, weight);
                }
            }
        }

        for (int i = 0; i < shipCount && !selectedShips.isEmpty(); i++) {
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, selectedShips.pick(itemGenRandom) + "_Hull");
            member.getRepairTracker().setMothballed(true);
            member.getRepairTracker().setCR(0.5f);

            cargo.getMothballedShips().addFleetMember(member);
        }
        for (int i = 0; i < shipBpCount && sellBlueprints && !selectedShipBps.isEmpty(); i++) {
            String shipId = selectedShipBps.pickAndRemove();
            this.cargo.addSpecial(new SpecialItemData("ship_bp", shipId), 1);
        }
    }

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
        return market.getName() + "\n" + "Phase Emporium";
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

