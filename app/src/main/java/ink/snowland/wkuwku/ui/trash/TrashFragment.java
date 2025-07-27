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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentTrashBinding;
import ink.snowland.wkuwku.databinding.ItemTrashBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.util.TimeUtils;
import ink.snowland.wkuwku.widget.GameViewAdapter;

public class TrashFragment extends BaseFragment {
    private FragmentTrashBinding binding;
    private TrashViewModel mViewModel;
    private int mTrashStorageDays = 15;
    private final ViewAdapter mAdapter = new ViewAdapter();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TrashViewModel.class);
        mViewModel.getTrash()
                .observe(this, trash -> {
                    runAtDelayed(() -> {
                        mAdapter.submitList(trash);
                    }, savedInstanceState == null ? 300 : 0);
                });
        mTrashStorageDays = getResources().getInteger(R.integer.trash_storage_days);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentActivity.setActionbarTitle(R.string.trash);
    }
    private void showDeleteDialog(@NonNull Game game) {
        new MaterialAlertDialogBuilder(requireActivity())
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.emergency)
                .setMessage(getString(R.string.delete_confirm, game.title))
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    mViewModel.delete(game);
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show();
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
            itemBinding.deleteDate.setText(getString(R.string.fmt_delete_after_days, mTrashStorageDays - TimeUtils.elapsedDays(game.lastModifiedTime)));
            itemBinding.buttonDelete.setOnClickListener(v -> {
                showDeleteDialog(game);
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