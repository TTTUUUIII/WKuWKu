package ink.snowland.wkuwku.db.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface GameDao {
    @Query("SELECT * FROM tb_game WHERE state != 3 ORDER BY LOWER(title) ASC")
    Observable<List<Game>> getAll();
    @Query("SELECT * FROM tb_game WHERE filepath IS :path AND state == :state")
    Single<Game> findByPathAndState(@NonNull String path, int state);
    @Query("SELECT * FROM tb_game WHERE state != 3 AND last_played_time != 0 ORDER BY last_played_time DESC LIMIT 20")
    Observable<List<Game>> getHistory();
    @Delete
    Completable delete(Game info);
    @Update
    Completable update(Game info);
    @Insert
    Completable insert(Game info);
}
