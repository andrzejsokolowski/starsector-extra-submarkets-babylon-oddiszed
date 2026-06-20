package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import data.scripts.ESMSettings;

/**
 * Catalogue of every weapon of one mount type (ballistic/energy/missile) and one size
 * (small/medium/large), sold at the configured price multiple. One submarket per (type,size).
 */
public abstract class ExtraStoreWeaponProcurementBase extends BaseSubmarketPlugin {

    protected static final float RESTOCK_INTERVAL_DAYS = 30f;

    protected abstract WeaponAPI.WeaponType getTypeFilter();
    protected abstract WeaponAPI.WeaponSize getSizeFilter();
    protected abstract String getDisplayName();

    @Override
    public void updateCargoPrePlayerInteraction() {
        if (sinceLastCargoUpdate < RESTOCK_INTERVAL_DAYS) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();
        cargo.clear();
        cargo.removeEmptyStacks();

        int qty = stackSize();
        for (String id : Global.getSector().getAllWeaponIds()) {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            if (spec == null || !isWeaponAllowed(spec)) continue;
            try {
                cargo.addWeapons(id, qty);
            } catch (Throwable t) {
                // skip any weapon that can't be stacked
            }
        }
        cargo.sort();
    }

    private int stackSize() {
        switch (getSizeFilter()) {
            case SMALL:  return 10;
            case MEDIUM: return 5;
            default:     return 3;
        }
    }

    protected boolean isWeaponAllowed(WeaponSpecAPI spec) {
        if (spec.getType() != getTypeFilter()) return false;
        if (spec.getSize() != getSizeFilter()) return false;
        if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) return false; // built-in ship systems
        if (spec.hasTag(Tags.NO_DROP)) return false;
        return true;
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
