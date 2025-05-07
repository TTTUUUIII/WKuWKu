package ink.snowland.wkuwku.ui.history;

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
import ink.snowland.wkuwku.databinding.FragmentHistoryBinding;
import ink.snowland.wkuwku.databinding.ItemHistoryBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.ui.home.HomeFragment;
import ink.snowland.wkuwku.ui.play.PlayFragment;
import ink.snowland.wkuwku.util.TimeUtils;
import ink.snowland.wkuwku.widget.GameDetailDialog;
import ink.snowland.wkuwku.widget.GameViewAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HistoryFragment extends BaseFragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel mViewModel;
    private Disposable mDisposable;
    private final ViewAdapter mAdapter = new ViewAdapter();
    private GameDetailDialog mDetailDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        mDisposable = mViewModel.getHistory().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAdapter::submitList);
        mDetailDialog = new GameDetailDialog(parentActivity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        DividerItemDecoration decoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
        binding.recyclerView.addItemDecoration(decoration);
        parentActivity.setActionbarSubTitle(R.string.recent_played);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
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

    private void showMorePopupMenu(@NonNull View view, @NonNull Game game) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true);
        }
        popupMenu.getMenuInflater().inflate(R.menu.more_history_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_detail) {
                showDetailDialog(game);
            }
            return true;
        });
        popupMenu.show();
    }

    private void showDetailDialog(@NonNull Game game) {
        mDetailDialog.show(game);
    }

    private class ViewHolder extends GameViewAdapter.GameViewHolder {

        private final ItemHistoryBinding itemBinding;
        public ViewHolder(@NonNull ItemHistoryBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(@NonNull Game game) {
            itemBinding.setGame(game);
            itemBinding.buttonMore.setOnClickListener(v -> {
                showMorePopupMenu(v, game);
            });
            if (game.lastPlayedTime == 0) {
                itemBinding.lastPlayedTime.setText(getString(R.string.last_played_t) + ": " + getString(R.string.never_played));
            } else {
                itemBinding.lastPlayedTime.setText(getString(R.string.last_played_t) + ": " + TimeUtils.toString("MM/dd HH:mm", game.lastPlayedTime));
            }
            itemBinding.buttonLaunch.setOnClickListener(v -> {
                launch(game);
            });
        }
    }

    private class ViewAdapter extends GameViewAdapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemHistoryBinding itemBinding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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