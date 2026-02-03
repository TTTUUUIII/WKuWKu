package ink.snowland.wkuwku.ui.game;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentGameBinding;
import ink.snowland.wkuwku.databinding.ItemGameBinding;
import ink.snowland.wkuwku.databinding.ItemGameGridBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.ui.launch.LaunchFragment;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.GameDetailDialog;
import ink.snowland.wkuwku.widget.GameEditDialog;

public class GamesFragment extends BaseFragment implements View.OnClickListener {
    private GamesViewModel mViewModel;
    private FragmentGameBinding binding;
    private final GameViewAdapter mAdapter = new GameViewAdapter();
    private GameDetailDialog mGameDetailDialog;
    private List<Game> mFullList;
    private static final String USE_GRID_LAYOUT = "games_fragment.use_grid_layout";
    private boolean mUseGridLayout = SettingsManager.getBoolean(USE_GRID_LAYOUT, false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(GamesViewModel.class);
        mViewModel.getAll()
                .observe(this, games -> {
                    mFullList = games;
                    submitFilteredList(null);
                });
        mGameDetailDialog = new GameDetailDialog(parentActivity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater);
        updateLayoutManager();
        binding.fab.setOnClickListener(this);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        setUseGridListLayoutType(mUseGridLayout);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        parentActivity.setDisplayListLayoutToggleButton(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        parentActivity.setDisplayListLayoutToggleButton(false);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.fab) {
            GameEditDialog.createAndShow(parentActivity, mViewModel::addGame, null);
        }
    }
    @Override
    public boolean onQueryTextChange(String newText) {
        submitFilteredList(newText.toLowerCase(Locale.US).trim());
        return true;
    }

    public void submitFilteredList(@Nullable String query) {
        if (query == null) {
            query = "";
        }
        int index = query.indexOf(":");
        String queryBy = "title";
        String queryText = query;
        if (index != -1) {
            queryBy = query.substring(0, index);
            queryText = query.substring(index + 1);
        }
        final List<Game> newList = new ArrayList<>();
        for (int position = 0; position < mFullList.size(); ++position) {
            Game it = mFullList.get(position);
            final String text;
            if (queryBy.equals("pub") || queryBy.equals("publisher")) {
                text = it.publisher.toLowerCase(Locale.US);
            } else {
                text = it.title.toLowerCase(Locale.US);
            }
            if (!queryText.isEmpty() && !text.contains(queryText)) continue;
            newList.add(it);
        }
        mAdapter.submitList(newList);
    }

    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_toggle_list_layout_type) {
            mUseGridLayout = !mUseGridLayout;
            updateLayoutManager();
            setUseGridListLayoutType(mUseGridLayout);
            return true;
        }
        return super.onMenuItemSelected(menuItem);
    }

    private void updateLayoutManager() {
        final RecyclerView.LayoutManager lm;
        if (mUseGridLayout) {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float screenWidthInPx = displayMetrics.widthPixels;
            float itemWidthInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, displayMetrics);
            lm = new GridLayoutManager(requireContext(), (int) Math.max(1, screenWidthInPx / itemWidthInPx));
        } else {
            lm = new LinearLayoutManager(requireContext());
        }
        binding.recyclerView.setLayoutManager(lm);
        binding.recyclerView.setAdapter(mAdapter);
        if (mUseGridLayout != SettingsManager.getBoolean(USE_GRID_LAYOUT, false)) {
            SettingsManager.putBoolean(USE_GRID_LAYOUT, mUseGridLayout);
        }
    }

    private void showMorePopupMenu(@NonNull Game game, @NonNull View view) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true);
        }
        popupMenu.getMenuInflater().inflate(R.menu.game_more_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete) {
                game.state = Game.STATE_DELETED;
                mViewModel.update(game);
            } else if (itemId == R.id.action_edit) {
                GameEditDialog.createAndShow(parentActivity, (newGame, uri) -> {
                    newGame.lastModifiedTime = System.currentTimeMillis();
                    mViewModel.update(newGame);
                }, game);
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

    private void launch(@NonNull Game game) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle args = new Bundle();
        args.putParcelable(LaunchFragment.ARG_GAME, game);
        navController.navigate(R.id.launch_fragment, args, getNavAnimOptions());
    }


    private class GameViewHolder extends RecyclerView.ViewHolder {
        private final ViewDataBinding itemBinding;
        public GameViewHolder(@NonNull ViewDataBinding binding) {
            super(binding.getRoot());
            itemBinding = binding;
        }

        public void bind(Game game) {
            if (itemBinding instanceof ItemGameBinding itemGameBinding) {
                itemGameBinding.setGame(game);
                itemGameBinding.buttonMore.setOnClickListener(v -> showMorePopupMenu(game, v));
                itemGameBinding.buttonLaunch.setOnClickListener(v -> launch(game));
            } else if (itemBinding instanceof ItemGameGridBinding itemGameBinding) {
                itemGameBinding.setGame(game);
                itemGameBinding.buttonMore.setOnClickListener(v -> showMorePopupMenu(game, v));
                itemGameBinding.buttonLaunch.setOnClickListener(v -> launch(game));
                File cover = findGameCoverFile(game);
                if (cover != null) {
                    Glide.with(itemGameBinding.cover)
                            .load(cover)
                            .into(itemGameBinding.cover);
                }
                itemGameBinding.defaultCover.setVisibility(cover == null ? View.VISIBLE : View.GONE);
            }
        }
    }

    private File findGameCoverFile(Game game) {
        File dir = new File(game.filepath).getParentFile();
        File cover = new File(dir, "cover.jpg");
        if (cover.exists()) return cover;
        cover = new File(dir, "cover.png");
        if (cover.exists()) return cover;
        return null;
    }

    private class GameViewAdapter extends ListAdapter<Game, GameViewHolder> {
        protected GameViewAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @NonNull
        @Override
        public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == 1) {
                return new GameViewHolder(ItemGameGridBinding.inflate(inflater, parent, false));
            }
            return new GameViewHolder(ItemGameBinding.inflate(inflater, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        @Override
        public int getItemViewType(int position) {
            return mUseGridLayout ? 1 : 0;
        }
    }
}