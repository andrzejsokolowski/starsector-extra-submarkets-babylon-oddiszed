package theeverythingshop.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base of the shops on the Treasury: 999 of everything (one of each in the learn-once
 * shops), a restock every 30 days, prices set by the player's standing with the
 * Independents, no selling back. Shelves are sorted by mod, then by name.
 */
public abstract class TreasuryShopBase extends BaseSubmarketPlugin {

    public static final Logger log = Global.getLogger(TreasuryShopBase.class);

    public static final int STOCK = 999;

    // One-copy shops: everything the player bought here, as "id|data". Never offered again.
    // Saved with the game.
    protected Set<String> boughtKeys = new HashSet<String>();
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
        sortByModThenName(cargo);
    }

    /** Shops that sell one of each learn-once item; what was bought never comes back. */
    protected boolean oneCopyEach() {
        return false;
    }

    protected boolean alreadyBought(String id, String data) {
        return boughtKeys != null && boughtKeys.contains(id + "|" + data);
    }

    /** One copy, unless the player bought this one here before. */
    protected void addOne(CargoAPI cargo, String id, String data) {
        if (alreadyBought(id, data)) {
            return;
        }
        addSpecial(cargo, id, data, 1);
    }

    protected void addSpecial(CargoAPI cargo, String id, String data, int amount) {
        try {
            cargo.addSpecial(new SpecialItemData(id, data), amount);
        } catch (Throwable t) {
            log.warn("Skipping " + id + " " + data, t);
            cargo.removeEmptyStacks();
        }
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        super.reportPlayerMarketTransaction(transaction);
        if (!oneCopyEach() || transaction == null || transaction.getBought() == null) {
            return;
        }
        if (boughtKeys == null) {
            boughtKeys = new HashSet<String>();
        }
        for (CargoStackAPI stack : transaction.getBought().getStacksCopy()) {
            SpecialItemData data = stack.getSpecialDataIfSpecial();
            if (data != null) {
                boughtKeys.add(data.getId() + "|" + data.getData());
            }
        }
    }

    /**
     * The game's own sort goes by each item's order number from its mod, which mixes mods
     * together. Sort by mod (the base game first), then by name.
     */
    protected static void sortByModThenName(CargoAPI cargo) {
        List<CargoStackAPI> stacks = cargo.getStacksCopy();
        final Map<CargoStackAPI, String> keys = new HashMap<CargoStackAPI, String>();
        for (CargoStackAPI stack : stacks) {
            keys.put(stack, sortKey(stack));
        }
        Collections.sort(stacks, new Comparator<CargoStackAPI>() {
            @Override
            public int compare(CargoStackAPI a, CargoStackAPI b) {
                return keys.get(a).compareTo(keys.get(b));
            }
        });
        cargo.clear();
        for (CargoStackAPI stack : stacks) {
            cargo.addItems(stack.getType(), stack.getData(), stack.getSize());
        }
    }

    private static String sortKey(CargoStackAPI stack) {
        String mod = "";
        String name = "";
        String data = "";
        try {
            if (stack.isSpecialStack()) {
                SpecialItemSpecAPI spec = stack.getSpecialItemSpecIfSpecial();
                ModSpecAPI source = spec == null ? null : spec.getSourceMod();
                mod = source == null ? "" : source.getName();
                SpecialItemData special = stack.getSpecialDataIfSpecial();
                data = special == null || special.getData() == null ? "" : special.getData();
            }
            name = stack.getDisplayName();
        } catch (Throwable t) {
            // keep what we have
        }
        return (mod == null ? "" : mod.toLowerCase()) + "\u0001" + (name == null ? "" : name.toLowerCase())
                + "\u0001" + data;
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
