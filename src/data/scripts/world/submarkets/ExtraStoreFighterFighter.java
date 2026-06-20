package data.scripts.world.submarkets;

import com.fs.starfarer.api.loading.WingRole;

public class ExtraStoreFighterFighter extends ExtraStoreFighterProcurementBase {
    @Override protected WingRole getRoleFilter() { return WingRole.FIGHTER; }
    @Override protected String getDisplayName() { return "Fighter Wings"; }
}
