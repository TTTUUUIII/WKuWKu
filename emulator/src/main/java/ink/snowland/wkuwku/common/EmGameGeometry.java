package ink.snowland.wkuwku.common;

public class EmGameGeometry {
    public final int baseWidth;    /* Nominal video width of game. */
    public final int baseHeight;   /* Nominal video height of game. */
    public final int maxWidth;     /* Maximum possible width of game. */
    public final int maxHeight;    /* Maximum possible height of game. */

    public final float aspectRatio;  /* Nominal aspect ratio of game. If
                                 * aspect_ratio is <= 0.0, an aspect ratio
                                 * of base_width / base_height is assumed.
                                 * A frontend could override this setting,
                                 * if desired. */

    public EmGameGeometry(int baseWidth, int baseHeight, int maxWidth, int maxHeight, float aspectRatio) {
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.aspectRatio = aspectRatio;
    }

    @Override
    public String toString() {
        return "EmGameGeometry{" +
                "baseWidth=" + baseWidth +
                ", baseHeight=" + baseHeight +
                ", maxWidth=" + maxWidth +
                ", maxHeight=" + maxHeight +
                ", aspectRatio=" + aspectRatio +
                '}';
    }
}
