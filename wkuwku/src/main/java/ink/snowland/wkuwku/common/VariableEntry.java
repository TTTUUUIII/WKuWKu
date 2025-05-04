package ink.snowland.wkuwku.common;

public class VariableEntry {
    public String key;
    public String value;

    public VariableEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public VariableEntry() {
        this("", "");
    }
}
