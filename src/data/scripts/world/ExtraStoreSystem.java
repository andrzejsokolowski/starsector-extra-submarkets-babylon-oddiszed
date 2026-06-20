package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

import static data.scripts.ESMVanillaModPlugin.ESM_SYSTEM_LOC;
import static data.scripts.ESMVanillaModPlugin.ESM_SYSTEM_NAME;

public class ExtraStoreSystem {
    public static final float SYSTEM_ROTATION_PERIOD = 2000f;

    public static void generateGreatMallSystem() {
        String name = ESM_SYSTEM_NAME;
        StarSystemAPI system = Global.getSector().createStarSystem(name);
        system.setType(StarSystemGenerator.StarSystemType.NEBULA);
        system.setAge(StarAge.OLD);
        system.getLocation().set(ESM_SYSTEM_LOC);
        ProcgenUsedNames.notifyUsed(name);

        // temporarily create a "star" (needed for some things)
        PlanetAPI star = system.initStar("esm_mall_center", "nebula_center_old", 0, 0);
        star.setSkipForJumpPointAutoGen(true);
        star.addTag(Tags.AMBIENT_LS);
        StarSystemGenerator.addSystemwideNebula(system, StarAge.OLD);

        // jump point
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("esm_mall_jump", "Gilgamesh Entrance");
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        jumpPoint.setFixedLocation(0, 0);
        system.addEntity(jumpPoint);

        // remove the star
        system.removeEntity(star);
        StarCoronaTerrainPlugin coronaPlugin = Misc.getCoronaFor(star);
        if (coronaPlugin != null) {
            system.removeEntity(coronaPlugin.getEntity());
        }
        system.setStar(null);
        SectorEntityToken center = system.initNonStarCenter();
        center.addTag(Tags.AMBIENT_LS);
        //jumpPoint.setCircularOrbit(center, 270, 2000, SYSTEM_ROTATION_PERIOD);

        system.autogenerateHyperspaceJumpPoints(true, false);

        system.setStar(star);

        SectorEntityToken aurora = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldTerrainPlugin.MagneticFieldParams(200f, 1200, center, 1150f, 1400f,
                        new Color(150, 30, 50, 30),
                        1f, // Вероятность авроры (всегда включена)

                        new Color(180, 110, 50, 130),
                        new Color(220, 150, 60, 150),
                        new Color(255, 200, 80, 190),
                        new Color(255, 240, 120, 240),
                        new Color(255, 180, 60, 255),

                        new Color(200, 90, 40),
                        new Color(120, 90, 30)
                ));
        aurora.setCircularOrbit(center, 0, 0, 100);

        system.addRingBand(center, "misc", "rings_dust0", 256f, 0, new Color(255, 200, 100, 150), 256f, 1200f, SYSTEM_ROTATION_PERIOD);

        SectorEntityToken visualRing = system.addTerrain(Terrain.RING,
                new BaseRingTerrain.RingParams(256, 1200, null, "System Belt"));
        visualRing.setCircularOrbit(center, 0, 0, SYSTEM_ROTATION_PERIOD);

        // Stations sit on a clean orbit ring clear of the dust band / magnetic field
        // (those are clustered around radius 1200), so the sprites are actually visible.
        // Nine stations, evenly spaced around the ring (40 degrees apart).
        float stationRadius = 2000f;
        createMallStation(system, center, "esm_ai_store", "station_hightech3", "[REDACTED]", 0f, stationRadius);
        createMallStation(system, center, "esm_elite_store", "station_midline3", "Elite Vessels", 40f, stationRadius);
        createMallStation(system, center, "esm_general_store", "station_lowtech3", "General Trade", 80f, stationRadius);
        createMallStation(system, center, "esm_ai_ship_procurement", "station_hightech3", "AI Ship Procurement Station", 120f, stationRadius);
        createMallStation(system, center, "esm_ship_procurement", "station_lowtech3", "Ship Procurement Station", 160f, stationRadius);
        createMallStation(system, center, "esm_weap_ballistic", "station_lowtech3", "Ballistic Arsenal", 200f, stationRadius);
        createMallStation(system, center, "esm_weap_energy", "station_hightech3", "Energy Arsenal", 240f, stationRadius);
        createMallStation(system, center, "esm_weap_missile", "station_midline3", "Missile Arsenal", 280f, stationRadius);
        createMallStation(system, center, "esm_fighter_store", "station_midline3", "Fighter Hangar", 320f, stationRadius);

        // Comm Relay
        SectorEntityToken relay = system.addCustomEntity("esm_mall_relay", null, Entities.COMM_RELAY_MAKESHIFT, Factions.INDEPENDENT);
        relay.setCircularOrbitPointingDown(center, 150f, 3500f, SYSTEM_ROTATION_PERIOD);

