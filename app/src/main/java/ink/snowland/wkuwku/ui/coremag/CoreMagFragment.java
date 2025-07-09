package ink.snowland.wkuwku.ui.coremag;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ink.snowland.wkuwku.AppConfig;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.CoreManifest;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.OnProgressListener;
import ink.snowland.wkuwku.databinding.FragmentCoreMagBinding;
import ink.snowland.wkuwku.databinding.ItemCoreinfoBinding;
import ink.snowland.wkuwku.databinding.ItemCoremagBinding;
import ink.snowland.wkuwku.databinding.ItemTrashBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.util.FileManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CoreMagFragment extends BaseFragment {
    private FragmentCoreMagBinding binding;
    private CoreMagViewModel mViewModel;
    private final CoreAdapter mAdapter = new CoreAdapter();
    private final List<CoreManifest.SystemElement> mSystems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(CoreMagViewModel.class);
        parentActivity.setActionbarTitle(R.string.core_manage);
        mViewModel.getManifest().observe(this, manifest -> {
            mSystems.clear();
            manifest.manufacturers.forEach(it -> {
                mSystems.addAll(it.systems);
            });
            mAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreMagBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
        return binding.getRoot();
    }

    private class CoreHolder extends RecyclerView.ViewHolder {

        private final ItemCoremagBinding itemBinding;
        private boolean expand = false;

        public CoreHolder(@NonNull ItemCoremagBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(CoreManifest.SystemElement data) {
            itemBinding.system.setText(data.name);
            data.cores.forEach(it -> {
                ItemCoreinfoBinding infoBinding = ItemCoreinfoBinding.inflate(getLayoutInflater(), itemBinding.coresContainer, true);
                infoBinding.alias.setText(it.alias);
                Disposable disposable1 = AppDatabase.db.gameCoreDao().findByAlias(it.alias).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess(core -> {
                            infoBinding.action.setText(R.string.delete);
                        })
                        .doOnError(error -> {
                            infoBinding.action.setText(R.string.download);
                        })
                        .subscribe(core -> {
                        }, error -> {
                        });
                infoBinding.action.setOnClickListener(v -> {
                    v.setEnabled(false);
                    Disposable disposable = Completable.create(emitter -> {
                                for (CoreManifest.FileElement file : it.files) {
                                    String filename = new File(file.path).getName();
                                    FileManager.copy(new URL(AppConfig.WEB_URL + file.path), FileManager.getPrivateFile(FileManager.CORE_DIRECTORY, filename), new OnProgressListener() {
                                        @Override
                                        public void update(long progress, long max) {
                                            run(() -> {
                                                infoBinding.action.setText(getString(R.string.fmt_downloading, (float) progress / max * 100));
                                            });
                                        }
                                    });
                                    emitter.onComplete();
                                }
                            })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doFinally(() -> {
                                infoBinding.action.setEnabled(true);
                            })
                            .subscribe(() -> {
                                infoBinding.action.setText(R.string.delete);
                            }, error -> {
                                infoBinding.action.setText(R.string.download);
                            });
                });
            });
            itemBinding.foldButton.setOnClickListener(v -> {
                toggleVisibleState();
            });
        }

        private void toggleVisibleState() {
            expand = !expand;
            itemBinding.coresContainer.setVisibility(expand ? View.VISIBLE : View.GONE);
            if (itemBinding.foldButton instanceof MaterialButton) {
                ((MaterialButton) itemBinding.foldButton).setIconResource(expand ? R.drawable.ic_arrow_drop_down_circle : R.drawable.ic_expand_circle_right);
            }
        }
    }

    private class CoreAdapter extends RecyclerView.Adapter<CoreHolder> {

        @NonNull
        @Override
        public CoreHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCoremagBinding itemBinding = ItemCoremagBinding.inflate(getLayoutInflater(), parent, false);
            return new CoreHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull CoreHolder holder, int position) {
            holder.bind(mSystems.get(position));
        }

        @Override
        public int getItemCount() {
            return mSystems.size();
        }
    }
}