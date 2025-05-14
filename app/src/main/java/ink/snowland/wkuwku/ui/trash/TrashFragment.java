package ink.snowland.wkuwku.ui.trash;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentTrashBinding;
import ink.snowland.wkuwku.databinding.ItemTrashBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.widget.GameViewAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TrashFragment extends BaseFragment {
    private FragmentTrashBinding binding;
    private TrashViewModel mViewModel;
    private Disposable mDisposable;
    private final ViewAdapter mAdapter = new ViewAdapter();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TrashViewModel.class);
        mDisposable = mViewModel.getTrash().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAdapter::submitList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTrashBinding.inflate(inflater, container, false);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
        binding.recyclerView.setAdapter(mAdapter);
        DividerItemDecoration decoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
        binding.recyclerView.addItemDecoration(decoration);
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
    }

    private final class ViewHolder extends GameViewAdapter.GameViewHolder {

        private final ItemTrashBinding itemBinding;
        public ViewHolder(@NonNull ItemTrashBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(@NonNull Game game) {
            itemBinding.setGame(game);
            itemBinding.buttonDelete.setOnClickListener(v -> {
                mViewModel.delete(game);
            });
            itemBinding.buttonRestore.setOnClickListener(v -> {
                mViewModel.restore(game);
            });
        }
    }

    private class ViewAdapter extends GameViewAdapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemTrashBinding itemBinding = ItemTrashBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void submitList(@Nullable List<Game> list) {
            super.submitList(list);
            mViewModel.setEmptyListIndicator(list == null || list.isEmpty());
        }
    }
}