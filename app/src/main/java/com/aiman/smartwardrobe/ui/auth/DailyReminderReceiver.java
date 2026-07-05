package com.aiman.smartwardrobe.ui.auth;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.ui.MainActivity;

/**
 * ============================================================================
 * DailyReminderReceiver — Broadcast Receiver for Daily Reminders
 * ============================================================================
 *
 * <p>Fires when the AlarmManager alarms trigger (configured daily at 8:00 AM).
 * Shows a system notification reminding the user to check recommended outfits
 * and log their daily wears.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class DailyReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "daily_wardrobe_reminders";
    public static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);
        showNotification(context);
    }

    private void showNotification(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Fall back to app launcher icon
                .setContentTitle("Outfit Logger Reminder 🚨")
                .setContentText("Don't forget to compose and log your style outfits today!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Daily Reminders";
            String description = "Remind user to log styling outfits daily";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
