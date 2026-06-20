package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreAiShipFrigate extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.FRIGATE; }
    @Override protected boolean wantAutomated() { return true; }
    @Override protected String getDisplayName() { return "AI Frigate Procurement"; }
}
