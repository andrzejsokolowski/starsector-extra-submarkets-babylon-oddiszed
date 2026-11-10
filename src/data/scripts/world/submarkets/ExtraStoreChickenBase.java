package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import data.scripts.chicken.Chicken;
import org.apache.log4j.Logger;

/**
 * Base of Chicken's shops on Chicken's Roost: 999 of everything, a restock every
 * 30 days, prices set by how much Chicken likes you, no selling back.
 * The tariff by standing comes from Chicken's Tech Shop by Chickenboy (MIT licence).
 */
public abstract class ExtraStoreChickenBase extends BaseSubmarketPlugin {

    public static final Logger log = Global.getLogger(ExtraStoreChickenBase.class);

    public static final int STOCK = 999;
    protected static final float RESTOCK_INTERVAL_DAYS = 30f;

    protected abstract void addStock(CargoAPI cargo);

    protected abstract String getDisplayName();

    @Override
    public void updateCargoPrePlayerInteraction() {
        if (sinceLastCargoUpdate < RESTOCK_INTERVAL_DAYS && !restockEveryVisit() && !getCargo().isEmpty()) {
            return;
        }
        restock();
    }

    /** Throw everything away and fill the shelves again. */
    public void restock() {
        sinceLastCargoUpdate = 0f;
        CargoAPI cargo = getCargo();
        cargo.clear();
        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();
        try {
            addStock(cargo);
        } catch (Throwable t) {
            log.error("Could not stock " + getDisplayName(), t);
        }
        cargo.removeEmptyStacks();
        cargo.sort();
    }

    /**
     * Empty the shelves before a save: thousands of stacks would bloat the save, and a save
     * holding items of a mod that was removed later would not load. They fill again on the next visit.
     */
    public void clearForSave() {
        getCargo().clear();
        getCargo().getMothballedShips().clear();
        getCargo().removeEmptyStacks();
    }

    protected boolean restockEveryVisit() {
        return false;
    }

    @Override
    public float getTariff() {
        PersonAPI chicken = Chicken.getChicken();
        if (chicken == null) {
            return 0.5f;
        }
        RepLevel level = chicken.getRelToPlayer().getLevel();
        switch (level) {
            case FAVORABLE:
                return 0.4f;
            case WELCOMING:
                return 0.3f;
            case FRIENDLY:
                return 0.2f;
            case COOPERATIVE:
                return 0.1f;
            default:
                return 0.5f;
        }
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "No sales/returns";
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "No sales/returns";
    }

    @Override
    public String getName() {
        return getDisplayName();
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
