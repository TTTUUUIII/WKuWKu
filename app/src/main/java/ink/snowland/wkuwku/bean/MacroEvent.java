package ink.snowland.wkuwku.bean;

import java.util.Arrays;

public class MacroEvent {
    public final int[] keys;
    public final int delayed;
    public final int duration;

    public MacroEvent(int[] keys, int delayed, int duration) {
        this.keys = keys;
        this.delayed = delayed;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "MacroEvent{" +
                "keys=" + Arrays.toString(keys) +
                ", delayed=" + delayed +
                ", duration=" + duration +
                '}';
    }
}
