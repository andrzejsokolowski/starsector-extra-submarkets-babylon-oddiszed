package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicSettings;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.*;

import static com.fs.starfarer.api.impl.campaign.ids.Items.*;
import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

public class ExtraStoreFactionRoulette extends BaseSubmarketPlugin {

    private String factionName = "None";
    private String currentFactionId = null;

    public static final List<String> FACTION_WHITELIST = MagicSettings.getList(ESM_MOD_ID, "factionRouletteWhitelist");
    public static final List<String> FACTION_BLACKLIST = MagicSettings.getList(ESM_MOD_ID, "factionRouletteBlacklist");
    public static boolean MARKET_SELL_BLUEPRINTS = MagicSettings.getBoolean(ESM_MOD_ID, "factionRouletteSellBlueprints");
    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "factionRouletteTariff");

    public ExtraStoreFactionRoulette() {
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        return "Selected Faction: " + factionName;
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        if (sinceLastCargoUpdate < 30) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();
        cargo.clear();
        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();

        Set<String> potentialFactionIds = new HashSet<>();

        Set<String> factionsWithMarkets = new HashSet<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getFactionId() != null) factionsWithMarkets.add(market.getFactionId());
        }
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction.isPlayerFaction()) continue;
            if (!faction.getKnownShips().isEmpty() && faction.isShowInIntelTab() && factionsWithMarkets.contains(faction.getId())) {
                potentialFactionIds.add(faction.getId());
            }
        }

        for (String fId : FACTION_WHITELIST) {
            FactionAPI f = Global.getSector().getFaction(fId);
            if (f != null && !f.getKnownShips().isEmpty()) potentialFactionIds.add(fId);
        }

        FACTION_BLACKLIST.forEach(potentialFactionIds::remove);

        List<String> sortedFactions = new ArrayList<>(potentialFactionIds);
        Collections.sort(sortedFactions);

        if (sortedFactions.isEmpty()) {
            sortedFactions.add("independent");
        }

        if (currentFactionId != null && sortedFactions.contains(currentFactionId)) {
            int prevIndex = sortedFactions.indexOf(currentFactionId);
            int nextIndex = (prevIndex + 1) % sortedFactions.size();
            currentFactionId = sortedFactions.get(nextIndex);
        } else {
            currentFactionId = sortedFactions.get(0);
        }

        FactionAPI selectedFaction = Global.getSector().getFaction(currentFactionId);
        factionName = (selectedFaction != null) ? selectedFaction.getDisplayName() : "Independent";

        int shipCount = MathUtils.getRandomNumberInRange(18, 24);
        int shipBpCount = MathUtils.getRandomNumberInRange(1, 2);
        int wingCount = MathUtils.getRandomNumberInRange(10, 15);
        int wingBpCount = MathUtils.getRandomNumberInRange(1, 1);
        int weaponCount = MathUtils.getRandomNumberInRange(20, 40);
        int weaponBpCount = MathUtils.getRandomNumberInRange(2, 3);

        if (selectedFaction != null) {
            addShips(selectedFaction, MARKET_SELL_BLUEPRINTS, shipCount, shipBpCount);
            addRandomWings(selectedFaction, MARKET_SELL_BLUEPRINTS, wingCount, wingBpCount);
            addRandomWeapons(selectedFaction, MARKET_SELL_BLUEPRINTS, weaponCount, weaponBpCount);
        }

        cargo.sort();
    }

    public boolean isShipAllowed(FleetMemberAPI member, FactionAPI faction) {
        if (member.getHullSpec().hasTag(Tags.RESTRICTED)) return false;
        if (member.getHullSpec().hasTag(Tags.UNRECOVERABLE)) return false;
        if (member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE) && !member.getHullSpec().hasTag(Tags.AUTOMATED_RECOVERABLE))
            return false;
        if (member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) return false;
        if (!faction.getKnownShips().contains(member.getHullSpec().getHullId())) return false;

        return true;
    }

    protected void addShips(FactionAPI faction, boolean sellBlueprints, int shipCount, int shipBpCount) {
        CargoAPI cargo = getCargo();
        WeightedRandomPicker<String> rolePicker = new WeightedRandomPicker<>(itemGenRandom);

        rolePicker.add(ShipRoles.COMBAT_SMALL, 40f);
        rolePicker.add(ShipRoles.COMBAT_MEDIUM, 50f);
        rolePicker.add(ShipRoles.COMBAT_LARGE, 40f);
        rolePicker.add(ShipRoles.COMBAT_CAPITAL, 20f);
        rolePicker.add(ShipRoles.CARRIER_SMALL, 5f);
        rolePicker.add(ShipRoles.CARRIER_MEDIUM, 7f);
        rolePicker.add(ShipRoles.CARRIER_LARGE, 8f);
        rolePicker.add(ShipRoles.PHASE_SMALL, 5f);
        rolePicker.add(ShipRoles.PHASE_MEDIUM, 7f);
        rolePicker.add(ShipRoles.PHASE_LARGE, 5f);
        rolePicker.add(ShipRoles.PHASE_CAPITAL, 3f);

        WeightedRandomPicker<String> bpPicker = new WeightedRandomPicker<>(itemGenRandom);

        int tries = 0;

        while (cargo.getMothballedShips().getNumMembers() < shipCount) {
            String role = rolePicker.pick();
            java.util.List<ShipRolePick> picks = null;
            try {
                picks = faction.pickShip(role, FactionAPI.ShipPickParams.priority());
            } catch (Exception e) {
            }

            if (picks != null) {
                for (ShipRolePick pick : picks) {
                    String variantId = pick.variantId;
                    FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
                    String hullId = member.getHullSpec().getHullId();
                    member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, hullId + "_Hull");

                    if (isShipAllowed(member, faction)) {
                        member.getRepairTracker().setMothballed(true);
                        member.getRepairTracker().setCR(0.5f);
                        cargo.getMothballedShips().addFleetMember(member);

                        // Add BPs
                        if (!Global.getSector().getPlayerFaction().getKnownShips().contains(hullId)) {
                            bpPicker.add(hullId);
                        }
                    }
                }
            }
            if (tries++ > shipCount + 40) break;
        }

        // Add BPs
        if (sellBlueprints) {
            for (int i = 0; i < shipBpCount; i++) {
                if (bpPicker.isEmpty()) break;
                cargo.addSpecial(new SpecialItemData(SHIP_BP, bpPicker.pickAndRemove()), 1);
            }
        }
    }

    protected void addRandomWings(FactionAPI faction, boolean sellBlueprints, int wingCount, int wingBpCount) {
        CargoAPI cargo = getCargo();
        Set<String> knownFighters = faction.getKnownFighters();
        WeightedRandomPicker<String> pickedWings = new WeightedRandomPicker<>(itemGenRandom);
        WeightedRandomPicker<String> pickedBlueprints = new WeightedRandomPicker<>(itemGenRandom);

        if (!knownFighters.isEmpty()) {
            for (String wingId : knownFighters) {
                FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wingId);
                if (spec.hasTag(Tags.NO_DROP)) continue;
                float weight = 10 - spec.getTier();

                if (spec.hasTag(TAG_BASE_BP) || spec.hasTag(TAG_LOWTECH_BP) || spec.hasTag(TAG_MIDLINE_BP) || spec.hasTag(TAG_HIGHTECH_BP))
                    weight = weight * 0.33f;

                pickedWings.add(wingId, weight);

                if (!Global.getSector().getPlayerFaction().getKnownFighters().contains(wingId)) {
                    pickedBlueprints.add(wingId);
                }
            }

            for (int i = 0; i < wingCount; i++) {
                if (pickedWings.isEmpty()) break;
                cargo.addItems(CargoAPI.CargoItemType.FIGHTER_CHIP, pickedWings.pickAndRemove(), 2);
            }
            if (sellBlueprints) {
                for (int i = 0; i < wingBpCount; i++) {
                    if (pickedBlueprints.isEmpty()) break;
                    cargo.addSpecial(new SpecialItemData(FIGHTER_BP, pickedBlueprints.pickAndRemove()), 1);
                }
            }
        }
    }

    protected void addRandomWeapons(FactionAPI faction, boolean sellBlueprints, int weaponCount, int weaponBpCount) {
        CargoAPI cargo = getCargo();
        WeightedRandomPicker<String> pickedWeapons = new WeightedRandomPicker<>(itemGenRandom);
        WeightedRandomPicker<String> pickedBlueprints = new WeightedRandomPicker<>(itemGenRandom);

        for (String id : faction.getKnownWeapons()) {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            if (isWeaponAllowed(spec, faction)) {
                float weight = 10 - spec.getTier();

                if (spec.hasTag(TAG_BASE_BP) || spec.hasTag(TAG_LOWTECH_BP) || spec.hasTag(TAG_MIDLINE_BP) || spec.hasTag(TAG_HIGHTECH_BP))
                    weight = weight * 0.33f;

                pickedWeapons.add(id, weight);

                if (!Global.getSector().getPlayerFaction().getKnownWeapons().contains(id)) {
                    pickedBlueprints.add(id);
                }
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

        if (sellBlueprints) {
            for (int i = 0; i < weaponBpCount; i++) {
                if (pickedBlueprints.isEmpty()) break;
                cargo.addSpecial(new SpecialItemData(WEAPON_BP, pickedBlueprints.pickAndRemove()), 1);
            }
        }
    }

    public boolean isWeaponAllowed(WeaponSpecAPI spec, FactionAPI faction) {
        if (spec.hasTag(Tags.NO_DROP)) return false;
        if (spec.hasTag(Tags.RESTRICTED)) return false;
        if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) return false;
        if (!faction.getKnownWeapons().contains(spec.getWeaponId())) return false;
        return true;
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
        return "Faction of" + "\n" + "The Month";
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