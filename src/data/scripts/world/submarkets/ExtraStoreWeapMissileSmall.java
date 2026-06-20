package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.WeaponAPI;

public class ExtraStoreWeapMissileSmall extends ExtraStoreWeaponProcurementBase {
    @Override protected WeaponAPI.WeaponType getTypeFilter() { return WeaponAPI.WeaponType.MISSILE; }
    @Override protected WeaponAPI.WeaponSize getSizeFilter() { return WeaponAPI.WeaponSize.SMALL; }
    @Override protected String getDisplayName() { return "Small Missile Weapons"; }
}
