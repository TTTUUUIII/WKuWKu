package ink.snowland.wkuwku.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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
}
