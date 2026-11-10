package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.magiclib.util.MagicSettings;

import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static data.scripts.ESMVanillaModPlugin.ESM_MOD_ID;

/**
 * AI Cores Store: every AI core from every installed mod, 999 of each, topped up on every visit.
 * A commodity is a core if it has the ai_core tag, the ai_cores demand class, or a core
 * officer or admin plugin (RAT, Tahlan, Crab Shack and others only have the plugin).
 */
public class ExtraStoreAICores extends BaseSubmarketPlugin {

    public static final Logger log = Global.getLogger(ExtraStoreAICores.class);
    public static final int STOCK = 999;

    public static float MARKET_TARIFF = MagicSettings.getFloat(ESM_MOD_ID, "aiCoreMarketTariff");

    public ExtraStoreAICores() {
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();
        cargo.clear();
        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();

        Set<String> single = new HashSet<String>(settingList("aiCoreSingle"));
        for (String id : findAICoreIds()) {
            try {
                boolean one = single.contains(id) || id.startsWith("sotf_ichip_");
                cargo.addCommodity(id, one ? 1 : STOCK);
            } catch (Throwable t) {
                log.warn("Skipping AI core " + id, t);
            }
        }
        cargo.removeEmptyStacks();
        cargo.sort();
    }

    public static List<String> findAICoreIds() {
        Set<String> exclude = new HashSet<String>(settingList("aiCoreExclude"));
        Set<String> result = new LinkedHashSet<String>();
        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            String id = spec.getId();
            if (id == null || exclude.contains(id) || id.endsWith("_nodrop")
                    || id.contains("playercore") || id.contains("player_core")) {
                continue;
            }
            // SotF instance chips are named characters: only the ones the player can own
            if (id.startsWith("sotf_ichip_") && spec.hasTag("no_drop")) {
                continue;
            }
            boolean core = spec.hasTag(Commodities.TAG_AI_CORE) || Commodities.AI_CORES.equals(spec.getDemandClass());
            if (!core) {
                try {
                    core = Misc.getAICoreOfficerPlugin(id) != null || Misc.getAICoreAdminPlugin(id) != null;
                } catch (Throwable t) {
                    // a broken plugin from another mod: not a core we can sell
                }
            }
            if (core) {
                result.add(id);
            }
        }
        for (String id : settingList("aiCoreExtra")) {
            try {
                if (Global.getSettings().getCommoditySpec(id) != null) {
                    result.add(id);
                }
            } catch (Throwable t) {
                // not installed
            }
        }
        return new ArrayList<String>(result);
    }

    /** Empty the shelves before a save; they fill again on the next visit. */
    public void clearForSave() {
        getCargo().clear();
        getCargo().removeEmptyStacks();
    }

    private static List<String> settingList(String key) {
        try {
            List<String> list = MagicSettings.getList(ESM_MOD_ID, key);
            if (list != null) {
                return list;
            }
        } catch (Throwable t) {
            // missing setting
        }
        return Collections.<String>emptyList();
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