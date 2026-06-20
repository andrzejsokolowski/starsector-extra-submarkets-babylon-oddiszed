package data.scripts;

import lunalib.lunaSettings.LunaSettings;

/**
 * Central access to this mod's LunaLib settings. All reads are null-safe and fall back to a
 * sensible default if LunaLib hasn't loaded the value yet, so nothing here can crash the game.
 */
public class ESMSettings {

    public static final String MOD_ID = "MM_ESM_Vanilla";
    public static final String PRICE_MULT_FIELD = "esm_price_mult";
    public static final float DEFAULT_PRICE_MULT = 4f;

    /** Whether a submarket toggle is on. {@code def} is used if LunaLib has no value. */
    public static boolean enabled(String fieldId, boolean def) {
        try {
            Boolean b = LunaSettings.getBoolean(MOD_ID, fieldId);
            return b != null ? b : def;
        } catch (Throwable t) {
            return def;
        }
    }

    /** Price multiplier applied across the procurement stores (ships, weapons, fighters). */
    public static float priceMult() {
        try {
            Double d = LunaSettings.getDouble(MOD_ID, PRICE_MULT_FIELD);
            if (d != null && d > 0d) return d.floatValue();
        } catch (Throwable t) {
            // fall through to default
        }
        return DEFAULT_PRICE_MULT;
    }

    /** Tariff that produces the configured multiple: price = base * (1 + tariff) = base * X. */
    public static float tariff() {
        return Math.max(0f, priceMult() - 1f);
    }
}
