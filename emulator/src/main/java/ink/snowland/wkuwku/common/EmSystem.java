package ink.snowland.wkuwku.common;

import java.util.Objects;

public class EmSystem {
    public final String name;
    public final String tag;
    public final String manufacturer;

    public EmSystem(String name, String tag, String manufacturer) {
        this.name = name;
        this.tag = tag;
        this.manufacturer = manufacturer;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EmSystem system = (EmSystem) o;
        return Objects.equals(tag, system.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tag);
    }
}
