package ink.snowland.wkuwku.db;

import static ink.snowland.wkuwku.db.Migrations.*;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ink.snowland.wkuwku.db.dao.GameDao;
import ink.snowland.wkuwku.db.dao.PlugManifestExtDao;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;

@Database(entities = {Game.class, PlugManifestExt.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract GameDao gameInfoDao();
    public abstract PlugManifestExtDao plugManifestExtDao();
    public static AppDatabase db;
    public static void initialize(Context applicationContext) {
        db = Room.databaseBuilder(applicationContext, AppDatabase.class, "db-wkuwku")
                .addMigrations(MIGRATION_1_2)
                .build();
    }
}