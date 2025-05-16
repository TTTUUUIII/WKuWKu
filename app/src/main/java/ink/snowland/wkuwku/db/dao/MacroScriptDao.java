package ink.snowland.wkuwku.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ink.snowland.wkuwku.db.entity.MacroScript;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface MacroScriptDao {
    @Query("SELECT * FROM tb_macro")
    Observable<List<MacroScript>> getAll();
    @Query("SELECT * FROM tb_macro")
    Single<List<MacroScript>> getList();
    @Query("SELECT * FROM tb_macro WHERE id = :id")
    Single<MacroScript> findById(int id);
    @Delete
    Completable delete(MacroScript info);
    @Insert
    Completable add(MacroScript info);
    @Update
    Completable update(MacroScript script);
}
