package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreAiShipCapital extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.CAPITAL_SHIP; }
    @Override protected boolean wantAutomated() { return true; }
    @Override protected String getDisplayName() { return "AI Capital Procurement"; }
}
