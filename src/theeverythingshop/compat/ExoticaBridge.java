package theeverythingshop.compat;

import exoticatechnologies.modifications.exotics.Exotic;
import exoticatechnologies.modifications.exotics.ExoticsHandler;
import exoticatechnologies.modifications.exotics.types.ExoticType;
import exoticatechnologies.modifications.upgrades.Upgrade;
import exoticatechnologies.modifications.upgrades.UpgradesHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The only class that touches Exotica Technologies code. It is only loaded through
 * {@link ExoticaCompat}, after the mod is found enabled, and returns plain strings so no
 * Exotica type leaks into the rest of the mod.
 * Written against the RkShaRkz continuation 2.1.1 (exotic data "key,TYPE", upgrade data "key,level").
 */
final class ExoticaBridge {

    private ExoticaBridge() {
    }

    /** Every exotic in every form it can take: {"et_exotic", "key,TYPE"}. */
    static List<String[]> listExotics() {
        List<String[]> out = new ArrayList<String[]>();
        List<ExoticType> types = new ArrayList<ExoticType>(ExoticType.Companion.getTypes().values());
        for (Exotic exotic : new ArrayList<Exotic>(ExoticsHandler.EXOTICS.values())) {
            if (exotic == null) {
                continue;
            }
            for (ExoticType type : types) {
                String typeKey = type.getNameKey();
                if (!"NORMAL".equals(typeKey) && !exotic.canUseExoticType(type)) {
                    continue;
                }
                out.add(new String[] { "et_exotic", exotic.getKey() + "," + typeKey });
            }
        }
        return out;
    }

    /** Every upgrade at every level it has: {"et_upgrade", "key,level"}. */
    static List<String[]> listUpgrades() {
        List<String[]> out = new ArrayList<String[]>();
        for (Upgrade upgrade : new ArrayList<Upgrade>(UpgradesHandler.UPGRADES_LIST)) {
            if (upgrade == null) {
                continue;
            }
            for (int level = 1; level <= upgrade.getMaxLevel(); level++) {
                out.add(new String[] { "et_upgrade", upgrade.getKey() + "," + level });
            }
        }
        return out;
    }
}
