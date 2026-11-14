package theeverythingshop.compat;

import com.fs.starfarer.api.Global;
import niko_SA.augments.core.stationAugmentStore;

import java.util.HashSet;
import java.util.Set;

/**
 * The only class that touches Station Augments code. Only loaded through
 * {@link StationAugmentsCompat}, after the mod is found enabled.
 */
final class StationAugmentsBridge {

    private StationAugmentsBridge() {
    }

    static Set<String> playerKnownAugments() {
        Set<String> known = stationAugmentStore.getKnownAugmentsOfFac(Global.getSector().getPlayerFaction());
        return known == null ? new HashSet<String>() : new HashSet<String>(known);
    }
}
