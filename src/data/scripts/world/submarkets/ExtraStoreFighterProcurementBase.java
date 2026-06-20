package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WingRole;
import data.scripts.ESMSettings;

/**
 * Catalogue of every fighter wing of one role (bomber/fighter/interceptor/assault/support),
 * sold as fighter chips at the configured price multiple. One submarket per role.
 */
public abstract class ExtraStoreFighterProcurementBase extends BaseSubmarketPlugin {

    protected static final float RESTOCK_INTERVAL_DAYS = 30f;
    protected static final int STACK = 5;

    protected abstract WingRole getRoleFilter();
    protected abstract String getDisplayName();

    @Override
    public void updateCargoPrePlayerInteraction() {
        if (sinceLastCargoUpdate < RESTOCK_INTERVAL_DAYS) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();
        cargo.clear();
        cargo.removeEmptyStacks();

        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
            if (spec == null || spec.getRole() != getRoleFilter()) continue;
            if (spec.hasTag(Tags.NO_DROP)) continue;
            try {
                cargo.addItems(CargoAPI.CargoItemType.FIGHTER_CHIP, spec.getId(), STACK);
            } catch (Throwable t) {
                // skip any wing that can't be stacked
            }
        }
        cargo.sort();
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public float getTariff() {
        return ESMSettings.tariff();
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
        return "Procurement only - no sales";
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
