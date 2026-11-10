package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.chicken.Chicken;
import data.scripts.compat.ExoticaCompat;
import data.scripts.world.ExtraStoreSystem;
import data.scripts.world.submarkets.ExtraStoreAICores;
import data.scripts.world.submarkets.ExtraStoreChickenBase;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.lwjgl.util.vector.Vector2f;

public class ESMVanillaModPlugin extends BaseModPlugin implements LunaSettingsListener {

    public static final String ESM_MOD_ID = "MM_ESM_Vanilla";
    public static final Vector2f ESM_SYSTEM_LOC = new Vector2f(-27000f, -20000f);
    public static final String ESM_SYSTEM_NAME = "Gilgamesh Nebula";

    /** Add or remove a submarket on a station's market based on its LunaLib toggle. */
    private void register(String stationId, String submarketId, String settingId, boolean def) {
        register(stationId, submarketId, settingId, def, true);
    }

    /** As above; {@code available} false always removes the submarket (its mod is missing). */
    private void register(String stationId, String submarketId, String settingId, boolean def, boolean available) {
        SectorEntityToken entity = Global.getSector().getEntityById(stationId);
        if (entity == null || entity.getMarket() == null) return;

        MarketAPI market = entity.getMarket();
        boolean isEnabled = available && ESMSettings.enabled(settingId, def);

        if (isEnabled) {
            if (!market.hasSubmarket(submarketId)) market.addSubmarket(submarketId);
        } else {
            if (market.hasSubmarket(submarketId)) market.removeSubmarket(submarketId);
        }
    }

    private void marketsInitialSetup() {
        // --- AI store ---
        register("esm_ai_store", "extra_submarkets_redacted_roulette", "esm_enable_redacted_roulette", true);
        register("esm_ai_store", "extra_submarkets_ai_derelict", "esm_enable_ai_derelict", true);
        register("esm_ai_store", "extra_submarkets_ai_remnant", "esm_enable_ai_remnant", true);
        register("esm_ai_store", "extra_submarkets_ai_cores", "esm_enable_ai_cores", true);
        register("esm_ai_store", "extra_submarkets_ai_omega", "esm_enable_ai_omega", true);

        // --- Elite store ---
        register("esm_elite_store", "extra_submarkets_elite_roulette", "esm_enable_elite_roulette", true);
        register("esm_elite_store", "extra_submarkets_phase_emporium", "esm_enable_phase_emporium", true);
        register("esm_elite_store", "extra_submarkets_xiv", "esm_enable_xiv", true);
        register("esm_elite_store", "extra_submarkets_lions_guard", "esm_enable_lions_guard", true);

        // --- General store ---
        register("esm_general_store", "extra_submarkets_weapon_bazaar", "esm_enable_weapon_bazaar", true);
        register("esm_general_store", "extra_submarkets_faction_roulette", "esm_enable_faction_roulette", true);
        register("esm_general_store", "extra_submarkets_player_market", "esm_enable_player_market", true);

        // --- Ship Procurement Station: every non-automated hull, split by class ---
        register("esm_ship_procurement", "extra_submarkets_ship_frigate", "esm_enable_ship_frigate", true);
        register("esm_ship_procurement", "extra_submarkets_ship_destroyer", "esm_enable_ship_destroyer", true);
        register("esm_ship_procurement", "extra_submarkets_ship_cruiser", "esm_enable_ship_cruiser", true);
        register("esm_ship_procurement", "extra_submarkets_ship_capital", "esm_enable_ship_capital", true);

        // --- AI Ship Procurement Station: every automated hull, split by class ---
        register("esm_ai_ship_procurement", "extra_submarkets_aiship_frigate", "esm_enable_aiship_frigate", true);
        register("esm_ai_ship_procurement", "extra_submarkets_aiship_destroyer", "esm_enable_aiship_destroyer", true);
        register("esm_ai_ship_procurement", "extra_submarkets_aiship_cruiser", "esm_enable_aiship_cruiser", true);
        register("esm_ai_ship_procurement", "extra_submarkets_aiship_capital", "esm_enable_aiship_capital", true);

        // --- Weapon Procurement Stations: every weapon, split by size ---
        register("esm_weap_ballistic", "extra_submarkets_weap_ballistic_small", "esm_enable_weap_ballistic_small", true);
        register("esm_weap_ballistic", "extra_submarkets_weap_ballistic_medium", "esm_enable_weap_ballistic_medium", true);
        register("esm_weap_ballistic", "extra_submarkets_weap_ballistic_large", "esm_enable_weap_ballistic_large", true);

        register("esm_weap_energy", "extra_submarkets_weap_energy_small", "esm_enable_weap_energy_small", true);
        register("esm_weap_energy", "extra_submarkets_weap_energy_medium", "esm_enable_weap_energy_medium", true);
        register("esm_weap_energy", "extra_submarkets_weap_energy_large", "esm_enable_weap_energy_large", true);

        register("esm_weap_missile", "extra_submarkets_weap_missile_small", "esm_enable_weap_missile_small", true);
        register("esm_weap_missile", "extra_submarkets_weap_missile_medium", "esm_enable_weap_missile_medium", true);
        register("esm_weap_missile", "extra_submarkets_weap_missile_large", "esm_enable_weap_missile_large", true);

        // --- Fighter Procurement Station: every wing, split by role ---
        register("esm_fighter_store", "extra_submarkets_fighter_bomber", "esm_enable_fighter_bomber", true);
        register("esm_fighter_store", "extra_submarkets_fighter_fighter", "esm_enable_fighter_fighter", true);
        register("esm_fighter_store", "extra_submarkets_fighter_interceptor", "esm_enable_fighter_interceptor", true);
        register("esm_fighter_store", "extra_submarkets_fighter_assault", "esm_enable_fighter_assault", true);
        register("esm_fighter_store", "extra_submarkets_fighter_support", "esm_enable_fighter_support", true);

        // --- Chicken's Roost: Chicken's Tech shops ---
        register(Chicken.STATION_ID, "esm_chicken_specials", "esm_enable_chicken_specials", true);
        register(Chicken.STATION_ID, "esm_chicken_blueprints", "esm_enable_chicken_blueprints", true);
        register(Chicken.STATION_ID, "esm_chicken_modspecs", "esm_enable_chicken_modspecs", true);
        boolean exotica = ExoticaCompat.isAvailable();
        register(Chicken.STATION_ID, "esm_chicken_exotics", "esm_enable_chicken_exotics", true, exotica);
        register(Chicken.STATION_ID, "esm_chicken_upgrades", "esm_enable_chicken_upgrades", true, exotica);
    }

