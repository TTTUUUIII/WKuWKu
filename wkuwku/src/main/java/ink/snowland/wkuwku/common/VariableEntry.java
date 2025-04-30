package ink.snowland.wkuwku.common;

public class VariableEntry<T> {
    public final String key;
    public T value;

    public VariableEntry(String key) {
        this.key = key;
    }
}
