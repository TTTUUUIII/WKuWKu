package ink.snowland.wkuwku.db.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "tb_game", indices = @Index(value = "filepath", unique = true))
public class Game implements Parcelable {
    public static final int STATE_VALID = 1;
    public static final int STATE_BROKEN = 2;
    public static final int STATE_DELETED = 3;

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "title")
    public String title;
    @ColumnInfo(name = "filepath")
    public String filepath;
    @ColumnInfo(name = "system")
    public String system;
    @ColumnInfo(name = "state")
    public int state;
    @ColumnInfo(name = "region", defaultValue = "unknown")
    public String region;
    @ColumnInfo(name = "added_time")
    public long addedTime;
    @ColumnInfo(name = "last_modified_time")
    public long lastModifiedTime;
    @ColumnInfo(name = "last_played_time")
    public long lastPlayedTime;
    @ColumnInfo(name = "remark", defaultValue = "")
    public String remark;

    public Game() {

    }

    protected Game(Parcel in) {
        id = in.readInt();
        title = in.readString();
        filepath = in.readString();
        system = in.readString();
        state = in.readInt();
        region = in.readString();
        addedTime = in.readLong();
        lastModifiedTime = in.readLong();
        lastPlayedTime = in.readLong();
        remark = in.readString();
    }

    public static final Creator<Game> CREATOR = new Creator<Game>() {
        @Override
        public Game createFromParcel(Parcel in) {
            return new Game(in);
        }

        @Override
        public Game[] newArray(int size) {
            return new Game[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return id == game.id && state == game.state && addedTime == game.addedTime && lastModifiedTime == game.lastModifiedTime && lastPlayedTime == game.lastPlayedTime && Objects.equals(title, game.title) && Objects.equals(filepath, game.filepath) && Objects.equals(system, game.system) && Objects.equals(region, game.region) && Objects.equals(remark, game.remark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, filepath, system, state, region, addedTime, lastModifiedTime, lastPlayedTime, remark);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(filepath);
        dest.writeString(system);
        dest.writeInt(state);
        dest.writeString(region);
        dest.writeLong(addedTime);
        dest.writeLong(lastModifiedTime);
        dest.writeLong(lastPlayedTime);
        dest.writeString(remark);
    }
}
