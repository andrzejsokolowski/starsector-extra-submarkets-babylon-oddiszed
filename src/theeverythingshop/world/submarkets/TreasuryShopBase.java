package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.apache.log4j.Logger;

/**
 * Base of the shops on the Treasury: 999 of everything, a restock every 30 days,
 * prices set by the player's standing with the Independents, no selling back.
 */
public abstract class TreasuryShopBase extends BaseSubmarketPlugin {

    public static final Logger log = Global.getLogger(TreasuryShopBase.class);

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
        RepLevel level = Global.getSector().getPlayerFaction().getRelationshipLevel(Factions.INDEPENDENT);
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
