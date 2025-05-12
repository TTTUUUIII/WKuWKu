package ink.snowland.wkuwku.common;

public class ControllerDescription {
    public final String desc;
    public final int id;

    public ControllerDescription(String desc, int id) {
        this.desc = desc;
        this.id = id;
    }

    @Override
    public String toString() {
        return "ControllerDescription{" +
                "desc='" + desc + '\'' +
                ", id=" + id +
                '}';
    }
}
