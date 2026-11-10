package data.scripts.chicken;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Which special items Chicken sells. Started from the checks in Chicken's Tech Shop by
 * Chickenboy (MIT licence), extended with what each mod in a large mod list needs.
 *
 * A special item with no data is sold when it is a colony item, has a right-click use, or is
 * on the include lists below, and is not excluded. Items that only work with data (hull
 * alteration installers, Exiled Sector hull items, forge templates...) are built by
 * {@link DataItemStock} instead. data/config/esm_chicken/excluded_items.csv lists one-offs,
 * quest items and items that crash; other mods can add rows to it.
 */
public class SpecialItemRules {

    public static final Logger log = Global.getLogger(SpecialItemRules.class);

    public static final String MODSPEC_ID = "modspec";
    public static final String EXCLUSIONS_CSV = "data/config/esm_chicken/excluded_items.csv";

    // Templates that only work with a target; blueprints and modspecs have their own shops
    private static final Set<String> TEMPLATE_IDS = new HashSet<String>(Arrays.asList(
            MODSPEC_ID, "ship_bp", "weapon_bp", "fighter_bp", "industry_bp"));

    // Plain items used through a dialog, the refit screen, a building or another mod's own
    // screen, so the colony and right-click checks can't see them
    private static final Set<String> INCLUDE_IDS = new HashSet<String>(Arrays.asList(
            // vanilla: Shrouded items, Threat hull mod parts
            "shrouded_substrate", "shrouded_thunderhead", "shrouded_mantle", "shrouded_lens",
            "fragment_fabricator", "threat_processing_unit",
            // DIY Planets
            "atmo_mineralizer", "atmo_sublimator", "solar_reflector", "tectonic_attenuator", "weather_core",
            "climate_sculptor", "gravity_oscillator", "rad_remover",
            // Secrets of the Frontier: part for the Daydream Synthesizer hull mod
            "sotf_daydreamanalyzer",
            // FSF: campaign cruise missiles
            "aEP_cruise_missile", "aEP_cruise_missile2",
            // Domain Phase Lab: currency for Ross's weapon trade
            "dpl_data_archive",
            // AoTD: Tenebrium and Shrouded Substrate materials
            "aotd_shrouded_substrate", "aotd_tenebrium",
            // Crab Shack: Forming Fluid (Gene Splicers are built with data)
            "cs_formingFluid",
            // Emergent Threats: refit-screen consumables
            "vice_biochip_ai_commander", "vice_biochip_nemesis", "vice_signal_masker_device",
            "vice_temporal_desync_item", "ix_biochip_sotf", "ix_nano_replicator",
            // Industrial Evolution: empty and degraded forge templates
            "IndEvo_EmptForgeTemplate", "IndEvo_DegForgeTemplate"));

    // Plain items found by tag (IndEvo VPCs and consumables, UAF keycards, Captain's Quarters)
    private static final Set<String> INCLUDE_TAGS = new HashSet<String>(Arrays.asList(
            "IndEvo_VPC_common", "IndEvo_VPC_uncommon", "IndEvo_VPC_rare", "IndEvo_consumable",
            "uaf_keycard", "cq_furnishing"));

    // Plain items found by id prefix (Exiled Sector materials, ciphers, engrams, schematics)
    private static final String[] INCLUDE_PREFIXES = { "exiledSector_" };

    // Tags that keep an item out: quest items, Exotica (its own shops), renamed duplicates
    private static final Set<String> EXCLUDE_TAGS = new HashSet<String>(Arrays.asList(
            "mission_item", "no_sell", "exotica", "single_bp", "exiledSector_renamed_id"));

    private static Set<String> excludedIds = null;
    private static Set<String> excludedTags = null;

    public static boolean isBlueprintPackage(SpecialItemSpecAPI spec) {
        for (String tag : spec.getTags()) {
            if (tag.endsWith("package_bp")) {
                return true;
            }
        }
        return false;
    }

    /** Hull mods a modspec can teach (RAT alterations only install through their own item). */
    public static boolean isLearnableHullMod(HullModSpecAPI spec) {
        return !spec.isHidden() && !spec.isHiddenEverywhere() && !spec.isAlwaysUnlocked()
                && !spec.hasTag(Tags.HULLMOD_NO_DROP) && !spec.hasTag(Tags.HULLMOD_DMOD)
                && !spec.hasTag("rat_alteration") && !isExcluded(spec.getId());
    }

    /** On the exclusion list by id. */
    public static boolean isExcluded(String id) {
        loadExclusions();
        return excludedIds.contains(id);
    }

    /** A special item sold with no data in Chicken's Tech - Specials. */
    public static boolean isPlainShopItem(SpecialItemSpecAPI spec) {
        String id = spec.getId();
        if (id == null || TEMPLATE_IDS.contains(id) || DataItemStock.DATA_ITEM_IDS.contains(id)
                || isBlueprintPackage(spec) || isExcluded(id)) {
            return false;
        }
        loadExclusions();
        for (String tag : spec.getTags()) {
            if (EXCLUDE_TAGS.contains(tag) || excludedTags.contains(tag)) {
                return false;
            }
        }
        if (spec.getName() == null || spec.getName().trim().isEmpty()) {
            return false;
        }

        boolean wanted = INCLUDE_IDS.contains(id) || spec.hasTag("colony_item")
                || ItemEffectsRepo.ITEM_EFFECTS.containsKey(id);
        for (String tag : spec.getTags()) {
            if (INCLUDE_TAGS.contains(tag)) {
                wanted = true;
            }
        }
        for (String prefix : INCLUDE_PREFIXES) {
            if (id.startsWith(prefix)) {
                wanted = true;
            }
        }

        // The item must work without data; a right-click use also makes it wanted
        try {
            CargoStackAPI stack = Global.getFactory().createCargoStack(CargoItemType.SPECIAL,
                    new SpecialItemData(id, null), null);
            SpecialItemPlugin plugin = stack.getPlugin();
            if (plugin == null) {
                plugin = spec.getNewPluginInstance(stack);
            }
            if (plugin == null || plugin.getName() == null) {
                return false;
            }
            if (plugin.hasRightClickAction()) {
                wanted = true;
            }
        } catch (Throwable t) {
            log.info("Skipping special item " + id + ", it doesn't work without data");
            return false;
        }
        return wanted;
    }

    private static void loadExclusions() {
        if (excludedIds != null) {
            return;
        }
        excludedIds = new HashSet<String>();
        excludedTags = new HashSet<String>();
        try {
            JSONArray rows = Global.getSettings().getMergedSpreadsheetDataForMod("id", EXCLUSIONS_CSV,
                    "MM_ESM_Vanilla");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = row.optString("id", "").trim();
                if (id.isEmpty() || id.startsWith("#")) {
                    continue;
                }
                if ("tag".equals(row.optString("type", "item").trim())) {
                    excludedTags.add(id);
                } else {
                    excludedIds.add(id);
                }
            }
        } catch (Throwable t) {
            log.error("Could not read " + EXCLUSIONS_CSV, t);
        }
    }
}
