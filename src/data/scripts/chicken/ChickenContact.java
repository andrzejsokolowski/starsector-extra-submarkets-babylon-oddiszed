package data.scripts.chicken;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;

/**
 * Chicken's contact entry. Like Nexerelin's special market contacts it can't be
 * dismissed and doesn't count towards the contact limit.
 * Ported from Chicken's Tech Shop by Chickenboy (MIT licence).
 */
public class ChickenContact extends ContactIntel {

    public ChickenContact(PersonAPI person, MarketAPI market) {
        super(person, market);
        state = ContactState.SUSPENDED; // so she doesn't count towards the limit
    }

    // don't lose importance when relocating
    @Override
    public void relocateToMarket(MarketAPI other, boolean withIntelUpdate) {
        super.relocateToMarket(other, withIntelUpdate);
        person.setImportance(person.getImportance().next());
    }

    @Override
    public void doPeriodicCheck() {
        super.doPeriodicCheck();
        person.getMemoryWithoutUpdate().set(BaseMissionHub.CONTACT_SUSPENDED, false);
    }

    @Override
    public String getName() {
        return "Tech Contact: " + person.getNameString();
    }
}
