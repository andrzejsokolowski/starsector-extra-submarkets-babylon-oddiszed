package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import data.scripts.ESMSettings;

import java.util.List;

/**
 * Shared logic for the "procurement" submarkets: a complete catalogue of every
 * hull in the loaded game of a given size class, optionally filtered to automated
 * ("AI piloted") hulls, sold at a flat premium over base hull value.
 *
 * The work is split across one submarket per hull size (and AI vs non-AI) so that
 * each catalogue stays small enough to build without a noticeable hitch.
 */
public abstract class ExtraStoreShipProcurementBase extends BaseSubmarketPlugin {

    /** In-game days between full restocks of the catalogue. */
    protected static final float RESTOCK_INTERVAL_DAYS = 30f;

    /** Which hull size this submarket lists. */
    protected abstract ShipAPI.HullSize getSizeFilter();

    /** True for the AI station (automated hulls), false for the normal station. */
    protected abstract boolean wantAutomated();

    /** Label shown on the submarket tab. */
    protected abstract String getDisplayName();

    /** Tariff from the shared LunaLib price multiplier (X => tariff X-1 => price X x base). */
    protected float getConfiguredTariff() {
        return ESMSettings.tariff();
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        // Self-heal: drop any stock that no longer passes the filters (e.g. a bad hull
        // left in saved stock by an older build). Cheap, and runs even when the periodic
        // full rebuild below is skipped, so existing saves are fixed on the next visit.
        pruneStaleStock();

        if (sinceLastCargoUpdate < RESTOCK_INTERVAL_DAYS) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();
        cargo.clear();
        cargo.getMothballedShips().clear();
        cargo.removeEmptyStacks();

        List<ShipHullSpecAPI> all = Global.getSettings().getAllShipHullSpecs();
        for (ShipHullSpecAPI spec : all) {
            if (spec == null) continue;
            if (spec.getHullSize() != getSizeFilter()) continue;
            if (isAutomated(spec) != wantAutomated()) continue;
            if (!isShipAllowed(spec)) continue;

            try {
                ShipVariantAPI variant = buildVariant(spec);
                if (variant == null) continue; // no safe variant (e.g. modular ship with no usable design)
                FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
                member.getRepairTracker().setMothballed(true);
                member.getRepairTracker().setCR(0.5f);
                cargo.getMothballedShips().addFleetMember(member);
            } catch (Throwable t) {
                // Skip any hull that can't be instantiated.
            }
        }

        cargo.sort();
    }

    /**
     * Pick the variant to sell for a hull.
     * <p>Single-hull ships use an empty variant, so the price reflects the pure hull base
     * value. Multi-component ships (module slots) can't be sold as an empty variant -- the
     * unfilled modules crash the market on render -- so we use the hull's designed (codex)
     * variant, which has its modules populated. If no complete design exists, the hull is
     * skipped rather than risk a crash.
     */
    private ShipVariantAPI buildVariant(ShipHullSpecAPI spec) {
        ShipVariantAPI empty = Global.getSettings().createEmptyVariant(spec.getHullId() + "_esmproc", spec);
        if (!looksModular(spec, empty)) {
            normalize(empty);
            return empty; // not a multi-component ship
        }
        String codexId = spec.getCodexVariantId();
        if (codexId != null && !codexId.isEmpty()) {
            ShipVariantAPI codex = Global.getSettings().getVariant(codexId);
            if (codex != null && !codex.getModuleSlots().isEmpty() && isModuleComplete(codex)) {
                ShipVariantAPI clone = codex.clone(); // clone so we never mutate the shared cached variant
                normalize(clone);
                return clone;
            }
        }
        return null; // modular ship with no usable designed variant -> skip (a bare hull crashes on render)
    }

