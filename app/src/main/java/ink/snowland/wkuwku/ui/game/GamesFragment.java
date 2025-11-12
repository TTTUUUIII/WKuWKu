package ink.snowland.wkuwku.ui.game;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.UiGameState;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentGameBinding;
import ink.snowland.wkuwku.databinding.ItemGameBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.ui.launch.LaunchFragment;
import ink.snowland.wkuwku.widget.GameDetailDialog;
import ink.snowland.wkuwku.widget.GameEditDialog;

public class GamesFragment extends BaseFragment implements View.OnClickListener {
    private GamesViewModel mViewModel;
    private final GameViewAdapter mAdapter = new GameViewAdapter();
    private GameDetailDialog mGameDetailDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(GamesViewModel.class);
        mViewModel.getAll()
                .observe(this, games ->
                    mAdapter.submitList(games.stream()
                            .map(UiGameState::from)
                            .collect(Collectors.toList())));
        mGameDetailDialog = new GameDetailDialog(parentActivity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentGameBinding binding = FragmentGameBinding.inflate(inflater);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.fab.setOnClickListener(this);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        binding.pendingIndicator.setDataModel(mViewModel);
        binding.pendingIndicator.setLifecycleOwner(this);
        return binding.getRoot();
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
        mAdapter.filter(newText.toLowerCase(Locale.US).trim());
        return true;
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
        private final ItemGameBinding itemBinding;
        public GameViewHolder(@NonNull ItemGameBinding binding) {
            super(binding.getRoot());
            itemBinding = binding;
        }

        public void bind(UiGameState state) {
            itemBinding.setViewData(state);
            itemBinding.buttonMore.setOnClickListener(v -> showMorePopupMenu(state.origin, v));
            itemBinding.buttonLaunch.setOnClickListener(v -> launch(state.origin));
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
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new GameViewHolder(ItemGameBinding.inflate(inflater, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        public void filter(@NonNull String query) {
            int index = query.indexOf(":");
            String queryBy = "title";
            String queryText = query;
            if (index != -1) {
                queryBy = query.substring(0, index);
                queryText = query.substring(index + 1);
            }
            final List<UiGameState> currentList = getCurrentList();
            for (int position = 0; position < currentList.size(); ++position) {
                UiGameState it = currentList.get(position);
                final String text;
                if (queryBy.equals("pub") || queryBy.equals("publisher")) {
                    text = it.origin.publisher.toLowerCase(Locale.US);
                } else {
                    text = it.origin.title.toLowerCase(Locale.US);
                }
                boolean hidden = !queryText.isEmpty() && !text.contains(queryText);
                if(it.isHidden() != hidden) {
                    it.setHidden(hidden);
                    notifyItemChanged(position);
                }
            }
        }
    }
}