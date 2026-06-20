package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreShipFrigate extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.FRIGATE; }
    @Override protected boolean wantAutomated() { return false; }
    @Override protected String getDisplayName() { return "Frigate Procurement"; }
}
