package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicSettings;

import java.io.IOException;
import java.util.*;

import static com.fs.starfarer.api.combat.WeaponAPI.WeaponType.*;
import static com.fs.starfarer.api.impl.campaign.ids.Items.WEAPON_BP;
import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

public class ExtraStoreWeaponBazaar extends BaseSubmarketPlugin {

    private static final List<String> WEAPON_BLACKLIST =
            MagicSettings.getList(ESM_MOD_ID, "weaponBazaarBlacklist");
    private static final boolean MARKET_SELL_BLUEPRINTS =
            MagicSettings.getBoolean(ESM_MOD_ID, "weaponBazaarSellBlueprints");
    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "weaponBazaarTariff");

    public ExtraStoreWeaponBazaar() {
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

        int lowTierGunsCount = 70;
        int lowTierBpsCount = MathUtils.getRandomNumberInRange(4, 5);

        int highTierGunsCount = 30;
        int highTierBpsCount = MathUtils.getRandomNumberInRange(1, 2);


        addRandomWeapons(lowTierGunsCount, false);
        addRandomWeapons(highTierGunsCount, true);

        if (MARKET_SELL_BLUEPRINTS) {
            addRandomWeaponBlueprints(lowTierBpsCount, false);
            addRandomWeaponBlueprints(highTierBpsCount, true);
        }

        cargo.sort();
    }

    private boolean isWeaponAllowedCommon(WeaponSpecAPI spec) {
        if (spec == null) return false;

        if (WEAPON_BLACKLIST.contains(spec.getWeaponId())) return false;

        WeaponAPI.WeaponType type = spec.getType();
        if (type == BUILT_IN || type == DECORATIVE || type == LAUNCH_BAY ||
                type == STATION_MODULE || type == SYSTEM) {
            return false;
        }

        if (spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.RESTRICTED)) return false;
        if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) return false;
        if (spec.getOrdnancePointCost(null, null) <= 0) return false;

        return true;
    }

    private boolean isWeaponAllowed(WeaponSpecAPI spec) {
        return isWeaponAllowedCommon(spec) && spec.getTier() <= 2;
    }

    private boolean isWeaponAllowedHighTier(WeaponSpecAPI spec) {
        return isWeaponAllowedCommon(spec) && spec.getTier() >= 3;
    }

    private void addRandomWeapons(int max, boolean isHighTier) {
        CargoAPI cargo = getCargo();
        List<String> weaponIds = Global.getSector().getAllWeaponIds();

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(itemGenRandom);

        for (String id : weaponIds) {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            boolean isWeaponAllowed;

            if (isHighTier)
                isWeaponAllowed = isWeaponAllowedHighTier(spec);
            else
                isWeaponAllowed = isWeaponAllowed(spec);

            if (isWeaponAllowed) {
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

    private void addRandomWeaponBlueprints(int max, boolean isHighTier) {
        CargoAPI cargo = getCargo();
        List<String> weaponIds = Global.getSector().getAllWeaponIds();

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(itemGenRandom);

        for (String id : weaponIds) {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            boolean isWeaponAllowed;

            if (isHighTier)
                isWeaponAllowed = isWeaponAllowedHighTier(spec);
            else
                isWeaponAllowed = isWeaponAllowed(spec);

            if (isWeaponAllowed) {
                float weight = 10 - spec.getTier();    // so 6-8

                weight *= spec.getRarity();

                if (!Global.getSector().getPlayerFaction().getKnownWeapons().contains(spec.getWeaponId())) {
                    picker.add(spec.getWeaponId(), weight);
                }
            }
        }
        for (int i = 0; i < max; i++) {
            if (picker.isEmpty()) break;
            String weaponId = picker.pickAndRemove();
            cargo.addSpecial(new SpecialItemData(WEAPON_BP, weaponId), 1);
        }
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
        return market.getName() + "\n" + "Weapon Bazaar";
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