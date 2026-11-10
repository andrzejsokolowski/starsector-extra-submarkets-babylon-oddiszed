package data.scripts.chicken;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import data.scripts.world.ExtraStoreSystem;
import org.apache.log4j.Logger;

import static data.scripts.ESMVanillaModPlugin.ESM_SYSTEM_NAME;

/**
 * Chicken, the spacer who sells tech goodies, and her station Chicken's Roost in the
 * Gilgamesh Nebula. Ported from Chicken's Tech Shop by Chickenboy (MIT licence).
 *
 * Everything here is created on game load when missing, so saves that started
 * before Chicken moved in get her too.
 */
public class Chicken {

    public static final Logger log = Global.getLogger(Chicken.class);

    public static final String PERSON_ID = "esm_chicken";
    public static final String PERSON_TAG = "esm_chicken";
    public static final String STATION_ID = "esm_chicken_roost";
    public static final String STATION_NAME = "Chicken's Roost";
    public static final String STATION_TYPE = "station_side05";

    // Between the [REDACTED] (0 degrees) and Elite Vessels (40 degrees) stations
    private static final float STATION_ANGLE = 20f;
    private static final float STATION_RADIUS = 2000f;

    /** Create the station, Chicken and her contact entry if any of them is missing. */
    public static void ensure() {
        MarketAPI market = getOrCreateStation();
        if (market == null) {
            return;
        }
        PersonAPI chicken = Global.getSector().getImportantPeople().getPerson(PERSON_ID);
        if (chicken == null) {
            chicken = createChicken();
        }
        if (chicken.getMarket() != market) {
            if (chicken.getMarket() != null) {
                chicken.getMarket().removePerson(chicken);
                chicken.getMarket().getCommDirectory().removePerson(chicken);
            }
            chicken.setMarket(market);
        }
        if (!market.getPeopleCopy().contains(chicken)) {
            market.addPerson(chicken);
        }
        if (market.getCommDirectory().getEntryForPerson(chicken) == null) {
            market.getCommDirectory().addPerson(chicken);
        }
        if (BaseMissionHub.get(chicken) == null) {
            BaseMissionHub.set(chicken, new BaseMissionHub(chicken));
            chicken.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 1);
        }
        if (!Global.getSector().getIntelManager().hasIntelOfClass(ChickenContact.class)) {
            Global.getSector().getIntelManager().addIntel(new ChickenContact(chicken, market), true);
        }
    }

    public static MarketAPI getMarket() {
        SectorEntityToken station = Global.getSector().getEntityById(STATION_ID);
        return station == null ? null : station.getMarket();
    }

    public static PersonAPI getChicken() {
        return Global.getSector().getImportantPeople().getPerson(PERSON_ID);
    }

    private static MarketAPI getOrCreateStation() {
        MarketAPI market = getMarket();
        if (market != null) {
            return market;
        }
        StarSystemAPI system = Global.getSector().getStarSystem(ESM_SYSTEM_NAME);
        if (system == null || system.getCenter() == null) {
            log.error("No " + ESM_SYSTEM_NAME + " in this save, Chicken's Roost not created");
            return null;
        }
        ExtraStoreSystem.createMallStation(system, system.getCenter(), STATION_ID, STATION_TYPE, STATION_NAME,
                STATION_ANGLE, STATION_RADIUS);
        log.info("Created " + STATION_NAME + " in " + ESM_SYSTEM_NAME);
        return getMarket();
    }

    private static PersonAPI createChicken() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(PERSON_ID);
        person.setImportance(PersonImportance.HIGH);
        person.setVoice(Voices.SPACER);
        person.setFaction(Factions.INDEPENDENT);
        person.setGender(FullName.Gender.FEMALE);
        person.setRankId(Ranks.UNKNOWN);
        person.setPostId(Ranks.POST_CITIZEN);
        person.getName().setFirst("Chicken");
        person.getName().setLast("");
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", PERSON_ID));
        person.addTag(PERSON_TAG);
        Global.getSector().getImportantPeople().addPerson(person);
        return person;
    }
}
