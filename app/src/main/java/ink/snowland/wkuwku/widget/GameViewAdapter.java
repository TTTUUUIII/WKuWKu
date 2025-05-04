package ink.snowland.wkuwku.widget;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ink.snowland.wkuwku.db.entity.Game;


public abstract class GameViewAdapter <T extends GameViewAdapter.GameViewHolder> extends ListAdapter<Game, GameViewAdapter.GameViewHolder> {

    protected GameViewAdapter() {
        super(new DiffUtil.ItemCallback<Game>() {
            @Override
            public boolean areItemsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    @NonNull
    @Override
    public abstract GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static abstract class GameViewHolder extends RecyclerView.ViewHolder {

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public abstract void bind(Game data);
    }
}
