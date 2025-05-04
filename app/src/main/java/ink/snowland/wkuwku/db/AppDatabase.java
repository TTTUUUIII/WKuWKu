package ink.snowland.wkuwku.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ink.snowland.wkuwku.db.dao.GameDao;
import ink.snowland.wkuwku.db.entity.Game;

@Database(entities = {Game.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract GameDao gameInfoDao();
    public static AppDatabase db;
    public static void initialize(Context applicationContext) {
        db = Room.databaseBuilder(applicationContext, AppDatabase.class, "db-wkuwku").build();
    }
}
