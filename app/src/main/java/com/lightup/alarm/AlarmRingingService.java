package com.lightup.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import java.io.IOException;

public class AlarmRingingService extends Service {
    static final String ACTION_START = "com.lightup.alarm.action.START_RINGING";
    static final String ACTION_STOP = "com.lightup.alarm.action.STOP_RINGING";

    private static final String CHANNEL_ID = "light_up_alarm_channel";
    private static final int NOTIFICATION_ID = 8127;
    private static final int REQUEST_OPEN_ALARM = 8200;

    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private Vibrator vibrator;
    private boolean ringing;
    private AlarmConfig activeAlarm;
    private long activeAlarmId = -1L;

    static void createAlarmChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Alarm alerts for Light Up.");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(null, null);
        channel.enableVibration(false);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createAlarmChannel(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopAlarm();
            return START_NOT_STICKY;
        }

        long alarmId = intent == null ? -1L : intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        startAlarm(alarmId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopSound();
        stopVibration();
        releaseWakeLock();
        super.onDestroy();
    }

    private void startAlarm(long alarmId) {
        activeAlarmId = alarmId;
        activeAlarm = AlarmStore.getAlarm(this, alarmId);
        startForeground(NOTIFICATION_ID, buildNotification());

        if (ringing) {
            return;
        }

        ringing = true;
        acquireWakeLock();
        startSound();
        startVibration();
        openAlarmScreen();
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, AlarmActivity.class);
        openIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, activeAlarmId);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_OPEN_ALARM,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle("Light Up alarm")
                .setContentText("Shine light at the phone to dismiss.")
                .setContentIntent(openPendingIntent)
                .setFullScreenIntent(openPendingIntent, true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSound(null);

        builder.setColor(getColor(R.color.light_up_primary));

        return builder.build();
    }

    private void openAlarmScreen() {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, activeAlarmId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(intent);
        } catch (RuntimeException ignored) {
            // Full-screen notification remains the fallback when background launch is restricted.
        }
    }

    private void startSound() {
        Uri customUri = activeAlarm == null || activeAlarm.ringtoneUri == null
                ? null
                : Uri.parse(activeAlarm.ringtoneUri);
        if (customUri != null && startSoundForUri(customUri)) {
            return;
        } else if (customUri != null) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "Custom alarm audio unavailable. Using system alarm sound.", Toast.LENGTH_LONG).show());
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        if (alarmUri != null) {
            startSoundForUri(alarmUri);
        }
    }

    private boolean startSoundForUri(Uri soundUri) {
        try {
            stopSound();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setDataSource(this, soundUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return true;
        } catch (IOException | RuntimeException exception) {
            stopSound();
            return false;
        }
    }

    private void stopSound() {
        if (mediaPlayer == null) {
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (IllegalStateException ignored) {
        }
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void startVibration() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        long[] pattern = new long[]{0L, 700L, 450L, 700L, 1200L};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return;
        }

        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "LightUp:AlarmWakeLock"
        );
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(10 * 60 * 1000L);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    private void stopAlarm() {
        ringing = false;
        stopSound();
        stopVibration();
        releaseWakeLock();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }
}
