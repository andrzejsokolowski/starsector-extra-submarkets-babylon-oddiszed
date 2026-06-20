package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.WeaponAPI;

public class ExtraStoreWeapBallisticLarge extends ExtraStoreWeaponProcurementBase {
    @Override protected WeaponAPI.WeaponType getTypeFilter() { return WeaponAPI.WeaponType.BALLISTIC; }
    @Override protected WeaponAPI.WeaponSize getSizeFilter() { return WeaponAPI.WeaponSize.LARGE; }
    @Override protected String getDisplayName() { return "Large Ballistic Weapons"; }
}
