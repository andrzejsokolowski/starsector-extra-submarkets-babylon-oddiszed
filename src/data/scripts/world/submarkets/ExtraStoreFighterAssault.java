package data.scripts.world.submarkets;

import com.fs.starfarer.api.loading.WingRole;

public class ExtraStoreFighterAssault extends ExtraStoreFighterProcurementBase {
    @Override protected WingRole getRoleFilter() { return WingRole.ASSAULT; }
    @Override protected String getDisplayName() { return "Assault Wings"; }
}
