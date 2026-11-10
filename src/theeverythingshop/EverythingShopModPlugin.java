package theeverythingshop;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import theeverythingshop.compat.ExoticaCompat;
import theeverythingshop.world.GateOfBabylon;
import theeverythingshop.world.submarkets.ExtraStoreAICores;
import theeverythingshop.world.submarkets.TreasuryShopBase;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.lwjgl.util.vector.Vector2f;

public class EverythingShopModPlugin extends BaseModPlugin implements LunaSettingsListener {

    public static final String MOD_ID = "the_everything_shop";
    public static final Vector2f SYSTEM_LOC = new Vector2f(-27000f, -20000f);
    public static final String SYSTEM_NAME = "Gate of Babylon";

    /** Add or remove a submarket on a station's market based on its LunaLib toggle. */
    private void register(String stationId, String submarketId, String settingId, boolean def) {
        register(stationId, submarketId, settingId, def, true);
    }

    /** As above; {@code available} false always removes the submarket (its mod is missing). */
    private void register(String stationId, String submarketId, String settingId, boolean def, boolean available) {
        SectorEntityToken entity = Global.getSector().getEntityById(stationId);
        if (entity == null || entity.getMarket() == null) return;

        MarketAPI market = entity.getMarket();
        boolean isEnabled = available && ShopSettings.enabled(settingId, def);

        if (isEnabled) {
            if (!market.hasSubmarket(submarketId)) market.addSubmarket(submarketId);
        } else {
            if (market.hasSubmarket(submarketId)) market.removeSubmarket(submarketId);
        }
    }

    private void marketsInitialSetup() {
        // --- AI store ---
        register("tes_ai_store", "tes_redacted_roulette", "tes_enable_redacted_roulette", true);
        register("tes_ai_store", "tes_ai_derelict", "tes_enable_ai_derelict", true);
        register("tes_ai_store", "tes_ai_remnant", "tes_enable_ai_remnant", true);
        register("tes_ai_store", "tes_ai_cores", "tes_enable_ai_cores", true);
        register("tes_ai_store", "tes_ai_omega", "tes_enable_ai_omega", true);

        // --- Elite store ---
        register("tes_elite_store", "tes_elite_roulette", "tes_enable_elite_roulette", true);
        register("tes_elite_store", "tes_phase_emporium", "tes_enable_phase_emporium", true);
        register("tes_elite_store", "tes_xiv", "tes_enable_xiv", true);
        register("tes_elite_store", "tes_lions_guard", "tes_enable_lions_guard", true);

        // --- General store ---
        register("tes_general_store", "tes_weapon_bazaar", "tes_enable_weapon_bazaar", true);
        register("tes_general_store", "tes_faction_roulette", "tes_enable_faction_roulette", true);
        register("tes_general_store", "tes_player_market", "tes_enable_player_market", true);

        // --- Ship Procurement Station: every non-automated hull, split by class ---
        register("tes_ship_procurement", "tes_ship_frigate", "tes_enable_ship_frigate", true);
        register("tes_ship_procurement", "tes_ship_destroyer", "tes_enable_ship_destroyer", true);
        register("tes_ship_procurement", "tes_ship_cruiser", "tes_enable_ship_cruiser", true);
        register("tes_ship_procurement", "tes_ship_capital", "tes_enable_ship_capital", true);

        // --- AI Ship Procurement Station: every automated hull, split by class ---
        register("tes_ai_ship_procurement", "tes_aiship_frigate", "tes_enable_aiship_frigate", true);
        register("tes_ai_ship_procurement", "tes_aiship_destroyer", "tes_enable_aiship_destroyer", true);
        register("tes_ai_ship_procurement", "tes_aiship_cruiser", "tes_enable_aiship_cruiser", true);
        register("tes_ai_ship_procurement", "tes_aiship_capital", "tes_enable_aiship_capital", true);

        // --- Weapon Procurement Stations: every weapon, split by size ---
        register("tes_weap_ballistic", "tes_weap_ballistic_small", "tes_enable_weap_ballistic_small", true);
        register("tes_weap_ballistic", "tes_weap_ballistic_medium", "tes_enable_weap_ballistic_medium", true);
        register("tes_weap_ballistic", "tes_weap_ballistic_large", "tes_enable_weap_ballistic_large", true);

        register("tes_weap_energy", "tes_weap_energy_small", "tes_enable_weap_energy_small", true);
        register("tes_weap_energy", "tes_weap_energy_medium", "tes_enable_weap_energy_medium", true);
        register("tes_weap_energy", "tes_weap_energy_large", "tes_enable_weap_energy_large", true);

        register("tes_weap_missile", "tes_weap_missile_small", "tes_enable_weap_missile_small", true);
        register("tes_weap_missile", "tes_weap_missile_medium", "tes_enable_weap_missile_medium", true);
        register("tes_weap_missile", "tes_weap_missile_large", "tes_enable_weap_missile_large", true);

        // --- Fighter Procurement Station: every wing, split by role ---
        register("tes_fighter_store", "tes_fighter_bomber", "tes_enable_fighter_bomber", true);
        register("tes_fighter_store", "tes_fighter_fighter", "tes_enable_fighter_fighter", true);
        register("tes_fighter_store", "tes_fighter_interceptor", "tes_enable_fighter_interceptor", true);
        register("tes_fighter_store", "tes_fighter_assault", "tes_enable_fighter_assault", true);
        register("tes_fighter_store", "tes_fighter_support", "tes_enable_fighter_support", true);

        // --- The Treasury: special items, blueprints, modspecs and Exotica ---
        register(GateOfBabylon.TREASURY_ID, "tes_treasury_specials", "tes_enable_treasury_specials", true);
        register(GateOfBabylon.TREASURY_ID, "tes_treasury_blueprints", "tes_enable_treasury_blueprints", true);
        register(GateOfBabylon.TREASURY_ID, "tes_treasury_modspecs", "tes_enable_treasury_modspecs", true);
        boolean exotica = ExoticaCompat.isAvailable();
        register(GateOfBabylon.TREASURY_ID, "tes_treasury_exotics", "tes_enable_treasury_exotics", true, exotica);
        register(GateOfBabylon.TREASURY_ID, "tes_treasury_upgrades", "tes_enable_treasury_upgrades", true, exotica);
    }

