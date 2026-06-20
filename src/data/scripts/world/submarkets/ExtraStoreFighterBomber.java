package data.scripts.world.submarkets;

import com.fs.starfarer.api.loading.WingRole;

public class ExtraStoreFighterBomber extends ExtraStoreFighterProcurementBase {
    @Override protected WingRole getRoleFilter() { return WingRole.BOMBER; }
    @Override protected String getDisplayName() { return "Bomber Wings"; }
}
