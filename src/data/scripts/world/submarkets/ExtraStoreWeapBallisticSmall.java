package data.scripts.world.submarkets;

import com.fs.starfarer.api.combat.WeaponAPI;

public class ExtraStoreWeapBallisticSmall extends ExtraStoreWeaponProcurementBase {
    @Override protected WeaponAPI.WeaponType getTypeFilter() { return WeaponAPI.WeaponType.BALLISTIC; }
    @Override protected WeaponAPI.WeaponSize getSizeFilter() { return WeaponAPI.WeaponSize.SMALL; }
    @Override protected String getDisplayName() { return "Small Ballistic Weapons"; }
}