    @Override
    public void onNewGameAfterProcGen() {
        GateOfBabylon.generateGreatMallSystem();

        StarSystemAPI shopSystem = Global.getSector().getStarSystem(SYSTEM_NAME);
        if (shopSystem != null) {
            GateOfBabylon.validateLocation(Global.getSector(), shopSystem, SYSTEM_LOC);
        }

        marketsInitialSetup();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        marketsInitialSetup();
        // Apply enable/disable toggles live when the player changes them in the LunaLib menu.
        try {
            if (!LunaSettings.hasSettingsListenerOfClass(this.getClass())) {
                LunaSettings.addSettingsListener(this);
            }
        } catch (Throwable t) {
            // LunaLib not available; settings still apply on the next load via marketsInitialSetup.
        }
    }

    @Override
    public void beforeGameSave() {
        try {
            for (String stationId : new String[] { GateOfBabylon.TREASURY_ID, "tes_ai_store" }) {
                SectorEntityToken entity = Global.getSector().getEntityById(stationId);
                if (entity == null || entity.getMarket() == null) continue;
                for (SubmarketAPI submarket : entity.getMarket().getSubmarketsCopy()) {
                    SubmarketPlugin plugin = submarket.getPlugin();
                    if (plugin instanceof TreasuryShopBase) {
                        ((TreasuryShopBase) plugin).clearForSave();
                    } else if (plugin instanceof ExtraStoreAICores) {
                        ((ExtraStoreAICores) plugin).clearForSave();
                    }
                }
            }
        } catch (Throwable t) {
            Global.getLogger(EverythingShopModPlugin.class).error("Could not empty the shops before saving", t);
        }
    }

    /** LunaSettingsListener: re-apply submarket toggles when settings change in-game. */
    @Override
    public void settingsChanged(String modId) {
        if (!MOD_ID.equals(modId)) return;
        if (Global.getSector() == null) return;
        try {
            marketsInitialSetup();
        } catch (Throwable t) {
            // never let a settings callback crash the game
        }
    }
}
