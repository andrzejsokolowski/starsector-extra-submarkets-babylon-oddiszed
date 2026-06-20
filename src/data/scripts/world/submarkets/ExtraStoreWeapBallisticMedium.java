package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.WeaponAPI;

public class ExtraStoreWeapBallisticMedium extends ExtraStoreWeaponProcurementBase {
    @Override protected WeaponAPI.WeaponType getTypeFilter() { return WeaponAPI.WeaponType.BALLISTIC; }
    @Override protected WeaponAPI.WeaponSize getSizeFilter() { return WeaponAPI.WeaponSize.MEDIUM; }
    @Override protected String getDisplayName() { return "Medium Ballistic Weapons"; }
}
