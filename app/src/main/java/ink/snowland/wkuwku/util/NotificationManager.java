package ink.snowland.wkuwku.util;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import ink.snowland.wkuwku.R;

public class NotificationManager {
    public final static String NOTIFICATION_DEFAULT_CHANNEL = "default";
    private static Context sApplicationContext;
    private static int nextId = 1;

    private NotificationManager() {
    }

    public static void initialize(Context applicationContext) {
        sApplicationContext = applicationContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat defaultChannel = new NotificationChannelCompat.Builder(NOTIFICATION_DEFAULT_CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Default")
                    .build();
            NotificationManagerCompat.from(sApplicationContext).createNotificationChannel(defaultChannel);
        }
    }

    public static int postNotification(String channel, String title, String content) {
        Notification notification = new NotificationCompat.Builder(sApplicationContext, channel)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(getPriority(channel))
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .build();
        return postNotification(notification);
    }

    public static int postNotification(Notification notification) {
        postNotification(nextId++, notification);
        return nextId - 1;
    }

    public static int postNotification(int id, Notification notification) {
        if (sApplicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(sApplicationContext).notify(id, notification);
        }
        return id;
    }

    private static int getPriority(String channel) {
        return NotificationCompat.PRIORITY_DEFAULT;
    }
}
