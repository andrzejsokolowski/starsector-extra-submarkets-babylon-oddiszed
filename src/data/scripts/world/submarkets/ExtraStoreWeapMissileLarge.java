package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.WeaponAPI;

public class ExtraStoreWeapMissileLarge extends ExtraStoreWeaponProcurementBase {
    @Override protected WeaponAPI.WeaponType getTypeFilter() { return WeaponAPI.WeaponType.MISSILE; }
    @Override protected WeaponAPI.WeaponSize getSizeFilter() { return WeaponAPI.WeaponSize.LARGE; }
    @Override protected String getDisplayName() { return "Large Missile Weapons"; }
}
