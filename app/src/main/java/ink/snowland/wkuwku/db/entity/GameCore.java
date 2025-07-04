package ink.snowland.wkuwku.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tb_core")
public class GameCore {
    @PrimaryKey
    @NonNull
    public String alias = "";

    @ColumnInfo(name = "path")
    public String path;
}
