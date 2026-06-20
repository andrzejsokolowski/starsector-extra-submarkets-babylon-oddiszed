package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreShipCruiser extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.CRUISER; }
    @Override protected boolean wantAutomated() { return false; }
    @Override protected String getDisplayName() { return "Cruiser Procurement"; }
}
