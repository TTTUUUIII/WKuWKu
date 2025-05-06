package ink.snowland.wkuwku.common;

public class EmSystemAvInfo {
    public final EmGameGeometry geometry;
    public final EmSystemTiming timing;

    public EmSystemAvInfo(EmGameGeometry geometry, EmSystemTiming timing) {
        this.geometry = geometry;
        this.timing = timing;
    }

    @Override
    public String toString() {
        return "EmSystemAvInfo{" +
                "geometry=" + geometry +
                ", timing=" + timing +
                '}';
    }
}
