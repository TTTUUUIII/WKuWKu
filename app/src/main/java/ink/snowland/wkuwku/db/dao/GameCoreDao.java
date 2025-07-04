package ink.snowland.wkuwku.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import ink.snowland.wkuwku.db.entity.GameCore;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface GameCoreDao {
    @Query("SELECT * FROM tb_core")
    Single<List<GameCore>> getList();
    @Query("SELECT * FROM tb_core WHERE alias = :alias")
    Single<GameCore> findByAlias(String alias);
    @Insert
    void add(GameCore core);
}
