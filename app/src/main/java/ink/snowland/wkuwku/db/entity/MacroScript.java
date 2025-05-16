package ink.snowland.wkuwku.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tb_macro")
public class MacroScript {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "title")
    public String title;
    @ColumnInfo(name = "script")
    public String script;

    public boolean checkValid() {
        if (script.isEmpty()) return false;
        return true;
    }
    @Override
    public String toString() {
        return "MacroScript{" +
                "title='" + title + '\'' +
                ", script='" + script + '\'' +
                '}';
    }
}
