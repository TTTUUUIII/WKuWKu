package ink.snowland.wkuwku.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

import ink.snowland.wkuwku.plug.PlugManifest;

@Entity(tableName = "tb_plug_manifest_ext")
public class PlugManifestExt {

    @ColumnInfo(name = "enabled")
    public boolean enabled = true;

    @PrimaryKey
    @ColumnInfo(name = "package_name")
    @NonNull public String packageName;

    @Embedded(prefix = "origin_")
    public PlugManifest origin;

    public PlugManifestExt(PlugManifest manifest) {
        this.origin = manifest;
        this.packageName = manifest.packageName;
    }

    public PlugManifestExt() {
        packageName = "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlugManifestExt that = (PlugManifestExt) o;
        return Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(packageName);
    }
}
