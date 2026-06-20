package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreShipCapital extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.CAPITAL_SHIP; }
    @Override protected boolean wantAutomated() { return false; }
    @Override protected String getDisplayName() { return "Capital Procurement"; }
}
