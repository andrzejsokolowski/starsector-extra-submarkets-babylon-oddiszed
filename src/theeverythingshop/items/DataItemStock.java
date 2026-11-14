package theeverythingshop.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Special items that only work with data, one entry per valid value. Every builder reads
 * only the public game API and the mod's own data files, does nothing when its mod is
 * missing, and checks each value before use, because a wrong value crashes most of these.
 */
public class DataItemStock {

    public static final Logger log = Global.getLogger(DataItemStock.class);

    // How many different Vulpoids are on the shelf: each copy is its own person
    private static final int VULPOIDS = 20;

    /** Exiled Sector schematics that work without data; sold in Blueprints, not Specials. */
    public static final Set<String> EXILED_SCHEMATICS = new HashSet<String>(Arrays.asList(
            "exiledSector_master_schematic", "exiledSector_logistics_schematic",
            "exiledSector_deintegration_schematic"));

    /**
     * Item ids handled here, or never sold plain because they need data (Knowledge
     * Constructs, forge templates and cryochambers are no longer sold at all).
     */
    public static final Set<String> DATA_ITEM_IDS = new HashSet<String>(Arrays.asList(
            "rat_alteration_install", "rat_consumeable_industry", "rat_artifact",
            "zea_recorder", "SA_augmentBlueprint", "sms_construct",
            "sms_superconstruct1", "sms_superconstruct2", "sms_superconstruct3", "special_vulpoid",
            "exiledSector_socketable", "exiledSector_star_chart", "exiledSector_refit_schematic",
            "cs_geneSplicer", "IndEvo_ForgeTemplate_1", "IndEvo_ForgeTemplate_2", "IndEvo_ForgeTemplate_3",
            "IndEvo_ForgeTemplate_4", "IndEvo_PetBox"));

    private final List<String[]> out = new ArrayList<String[]>();
    private final int qty;
    private final SettingsAPI settings = Global.getSettings();
    private final Random random = new Random();

    private DataItemStock(int qty) {
        this.qty = qty;
    }

    /** The data items sold in Specials, {id, data, amount}. */
    public static List<String[]> specials(int qty) {
        final DataItemStock stock = new DataItemStock(qty);
        stock.run("RAT hull alterations", new Runnable() { public void run() { stock.ratAlterations(); } });
        stock.run("RAT artifacts", new Runnable() { public void run() { stock.ratArtifacts(); } });
        stock.run("Elysian Data Recorders", new Runnable() { public void run() { stock.zeaRecorders(); } });
        stock.run("Knowledge Constructs", new Runnable() { public void run() { stock.superconstructs(); } });
        stock.run("Vulpoids", new Runnable() { public void run() { stock.vulpoids(); } });
        stock.run("Exiled Sector items", new Runnable() { public void run() { stock.exiledSector(); } });
        stock.run("Gene Splicers", new Runnable() { public void run() { stock.geneSplicers(); } });
        return stock.out;
    }

    /** Station Augment blueprints, one entry per augment, {id, data, amount}. Learn-once. */
    public static List<String[]> learnableBlueprints() {
        final DataItemStock stock = new DataItemStock(1);
        stock.run("Station Augment blueprints", new Runnable() { public void run() { stock.stationAugments(); } });
        return stock.out;
    }

    /** Blueprint items that are used up (RAT industry blueprints, Exiled Sector schematics). */
    public static List<String[]> consumableBlueprints(int qty) {
        final DataItemStock stock = new DataItemStock(qty);
        stock.run("RAT industry blueprints", new Runnable() { public void run() { stock.ratIndustries(); } });
        stock.run("Exiled Sector schematics", new Runnable() { public void run() { stock.exiledSchematics(); } });
        return stock.out;
    }

    private void run(String what, Runnable builder) {
        try {
            builder.run();
        } catch (Throwable t) {
            log.warn("Could not stock " + what, t);
        }
    }

    private boolean hasItem(String id) {
        try {
            return settings.getSpecialItemSpec(id) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private void add(String id, String data) {
        add(id, data, qty);
    }

    private void add(String id, String data, int amount) {
        out.add(new String[] { id, data, String.valueOf(amount) });
    }

    private JSONArray csv(String idColumn, String path, String modId) {
        try {
            return settings.getMergedSpreadsheetDataForMod(idColumn, path, modId);
        } catch (Throwable t) {
            log.warn("Could not read " + path, t);
            return new JSONArray();
        }
    }

    private boolean modOn(String modId) {
        return settings.getModManager().isModEnabled(modId);
    }

    // RAT: one installer per alteration the mod drops itself, plus two useful site rewards.
    // Core conversions are left out: they grant core skills without the core.
    private void ratAlterations() {
        if (!hasItem("rat_alteration_install")) {
            return;
        }
        Set<String> extra = new HashSet<String>(Arrays.asList("rat_abyssal_conversion", "rat_upscale_protocol"));
        for (HullModSpecAPI spec : settings.getAllHullModSpecs()) {
            if (!spec.hasTag("rat_alteration")) {
                continue;
            }
            if (spec.hasTag("rat alteration_no_drop") && !extra.contains(spec.getId())) {
                continue;
            }
            add("rat_alteration_install", spec.getId());
        }
    }

    // RAT: Forge-Assisted Industry blueprints, one per industry that takes them
    private void ratIndustries() {
        if (!hasItem("rat_consumeable_industry")) {
            return;
        }
        for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
            if (spec.hasTag("rat_consumeable_industry")) {
                add("rat_consumeable_industry", spec.getId());
            }
        }
    }

    // RAT: artifacts from rat_artifacts.csv
    private void ratArtifacts() {
        if (!hasItem("rat_artifact")) {
            return;
        }
        JSONArray rows = csv("id", "data/campaign/rat_artifacts.csv", "assortment_of_things");
        for (int i = 0; i < rows.length(); i++) {
            String id = rows.optJSONObject(i) == null ? "" : rows.optJSONObject(i).optString("id", "").trim();
            if (!id.isEmpty() && !id.startsWith("#")) {
                add("rat_artifact", id);
            }
        }
    }

    // Knights of Ludd: one recorder per faction, taken from the mod's own drop groups
    private void zeaRecorders() {
        if (!hasItem("zea_recorder")) {
            return;
        }
        String prefix = "item_zea_recorder:";
        JSONArray rows = csv("commodity", "data/campaign/procgen/drop_groups.csv", "knights_of_ludd");
        Set<String> done = new HashSet<String>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            String c = row == null ? "" : row.optString("commodity", "");
            if (c.startsWith(prefix) && done.add(c)) {
                add("zea_recorder", c.substring(prefix.length()));
            }
        }
    }

