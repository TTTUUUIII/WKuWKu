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
    private FragmentGameBinding binding;
    private GamesViewModel mViewModel;
    private final UiGameViewAdapter mAdapter = new UiGameViewAdapter();
    private GameDetailDialog mGameDetailDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(GamesViewModel.class);
        mViewModel.getAll()
                .observe(this, games -> {
                    mAdapter.submitList(games.stream()
                            .map(UiGameState::from)
                            .collect(Collectors.toList()));
                });
        mGameDetailDialog = new GameDetailDialog(parentActivity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater);
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
            showAddGameDialog();
        }
    }
    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.filter(newText.toLowerCase(Locale.US));
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
        NavController navController = NavHostFragment.findNavController(this);
        Bundle args = new Bundle();
        args.putParcelable(LaunchFragment.ARG_GAME, game);
        navController.navigate(R.id.launch_fragment, args, getNavAnimOptions());
    }


    private class UiGameViewHolder extends RecyclerView.ViewHolder {
        private final ItemGameBinding binding;
        public UiGameViewHolder(@NonNull ItemGameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(UiGameState state) {
            binding.setViewData(state);
            binding.buttonMore.setOnClickListener(v -> showMorePopupMenu(state.origin, v));
            binding.buttonLaunch.setOnClickListener(v -> launch(state.origin));
        }
    }

    private class UiGameViewAdapter extends ListAdapter<UiGameState, UiGameViewHolder> {
        protected UiGameViewAdapter() {
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
        public UiGameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UiGameViewHolder(ItemGameBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UiGameViewHolder holder, int position) {
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
            final List<UiGameState> list = getCurrentList();
            for (UiGameState it : list) {
                final String text;
                if (queryBy.equals("pub") || queryBy.equals("publisher")) {
                    text = it.origin.publisher.toLowerCase(Locale.US);
                } else {
                    text = it.origin.title.toLowerCase(Locale.US);
                }
                boolean hidden = !queryText.isEmpty() && !text.contains(queryText);
                it.setHidden(hidden);
            }
        }
    }
}