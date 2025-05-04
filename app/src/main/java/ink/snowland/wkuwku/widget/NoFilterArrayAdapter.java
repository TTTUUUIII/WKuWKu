package ink.snowland.wkuwku.widget;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import java.util.List;

public class NoFilterArrayAdapter<T> extends ArrayAdapter<T> {

    public NoFilterArrayAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public NoFilterArrayAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull T[] objects) {
        super(context, resource, objects);
    }

    public NoFilterArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull T[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
    }

    public NoFilterArrayAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new InvalidFilter();
    }

    private static class InvalidFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

        }
    }
}
