package ink.snowland.wkuwku.ui.game;

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

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentGameBinding;
import ink.snowland.wkuwku.databinding.ItemGameBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.ui.home.HomeFragment;
import ink.snowland.wkuwku.ui.launch.LaunchFragment;
import ink.snowland.wkuwku.widget.GameDetailDialog;
import ink.snowland.wkuwku.widget.GameEditDialog;
import ink.snowland.wkuwku.widget.GameViewAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GamesFragment extends BaseFragment implements View.OnClickListener {
    private FragmentGameBinding binding;
    private GamesViewModel mViewModel;
    private final ViewAdapter mAdapter = new ViewAdapter();
    private Disposable mDisposable;
    private GameDetailDialog mGameDetailDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(GamesViewModel.class);
        mDisposable = mViewModel.getGameInfos().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAdapter::submitList);
        mGameDetailDialog = new GameDetailDialog(parentActivity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DividerItemDecoration decoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
        binding.recyclerView.addItemDecoration(decoration);
        binding.fab.setOnClickListener(this);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        binding.pendingIndicator.setDataModel(mViewModel);
        binding.pendingIndicator.setLifecycleOwner(this);
        parentActivity.setActionbarTitle(R.string.all_games);
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
        popupMenu.getMenuInflater().inflate(R.menu.more_games_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete) {
                game.state = Game.STATE_DELETED;
                mViewModel.update(game);
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
        new GameEditDialog(parentActivity).show((game, uri) -> {
            game.lastModifiedTime = System.currentTimeMillis();
            mViewModel.update(game);
        }, base);
    }

    private void showAddGameDialog() {
        new GameEditDialog(parentActivity)
                .show(mViewModel::addGame);
    }

    private void launch(@NonNull Game game) {
        Fragment parent = requireParentFragment()
                .getParentFragment();
        assert parent instanceof HomeFragment;
        NavController navController = NavHostFragment.findNavController(parent);
        Bundle args = new Bundle();
        args.putParcelable(LaunchFragment.ARG_GAME, game);
        navController.navigate(R.id.launch_fragment, args);
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
            itemBinding.buttonMore.setOnClickListener(v -> {
                showMorePopupMenu(game, v);
            });
            itemBinding.buttonLaunch.setOnClickListener(v -> {
                handler.postDelayed(() -> launch(game), 340);
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