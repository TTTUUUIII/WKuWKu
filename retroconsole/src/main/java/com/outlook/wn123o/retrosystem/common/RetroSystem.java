package com.outlook.wn123o.retrosystem.common;

import java.util.Objects;

public class RetroSystem {
    public final String name;
    public final String tag;
    public final String manufacturer;

    public RetroSystem(String name, String tag, String manufacturer) {
        this.name = name;
        this.tag = tag;
        this.manufacturer = manufacturer;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RetroSystem system = (RetroSystem) o;
        return Objects.equals(tag, system.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tag);
    }
}
