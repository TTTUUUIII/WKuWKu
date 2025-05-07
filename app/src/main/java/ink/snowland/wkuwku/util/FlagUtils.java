package ink.snowland.wkuwku.util;

import androidx.annotation.NonNull;

public final class FlagUtils {
    private FlagUtils() {}
    private static final int BASE = 0x1F1E6;
    private static final StringBuilder mBuilder = new StringBuilder();

    public static String flag(@NonNull String code) {
        if (code.length() != 2) {
            return code;
        }
        char[] codes = code.toCharArray();
        mBuilder.delete(0, mBuilder.length());
        return mBuilder.append(Character.toChars(codes[0] - 'A' + BASE))
                .append(Character.toChars(codes[1] - 'A' + BASE))
                .toString();
    }
}
