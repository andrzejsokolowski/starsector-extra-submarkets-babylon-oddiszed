package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.magiclib.util.MagicSettings;

import java.util.Map;

import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

public class ExtraStoreAICores extends BaseSubmarketPlugin {

    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "aiCoreMarketTariff");
    public static Map<String, Float> AI_CORE_LIST = MagicSettings.getFloatMap(ESM_MOD_ID, "aiCoreList");

    public ExtraStoreAICores() {
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

        for (Map.Entry<String, Float> entry : AI_CORE_LIST.entrySet()) {
            String itemId = entry.getKey();
            float rawAmount = entry.getValue();

            if (Global.getSettings().getCommoditySpec(itemId) != null) {
                int amount = (int) rawAmount;
                cargo.addCommodity(itemId, amount);
            }
        }

        cargo.sort();
    }

    //==========================================================================
    //==========================================================================
    @Override
    public String getName() {
        return "AI Core Store" + "\n" + "'Christmas Store'";
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