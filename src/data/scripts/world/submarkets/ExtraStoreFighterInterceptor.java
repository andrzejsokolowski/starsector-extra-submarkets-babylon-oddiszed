package data.scripts.world.submarkets;

import com.fs.starfarer.api.loading.WingRole;

public class ExtraStoreFighterInterceptor extends ExtraStoreFighterProcurementBase {
    @Override protected WingRole getRoleFilter() { return WingRole.INTERCEPTOR; }
    @Override protected String getDisplayName() { return "Interceptor Wings"; }
}
