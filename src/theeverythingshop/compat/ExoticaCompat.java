package theeverythingshop.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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

    /**
     * Exotics that normally install from an AI core (alpha, beta, gamma, Tahlan's archdaemon
     * core), not from a chip: their chips never drop, so they aren't sold. The cores are in the
     * AI Cores Store.
     */
    private static final Set<String> INSTALLED_FROM_CORES = new HashSet<String>(Arrays.asList(
            "AlphaSubcore", "SubsumedAlphaCore", "SubsumedBetaCore", "SubsumedGammaCore", "DaemonCore"));

    public static List<String[]> listExotics() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        try {
            List<String[]> out = new ArrayList<String[]>();
            for (String[] entry : ExoticaBridge.listExotics()) {
                String key = entry[1].split(",")[0];
                if (!INSTALLED_FROM_CORES.contains(key)) {
                    out.add(entry);
                }
            }
            return out;
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
}