    /**
     * Whether a hull is multi-component. The empty variant's getModuleSlots() is unreliable here
     * (a blank variant has no modules assigned, so it reads empty even for a modular hull -- which
     * previously let modular ships slip through as bare hulls and crash the fleet-member view).
     * So we check the HULL hint plus the hull's default station-module map as a backstop.
     */
    private boolean looksModular(ShipHullSpecAPI spec, ShipVariantAPI variant) {
        if (spec != null && spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.SHIP_WITH_MODULES)) {
            return true;
        }
        if (variant != null) {
            if (variant.getModuleSlots() != null && !variant.getModuleSlots().isEmpty()) return true;
            if (variant.getStationModules() != null && !variant.getStationModules().isEmpty()) return true;
        }
        return false;
    }

    /**
     * Make a freshly-built variant safe to render in the fleet-member view. An empty variant
     * of a hull with built-in weapons or wings can have weapon groups that don't line up with
     * its built-ins, which makes the engine throw ArrayIndexOutOfBounds (in
     * FleetMemberStatus.applyToShip) while drawing the ship. Regenerating the groups and wings
     * fixes the structure without adding any purchased loadout, so the price stays at hull base.
     */
    private void normalize(ShipVariantAPI v) {
        if (v == null) return;
        try { v.autoGenerateWeaponGroups(); } catch (Throwable t) { /* best effort */ }
        try { v.refreshBuiltInWings(); } catch (Throwable t) { /* best effort */ }
    }

    /** True if the variant is not modular, or is modular with every module slot filled. */
    private boolean isModuleComplete(ShipVariantAPI v) {
        if (v == null) return false;
        List<String> slots = v.getModuleSlots();
        if (slots == null || slots.isEmpty()) return true;
        for (String slot : slots) {
            if (v.getModuleVariant(slot) == null) return false;
        }
        return true;
    }

    /** Remove stocked ships that no longer match the filters, or are broken module ships. */
    private void pruneStaleStock() {
        CargoAPI cargo = getCargoNullOk();
        if (cargo == null) return;
        for (FleetMemberAPI m : cargo.getMothballedShips().getMembersListCopy()) {
            ShipHullSpecAPI spec = m.getHullSpec();
            boolean ok = spec != null
                    && spec.getHullSize() == getSizeFilter()
                    && isAutomated(spec) == wantAutomated()
                    && isShipAllowed(spec);
            // A modular hull is only safe if its saved variant actually has its modules filled.
            // This catches the bare-hull modular ships older builds left in saved stock.
            ShipVariantAPI v = m.getVariant();
            if (ok && looksModular(spec, v)) {
                ok = v != null && !v.getModuleSlots().isEmpty() && isModuleComplete(v);
            }
            if (!ok) {
                cargo.getMothballedShips().removeFleetMember(m);
            } else {
                normalize(m.getVariant()); // repair weapon-group/wing structure of older saved stock
            }
        }
    }

    protected boolean isAutomated(ShipHullSpecAPI spec) {
        List<String> builtIns = spec.getBuiltInMods();
        return builtIns != null && builtIns.contains(HullMods.AUTOMATED);
    }

    /**
     * Only list ships that are actually meant to be obtainable as a normal vessel. Stations,
     * loose module parts, hidden/test hulls, unboardable non-automated hulls, restricted or
     * unrecoverable specials and (D) duplicates are skipped. Multi-component ships are allowed
     * here and handled safely in {@link #buildVariant} (sold as their designed variant).
     */
    protected boolean isShipAllowed(ShipHullSpecAPI spec) {
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.MODULE)) return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNDER_PARENT)) return false;
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.HIDE_IN_CODEX)) return false;
        // Unboardable hulls aren't ownable -- except automated ships flagged recoverable,
        // which are exactly what the AI store is meant to carry (Remnant, Threat, etc.).
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE)
                && !spec.hasTag(Tags.AUTOMATED_RECOVERABLE)) return false;
        if (spec.isDefaultDHull()) return false;            // skip auto-generated (D) duplicates
        if (spec.hasTag(Tags.RESTRICTED)) return false;     // boss/special, not for sale
        if (spec.hasTag(Tags.UNRECOVERABLE)) return false;  // not an ownable hull
        if (spec.getBaseValue() <= 0f) return false;        // no price => not a real product
        return true;
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public float getTariff() {
        return getConfiguredTariff();
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
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
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "Procurement only - no sales";
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