        // Gate
        SectorEntityToken gate = system.addCustomEntity("esm_mall_gate", "Ancient Gate", "inactive_gate", null);
        gate.setCircularOrbit(center, 270f, 3500f, SYSTEM_ROTATION_PERIOD);

        // Abandoned Station
        SectorEntityToken storage = system.addCustomEntity("esm_mall_storage", "Gate Maintenance Station", "station_mining00", Factions.NEUTRAL);
        storage.setCircularOrbitPointingDown(gate, 0, 300, 40f);
        storage.getMemoryWithoutUpdate().set("$abandonedStation", true);
        Misc.setAbandonedStationMarket("esm_mall_storage_market", storage);

        //Add secret Loot
        spawnOuterCaches(system);
        spawnOmegaCaches(system);

        // do it last to ensure correct naming of the system
        system.setBaseName(name);
    }

    public static void spawnOuterCaches(StarSystemAPI system) {
        Random random = new Random(getStartingSeed());

        WeightedRandomPicker<String> cacheTypes = new WeightedRandomPicker<String>(random);
        cacheTypes.add(Entities.WEAPONS_CACHE_REMNANT, 4f);
        cacheTypes.add(Entities.WEAPONS_CACHE_SMALL_REMNANT, 10f);
        cacheTypes.add(Entities.SUPPLY_CACHE, 4f);
        cacheTypes.add(Entities.SUPPLY_CACHE_SMALL, 10f);
        cacheTypes.add(Entities.EQUIPMENT_CACHE, 4f);
        cacheTypes.add(Entities.EQUIPMENT_CACHE_SMALL, 10f);

        int numCaches = 14;
        float minRadius = 10000f;
        float maxRadius = 15000f;
        float angleStep = 360f / numCaches;

        for (int i = 0; i < numCaches; i++) {
            String type = cacheTypes.pick();

            float angle = (i * angleStep) + (random.nextFloat() * angleStep);
            float radius = minRadius + (random.nextFloat() * (maxRadius - minRadius));
            Vector2f pos = Misc.getUnitVectorAtDegreeAngle(angle);
            pos.scale(radius);

            SectorEntityToken cache = BaseThemeGenerator.addSalvageEntity(system, type, Factions.NEUTRAL);

            cache.getLocation().set(pos.x, pos.y);

            cache.setDiscoverable(true);

            float orbitDays = radius / 5f;
            cache.setCircularOrbit(system.getCenter(), angle, radius, orbitDays);
        }
    }

    public static void spawnOmegaCaches(StarSystemAPI system) {
        Random random = new Random(getStartingSeed());

        int numCaches = 3;
        float minRadius = 17500f;
        float maxRadius = 20000f;
        float angleStep = 360f / numCaches;

        for (int i = 0; i < numCaches; i++) {
            String type = Entities.ALPHA_SITE_WEAPONS_CACHE;

            float angle = (i * angleStep) + (random.nextFloat() * angleStep);
            float radius = minRadius + (random.nextFloat() * (maxRadius - minRadius));
            Vector2f pos = Misc.getUnitVectorAtDegreeAngle(angle);
            pos.scale(radius);

            SectorEntityToken cache = BaseThemeGenerator.addSalvageEntity(system, type, Factions.NEUTRAL);

            cache.getLocation().set(pos.x, pos.y);

            cache.setDiscoverable(true);

            float orbitDays = radius / 5f;
            cache.setCircularOrbit(system.getCenter(), angle, radius, orbitDays);
        }
    }

    private static void createMallStation(StarSystemAPI system, SectorEntityToken center, String id, String type, String name, float angle, float radius) {
        SectorEntityToken station = system.addCustomEntity(id, name, type, Factions.NEUTRAL);

        // Direct angle-based orbit
        station.setCircularOrbitWithSpin(center, angle, radius, SYSTEM_ROTATION_PERIOD, 10f, 20f);

        // Market Setup
        MarketAPI market = Global.getFactory().createMarket(id + "_market", name, 3);
        market.setSize(3);
        market.setHidden(false);

        market.setFactionId(Factions.INDEPENDENT);
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

        market.setEconGroup(null);
        market.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true); // Prevents the mall from dying
        market.getMemoryWithoutUpdate().set("$nex_uninvadable", true);

        market.addCondition(Conditions.POPULATION_3);
        market.addIndustry(Industries.POPULATION);
        market.addIndustry(Industries.SPACEPORT);
        market.addIndustry(Industries.PATROLHQ);

        market.addSubmarket(Submarkets.SUBMARKET_OPEN);
        market.addSubmarket(Submarkets.SUBMARKET_BLACK);
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE);

        market.setInvalidMissionTarget(true);
        market.addTag(Tags.MARKET_NO_OFFICER_SPAWN);

        market.getTariff().modifyFlat("default_tariff", market.getFaction().getTariffFraction());

        market.setPrimaryEntity(station);
        station.setMarket(market);
        station.setFaction(Factions.INDEPENDENT);

        market.reapplyIndustries();
        Global.getSector().getEconomy().addMarket(market, true);
    }

    // Stations
    //OLD Corridor Like layout
        /*
        createMallStation(system, center, "esm_mall_store_1", "station_mining00", "Gunman Inc", -600f, 1200f); //Guns and Pirate Market
        createMallStation(system, center, "esm_mall_store_2", "station_side04", "REDACTED psst", -750f, 0f); //AI Ships
        createMallStation(system, center, "esm_mall_store_3", "station_mining00", "Vacant Store 3", -600f, -1200f); //Faction Roulette
        createMallStation(system, center, "esm_mall_store_4", "station_mining00", "Vacant Store 4", 600f, 1200f);
        createMallStation(system, center, "esm_mall_store_5", "station_side04", "Excelsior", 750f, 0f); //XIV and Lions Guard
        createMallStation(system, center, "esm_mall_store_6", "station_mining00", "Vacant Store 6", 600f, -1200f);
        */

    /*private static void createMallStation(StarSystemAPI system, SectorEntityToken center, String id, String type, String name, float x, float y) {
        // Physical Entity
        SectorEntityToken station = system.addCustomEntity(id, name, type, Factions.NEUTRAL);
        float angle = (float) Math.toDegrees(Math.atan2(y, x));
        float radius = (float) Math.sqrt(x * x + y * y);
        station.setCircularOrbitWithSpin(center, angle, radius, SYSTEM_ROTATION_PERIOD, 10f, 20f);

        // Market Setup
        MarketAPI market = Global.getFactory().createMarket(id + "_market", name, 3);
        market.setSize(3);
        market.setHidden(false);

        market.setFactionId(Factions.INDEPENDENT);
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

        market.setEconGroup(market.getId());
        market.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true); // Prevents the mall from dying
        market.getMemoryWithoutUpdate().set("$nex_uninvadable", true);

        market.addCondition(Conditions.POPULATION_3);
        market.addIndustry(Industries.POPULATION);
        market.addIndustry(Industries.SPACEPORT);
        market.addIndustry(Industries.MILITARYBASE);

        //market.addSubmarket(Submarkets.SUBMARKET_OPEN);
        //market.addSubmarket(Submarkets.SUBMARKET_BLACK);
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE);

        market.setInvalidMissionTarget(true);
        market.addTag(Tags.MARKET_NO_OFFICER_SPAWN);

        // Tariff setup like Pirate Base
        market.getTariff().modifyFlat("default_tariff", market.getFaction().getTariffFraction());

        // 3. Linkage
        market.setPrimaryEntity(station);
        station.setMarket(market);
        station.setFaction(Factions.INDEPENDENT);

        // Finalize
        market.reapplyIndustries();
        Global.getSector().getEconomy().addMarket(market, true);
    }*/

    public static void validateLocation(SectorAPI sector, StarSystemAPI system, Vector2f startLoc) {
        Random random = new Random(getStartingSeed());
        float minDist = 150;

        try_again:
        for (int i = 0; i < 100; i++) {
            Vector2f loc = new Vector2f(startLoc);
            if (i > 0) {
                Vector2f mod = new Vector2f(0, 150f * i);
                mod = VectorUtils.rotate(mod, random.nextFloat() * 360f);
                loc.x += mod.x;
                loc.y += mod.y;
            }

            for (StarSystemAPI sys : sector.getStarSystems()) {
                if (sys == system) continue;

                float otherRadius = sys.getMaxRadiusInHyperspace();
                if (MathUtils.getDistance(sys.getLocation(), loc) < minDist + otherRadius) {
                    continue try_again;
                }
            }
            system.getLocation().set(loc.x, loc.y);
            break;
        }

        clearDeepHyper(system.getHyperspaceAnchor(), 400f);
    }

    private static long getStartingSeed() {
        String seedStr = Global.getSector().getSeedString().replaceAll("[^0-9]", "");
        return Long.parseLong(seedStr);
    }

    private static void clearDeepHyper(SectorEntityToken entity, float radius) {

        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;
        clearDeepHyper(entity, radius, minRadius, editor);
    }

    private static void clearDeepHyper(SectorEntityToken entity, float radius, float minRadius, NebulaEditor editor) {
        editor.clearArc(entity.getLocation().x, entity.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(entity.getLocation().x, entity.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }
}