package ink.snowland.wkuwku.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface PlugManifestExtDao {
    @Query("SELECT * FROM tb_plug_manifest_ext")
    Observable<List<PlugManifestExt>> getAll();
    @Query("SELECT * FROM tb_plug_manifest_ext")
    Single<List<PlugManifestExt>> getSingleAll();
    @Insert
    void insert(PlugManifestExt manifest);
    @Update
    void update(PlugManifestExt manifest);
    @Delete
    void delete(PlugManifestExt manifest);

    @Query("DELETE FROM tb_plug_manifest_ext WHERE package_name = :packageName")
    void deleteByPackageName(String packageName);
}
