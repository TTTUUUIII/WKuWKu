package ink.snowland.wkuwku.common;

public class EmMessageExt {
    public static final int MESSAGE_TYPE_NOTIFICATION       = 0;
    public static final int MESSAGE_TYPE_NOTIFICATION_ALT   = 1;
    public static final int MESSAGE_TYPE_STATUS             = 2;
    public static final int MESSAGE_TYPE_PROGRESS           = 3;
    public static final int MESSAGE_LEVEL_DEBUG     = 0;
    public static final int MESSAGE_LEVEL_INFO      = 1;
    public static final int MESSAGE_LEVEL_WARN      = 2;
    public static final int MESSAGE_LEVEL_ERROR     = 3;
    public static final int MESSAGE_TARGET_ALL = 0;
    public static final int MESSAGE_TARGET_OSD = 1;
    public static final int MESSAGE_TARGET_LOG = 2;

    public final String msg;
    public final int priority;
    public final int level;
    public final int target;
    public final int type;
    public final int progress;
    public final int duration;

    public EmMessageExt(String msg, int priority, int level, int target, int type, int progress, int duration) {
        this.msg = msg;
        this.priority = priority;
        this.level = level;
        this.target = target;
        this.type = type;
        this.progress = progress;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "EmMessageExt{" +
                "msg='" + msg + '\'' +
                ", target=" + target +
                ", type=" + type +
                ", level=" + level +
                '}';
    }
}
