package ink.snowland.wkuwku.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "tb_macro", indices = {@Index(value = "title", unique = true)})
public class MacroScript {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "title")
    public String title;
    @ColumnInfo(name = "script")
    public String script;

    @Override
    public String toString() {
        return "MacroScript{" +
                "title='" + title + '\'' +
                ", script='" + script + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroScript that = (MacroScript) o;
        return id == that.id && Objects.equals(title, that.title) && Objects.equals(script, that.script);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, script);
    }
}
