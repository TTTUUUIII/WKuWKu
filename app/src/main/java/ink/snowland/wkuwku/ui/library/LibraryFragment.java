package ink.snowland.wkuwku.ui.library;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentLibraryBinding;
import ink.snowland.wkuwku.databinding.ItemGameBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.ui.home.HomeFragment;
import ink.snowland.wkuwku.ui.play.PlayFragment;
import ink.snowland.wkuwku.util.TimeUtils;
import ink.snowland.wkuwku.widget.GameDetailDialog;
import ink.snowland.wkuwku.widget.GameEditDialog;
import ink.snowland.wkuwku.widget.GameViewAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LibraryFragment extends BaseFragment implements View.OnClickListener {
    private FragmentLibraryBinding binding;
    private LibraryViewModel mViewModel;
    private final ViewAdapter mAdapter = new ViewAdapter();
    private Disposable mDisposable;
    private GameEditDialog mEditGameDialog;
    private GameDetailDialog mGameDetailDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        mDisposable = mViewModel.getGameInfos().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAdapter::submitList);
        mEditGameDialog = new GameEditDialog(mParentActivity);
        mGameDetailDialog = new GameDetailDialog(mParentActivity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DividerItemDecoration decoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
        binding.recyclerView.addItemDecoration(decoration);
        binding.fab.setOnClickListener(this);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        mParentActivity.setActionbarSubTitle(R.string.all_games);
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.fab) {
            showAddGameDialog();
        }
    }

    private void showMorePopupMenu(@NonNull Game game, @NonNull View view) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true);
        }
        popupMenu.getMenuInflater().inflate(R.menu.more_library_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete) {
                showDeleteDialog(game);
            } else if (itemId == R.id.action_edit) {
                showEditDialog(game);
            } else if (itemId == R.id.action_detail) {
                showDetailDialog(game);
            }
            return true;
        });
        popupMenu.show();
    }

    private void showDetailDialog(@NonNull Game game) {
        mGameDetailDialog.show(game);
    }

    private void showEditDialog(@NonNull Game base) {
        mEditGameDialog.show((game, uri) -> {
            game.lastModifiedTime = System.currentTimeMillis();
            mViewModel.updateGame(game);
        }, base);
    }

    private void showDeleteDialog(@NonNull Game game) {
        if (game.state == Game.STATE_BROKEN) {
            mViewModel.deleteGame(game);
        } else {
            new MaterialAlertDialogBuilder(requireActivity())
                    .setIcon(R.mipmap.ic_launcher_round)
                    .setTitle(R.string.emergency)
                    .setMessage(getString(R.string.delete_confirm, game.title))
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        game.state = Game.STATE_DELETED;
                        mViewModel.updateGame(game);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(false)
                    .show();
        }
    }
    private void showAddGameDialog() {
        mEditGameDialog.show((game, uri) -> {
            mViewModel.addGame(game, uri);
        });
    }

    private void launch(@NonNull Game game) {
        Fragment parent = requireParentFragment()
                .getParentFragment();
        assert parent instanceof HomeFragment;
        NavController navController = NavHostFragment.findNavController(parent);
        Bundle args = new Bundle();
        args.putParcelable(PlayFragment.ARG_GAME, game);
        navController.navigate(R.id.play_fragment, args);
    }

    private class ViewHolder extends GameViewAdapter.GameViewHolder {

        private final ItemGameBinding itemBinding;
        public ViewHolder(@NonNull ItemGameBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(@NonNull Game game) {
            itemBinding.setGame(game);
            if (game.lastPlayedTime == 0) {
                itemBinding.lastPlayedTime.setText(getString(R.string.last_played_t) + getString(R.string.never_played));
            } else {
                itemBinding.lastPlayedTime.setText(getString(R.string.last_played_t) + TimeUtils.toString("MM/dd HH:mm", game.lastPlayedTime));
            }
            itemBinding.buttonMore.setOnClickListener(v -> {
                showMorePopupMenu(game, v);
            });
            itemBinding.buttonLaunch.setOnClickListener(v -> {
                launch(game);
            });
        }
    }

    private class ViewAdapter extends GameViewAdapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemGameBinding itemBinding = ItemGameBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void submitList(@Nullable List<Game> list) {
            super.submitList(list);
            if (list != null) {
                mViewModel.setEmptyListIndicator(list.isEmpty());
            }
        }
    }
}