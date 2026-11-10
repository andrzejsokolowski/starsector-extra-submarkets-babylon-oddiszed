package data.scripts.chicken.missions;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.SpySatDeployment;
import com.fs.starfarer.api.util.Misc;

import java.util.Random;

/**
 * Chicken's spy satellite job: the vanilla mission plus her insult for whoever
 * wrecked the old satellite. Ported from Chicken's Tech Shop by Chickenboy (MIT licence).
 */
public class ESMChickenSpySat extends SpySatDeployment {

    private String makeFunnyInsult() {
        String[] adjective = { "silly", "crazy", "huge", "absolute", "foolish", "giant" };
        String[] noun = { "banana", "donkey", "muppet", "fool", "donut" };
        Random random = new Random();
        return adjective[random.nextInt(adjective.length)] + " " + noun[random.nextInt(noun.length)];
    }

    @Override
    protected void updateInteractionDataImpl() {
        set("$ssat_barEvent", isBarEvent());
        set("$ssat_underworld", getPerson().hasTag(Tags.CONTACT_UNDERWORLD));
        set("$ssat_manOrWoman", getPerson().getManOrWoman());
        set("$ssat_reward", Misc.getWithDGS(getCreditsReward()));

        set("$ssat_personName", getPerson().getNameString());
        set("$ssat_systemName", market.getStarSystem().getNameWithLowercaseTypeShort());
        set("$ssat_marketName", market.getName());
        set("$ssat_marketOnOrAt", market.getOnOrAt());
        set("$ssat_dist", getDistanceLY(market));

        set("$esm_ck_ssat_insult", makeFunnyInsult());
    }
}