    @Override
    public void onNewGameAfterProcGen() {
        ExtraStoreSystem.generateGreatMallSystem();

        StarSystemAPI esmSystem = Global.getSector().getStarSystem(ESM_SYSTEM_NAME);
        if (esmSystem != null) {
            ExtraStoreSystem.validateLocation(Global.getSector(), esmSystem, ESM_SYSTEM_LOC);
        }

        marketsInitialSetup();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        try {
            Chicken.ensure();
        } catch (Throwable t) {
            Global.getLogger(ESMVanillaModPlugin.class).error("Could not set up Chicken's Roost", t);
        }
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
            for (String stationId : new String[] { Chicken.STATION_ID, "esm_ai_store" }) {
                SectorEntityToken entity = Global.getSector().getEntityById(stationId);
                if (entity == null || entity.getMarket() == null) continue;
                for (SubmarketAPI submarket : entity.getMarket().getSubmarketsCopy()) {
                    SubmarketPlugin plugin = submarket.getPlugin();
                    if (plugin instanceof ExtraStoreChickenBase) {
                        ((ExtraStoreChickenBase) plugin).clearForSave();
                    } else if (plugin instanceof ExtraStoreAICores) {
                        ((ExtraStoreAICores) plugin).clearForSave();
                    }
                }
            }
        } catch (Throwable t) {
            Global.getLogger(ESMVanillaModPlugin.class).error("Could not empty the shops before saving", t);
        }
    }

    /** LunaSettingsListener: re-apply submarket toggles when settings change in-game. */
    @Override
    public void settingsChanged(String modId) {
        if (!ESM_MOD_ID.equals(modId)) return;
        if (Global.getSector() == null) return;
        try {
            marketsInitialSetup();
        } catch (Throwable t) {
            // never let a settings callback crash the game
        }
    }
}
