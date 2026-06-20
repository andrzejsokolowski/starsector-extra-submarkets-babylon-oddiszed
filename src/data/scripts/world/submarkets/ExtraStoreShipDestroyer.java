package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.ShipAPI;

public class ExtraStoreShipDestroyer extends ExtraStoreShipProcurementBase {
    @Override protected ShipAPI.HullSize getSizeFilter() { return ShipAPI.HullSize.DESTROYER; }
    @Override protected boolean wantAutomated() { return false; }
    @Override protected String getDisplayName() { return "Destroyer Procurement"; }
}
