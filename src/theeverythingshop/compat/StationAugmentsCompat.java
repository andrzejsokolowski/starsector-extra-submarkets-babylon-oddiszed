package theeverythingshop.compat;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Set;

/**
 * Safe access to Station Augments. Without the mod, or with a version whose code differs,
 * the answer is "nothing known", so its blueprints just stay on sale.
 */
public final class StationAugmentsCompat {

    public static final Logger log = Global.getLogger(StationAugmentsCompat.class);
    public static final String MOD_ID = "niko_stationAugments";

    private StationAugmentsCompat() {
    }

    /** Augment ids the player's faction already knows. */
    public static Set<String> playerKnownAugments() {
        try {
            if (!Global.getSettings().getModManager().isModEnabled(MOD_ID)) {
                return Collections.emptySet();
            }
            return StationAugmentsBridge.playerKnownAugments();
        } catch (LinkageError e) {
            log.warn("Station Augments version not compatible, known augments not checked", e);
        } catch (RuntimeException e) {
            log.warn("Could not read the known station augments", e);
        }
        return Collections.emptySet();
    }
}
