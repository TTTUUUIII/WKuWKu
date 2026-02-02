package ink.snowland.wkuwku.ui.history;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.UiGameState;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentHistoryBinding;
import ink.snowland.wkuwku.databinding.ItemHistoryBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.ui.launch.LaunchFragment;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.widget.GameDetailDialog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HistoryFragment extends BaseFragment {

    private HistoryViewModel mViewModel;
    private Disposable mDisposable;
    private final GameViewAdapter mAdapter = new GameViewAdapter();
    private GameDetailDialog mDetailDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        mDisposable = mViewModel.getHistory().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(games -> {
                    mAdapter.submitList(games.stream()
                            .map(UiGameState::from)
                            .collect(Collectors.toList()));
                });
        mDetailDialog = new GameDetailDialog(parentActivity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentHistoryBinding binding = FragmentHistoryBinding.inflate(inflater);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(parentActivity, getGridSpanCount()));
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        return binding.getRoot();
    }

    private int getGridSpanCount() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthInPx = displayMetrics.widthPixels;
        float itemWidthInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, displayMetrics);
        return (int) Math.max(1, screenWidthInPx / itemWidthInPx);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
    }

    private void launch(@NonNull Game game) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle args = new Bundle();
        args.putBoolean(LaunchFragment.ARG_AUTO_LOAD_STATE, true);
        args.putParcelable(LaunchFragment.ARG_GAME, game);
        navController.navigate(R.id.launch_fragment, args, getNavAnimOptions());
    }

    private void showMorePopupMenu(@NonNull View view, @NonNull Game game) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true);
        }
        popupMenu.getMenuInflater().inflate(R.menu.history_more_menu, popupMenu.getMenu());
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

    private class GameViewHolder extends RecyclerView.ViewHolder {

        private final ItemHistoryBinding itemBinding;
        public GameViewHolder(@NonNull ItemHistoryBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(@NonNull UiGameState gameState) {
            itemBinding.setGameState(gameState);
            itemBinding.buttonMore.setOnClickListener(v -> showMorePopupMenu(v, gameState.origin));
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true);
            Glide.with(itemBinding.getRoot())
                    .setDefaultRequestOptions(options)
                    .load(FileManager.getFile(FileManager.IMAGE_DIRECTORY, gameState.origin.id + ".png"))
                    .into(itemBinding.screenShot);
            long elapsedRealtimeMillis = System.currentTimeMillis() - gameState.origin.lastPlayedTime;
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
            itemBinding.play.setOnClickListener(v -> launch(gameState.origin));
        }
    }

    private class GameViewAdapter extends ListAdapter<UiGameState, GameViewHolder> {

        protected GameViewAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull UiGameState oldItem, @NonNull UiGameState newItem) {
                    return oldItem.origin.id == newItem.origin.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull UiGameState oldItem, @NonNull UiGameState newItem) {
                    return oldItem.isHidden() == newItem.isHidden() && oldItem.origin.equals(newItem.origin);
                }
            });
        }

        @NonNull
        @Override
        public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemHistoryBinding itemBinding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new GameViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        @Override
        public void submitList(@Nullable List<UiGameState> list) {
            super.submitList(list);
            if (list != null) {
                mViewModel.setEmptyListIndicator(list.isEmpty());
            }
        }

//        public void filter(@NonNull String query) {
//            int index = query.indexOf(":");
//            String queryBy = "title";
//            String queryText = query;
//            if (index != -1) {
//                queryBy = query.substring(0, index);
//                queryText = query.substring(index + 1);
//            }
//            final List<UiGameState> currentList = getCurrentList();
//            for (int position = 0; position < currentList.size(); ++position) {
//                UiGameState it = currentList.get(position);
//                final String text;
//                if (queryBy.equals("pub") || queryBy.equals("publisher")) {
//                    text = it.origin.publisher.toLowerCase(Locale.US);
//                } else {
//                    text = it.origin.title.toLowerCase(Locale.US);
//                }
//                boolean hidden = !queryText.isEmpty() && !text.contains(queryText);
//                if(it.isHidden() != hidden) {
//                    it.setHidden(hidden);
//                    notifyItemChanged(position);
//                }
//            }
//        }
    }
}