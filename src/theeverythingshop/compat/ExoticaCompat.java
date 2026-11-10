package theeverythingshop.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Safe access to Exotica Technologies. Without the mod, or with a fork whose code differs
 * from what {@link ExoticaBridge} expects, everything here answers "nothing" instead of crashing.
 */
public final class ExoticaCompat {

    public static final Logger log = Global.getLogger(ExoticaCompat.class);
    public static final String MOD_ID = "exoticatechnologies";

    private ExoticaCompat() {
    }

    /**
     * Exotica is enabled and has the items this mod stocks. Whether its code matches is
     * found out when listing: a different fork throws a LinkageError there, caught below.
     */
    public static boolean isAvailable() {
        try {
            if (!Global.getSettings().getModManager().isModEnabled(MOD_ID)) {
                return false;
            }
            SpecialItemSpecAPI exotic = Global.getSettings().getSpecialItemSpec("et_exotic");
            SpecialItemSpecAPI upgrade = Global.getSettings().getSpecialItemSpec("et_upgrade");
            if (exotic == null || upgrade == null) {
                return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static List<String[]> listExotics() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        try {
            return ExoticaBridge.listExotics();
        } catch (LinkageError e) {
            log.warn("Exotica Technologies version not compatible, no exotics stocked", e);
        } catch (RuntimeException e) {
            log.warn("Could not list Exotica exotics", e);
        }
        return Collections.emptyList();
    }

    public static List<String[]> listUpgrades() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        try {
            return ExoticaBridge.listUpgrades();
        } catch (LinkageError e) {
            log.warn("Exotica Technologies version not compatible, no upgrades stocked", e);
        } catch (RuntimeException e) {
            log.warn("Could not list Exotica upgrades", e);
        }
        return Collections.emptyList();
    }

    public static List<String> listInstallItems() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        try {
            return ExoticaBridge.listInstallItems();
        } catch (LinkageError e) {
            log.warn("Exotica Technologies version not compatible, no exotic install items stocked", e);
        } catch (RuntimeException e) {
            log.warn("Could not list Exotica install items", e);
        }
        return Collections.emptyList();
    }
}
