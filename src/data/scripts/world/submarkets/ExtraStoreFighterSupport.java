package data.scripts.world.submarkets;

import com.fs.starfarer.api.loading.WingRole;

public class ExtraStoreFighterSupport extends ExtraStoreFighterProcurementBase {
    @Override protected WingRole getRoleFilter() { return WingRole.SUPPORT; }
    @Override protected String getDisplayName() { return "Support Wings"; }
}