    // Station Augments: one blueprint per augment whose required mods are enabled
    private void stationAugments() {
        if (!hasItem("SA_augmentBlueprint")) {
            return;
        }
        JSONArray rows = csv("id", "data/campaign/SA_station_augments.csv", "niko_stationAugments");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String id = row.optString("id", "").trim();
            if (id.isEmpty() || id.startsWith("#")) {
                continue;
            }
            boolean ok = true;
            for (String m : row.optString("req_mod_ids", "").split(",")) {
                if (!m.trim().isEmpty() && !modOn(m.trim())) {
                    ok = false;
                }
            }
            if (ok) {
                add("SA_augmentBlueprint", id);
            }
        }
    }


    // Ship Mastery: the blank Knowledge Construct (the one SMS makes from shared mastery;
    // right-click it to pick any ship), and the three superconstructs, which only work
    // with their "active" data. The per-hull constructs are not sold.
    private void superconstructs() {
        if (hasItem("sms_construct")) {
            add("sms_construct", "sms_PlayerCreated_");
        }
        for (String id : new String[] { "sms_superconstruct1", "sms_superconstruct2", "sms_superconstruct3" }) {
            if (hasItem(id)) {
                add(id, "sms_BlankConstructActive");
            }
        }
    }

    // Vulpoids: each cryosleeper is a person, so sell a few different ones, one of each
    private void vulpoids() {
        if (!hasItem("special_vulpoid")) {
            return;
        }
        for (int i = 0; i < VULPOIDS; i++) {
            CargoStackAPI stack = Global.getFactory().createCargoStack(CargoItemType.SPECIAL,
                    new SpecialItemData("special_vulpoid", null), null);
            stack.getPlugin(); // makes the person and writes it into the stack data
            SpecialItemData data = stack.getSpecialDataIfSpecial();
            if (data != null && data.getData() != null) {
                add("special_vulpoid", data.getData(), 1);
            }
        }
    }

    // Exiled Sector: hull items and subroutines (one roll each) and star charts of every tier.
    // Wormhole anchors stay out, like every other anchor.
    private void exiledSector() {
        if (hasItem("exiledSector_socketable")) {
            for (String file : new String[] { "hull_items.csv", "subroutines.csv" }) {
                JSONArray rows = csv("id", "data/config/exiledSector/" + file, "exiledSector");
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.optJSONObject(i);
                    String id = row == null ? "" : row.optString("id", "").trim();
                    if (!id.isEmpty() && !id.startsWith("#")) {
                        add("exiledSector_socketable", id + "|" + random.nextLong());
                    }
                }
            }
        }
        if (hasItem("exiledSector_star_chart")) {
            for (int tier = 1; tier <= 15; tier++) {
                add("exiledSector_star_chart",
                        "{\"d\":\"star_chart\",\"s\":" + random.nextLong() + ",\"t\":" + tier + "}");
            }
        }
    }

    // Exiled Sector schematics: the four refit schematic tiers and the three single schematics
    private void exiledSchematics() {
        if (hasItem("exiledSector_refit_schematic")) {
            for (String tier : new String[] { "SCRAPPED", "STANDARD", "MILITARY", "DOMAIN" }) {
                add("exiledSector_refit_schematic", tier);
            }
        }
        for (String id : EXILED_SCHEMATICS) {
            if (hasItem(id)) {
                add(id, null);
            }
        }
    }

    // Crab Shack: an empty Gene Splicer, and filled ones for every vanilla combat officer skill
    // and Crab Shack's good splicer skills, normal and elite. Harmful "bad" skills stay out.
    private void geneSplicers() {
        if (!hasItem("cs_geneSplicer")) {
            return;
        }
        add("cs_geneSplicer", null);
        for (String id : settings.getSkillIds()) {
            SkillSpecAPI skill = settings.getSkillSpec(id);
            if (skill == null || skill.hasTag("cs_splicerBad")) {
                continue;
            }
            boolean vanillaCombat = skill.isCombatOfficerSkill() && skill.getSourceMod() == null;
            if (!vanillaCombat && !skill.hasTag("cs_splicerGood")) {
                continue;
            }
            add("cs_geneSplicer", id + "$1");
            add("cs_geneSplicer", id + "$2");
        }
    }


}
