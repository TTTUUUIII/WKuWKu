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
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentHistoryBinding;
import ink.snowland.wkuwku.databinding.ItemHistoryBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.ui.home.HomeFragment;
import ink.snowland.wkuwku.ui.launch.LaunchFragment;
import ink.snowland.wkuwku.util.FileManager;
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
        binding.recyclerView.setLayoutManager(new GridLayoutManager(parentActivity, 2));
        parentActivity.setActionbarTitle(R.string.recent_played);
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
        args.putBoolean(LaunchFragment.ARG_AUTO_RESTORE_STATE, true);
        args.putParcelable(LaunchFragment.ARG_GAME, game);
        navController.navigate(R.id.launch_fragment, args);
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
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true);
            Glide.with(itemBinding.getRoot())
                    .setDefaultRequestOptions(options)
                    .load(FileManager.getFile(FileManager.IMAGE_DIRECTORY, game.id + ".png"))
                    .into(itemBinding.screenShot);
            long elapsedRealtimeMillis = System.currentTimeMillis() - game.lastPlayedTime;
            long elapsedSeconds = (long) (elapsedRealtimeMillis / 1e3);
            if (elapsedSeconds > 24 * 60 * 60) {
                itemBinding.lastPlayedTime.setText(getString(R.string.fmt_played_days_ago, elapsedSeconds / 86400));
            } else if (elapsedSeconds > 60 * 60) {
                itemBinding.lastPlayedTime.setText(getString(R.string.fmt_played_hours_ago, elapsedSeconds / 3600));
            } else if (elapsedSeconds > 60) {
                itemBinding.lastPlayedTime.setText(getString(R.string.fmt_played_minutes_ago, elapsedSeconds / 60));
            } else {
                itemBinding.lastPlayedTime.setText(R.string.played_just_now);
            }
            itemBinding.play.setOnClickListener(v -> {
                handler.postDelayed(() -> launch(game), 340);
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