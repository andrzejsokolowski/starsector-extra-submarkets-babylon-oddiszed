package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreAiShipDestroyer extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.DESTROYER; }
    @Override protected boolean wantAutomated() { return true; }
    @Override protected String getDisplayName() { return "AI Destroyer Procurement"; }
}
