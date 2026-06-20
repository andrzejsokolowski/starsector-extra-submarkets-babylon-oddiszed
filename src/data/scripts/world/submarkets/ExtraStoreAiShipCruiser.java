package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreAiShipCruiser extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.CRUISER; }
    @Override protected boolean wantAutomated() { return true; }
    @Override protected String getDisplayName() { return "AI Cruiser Procurement"; }
}
