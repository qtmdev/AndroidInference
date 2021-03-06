package us.michaelchen.compasslogger.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import us.michaelchen.compasslogger.periodicservices.Periodics;
import us.michaelchen.compasslogger.periodicservices.datarecording.DeviceSpecsRecordingService;
import us.michaelchen.compasslogger.periodicservices.keepalive.AbstractKeepAliveService;
import us.michaelchen.compasslogger.receiver.GenericIntentReceiver;
import us.michaelchen.compasslogger.receiver.PeriodicReceiver;

/**
 * Created by ioreyes on 6/2/16.
 */
public class MasterSwitch {
    // Used by periodics
    private static PendingIntent periodicIntent = null;

    private static Context app = null;

    /**
     * Turns on all the data collection services
     * @param c Calling Android context
     */
    public static void on(Context c) {
        // Associate all spawned services and receivers to the Application,
        // not component Activities and Services (which have much shorter lifespans)
        if(app == null) {
            app = c.getApplicationContext();
        }

        if(!isRunning()) {
            if(PreferencesWrapper.isFirstRun()) {
                recordDeviceSpecs();
                // Now, update the FIRST_RUN check.
                PreferencesWrapper.setFirstRun();
            }

            startPeriodics();
        }
    }

    /**
     * Turns off all the data collection services
     * @param c Calling Android context
     */
    public static void off(Context c) {
        // Associate all spawned services and receivers to the Application,
        // not component Activities and Services (which have much shorter lifespans)
        if(app == null) {
            app = c.getApplicationContext();
        }

        if(isRunning()) {
            stopPeriodics();

            // Reset the timestamp for future iterations
            // to assume a MasterSwitch.On() alarm reset.
            PreferencesWrapper.resetLastAlarmTimestamp();
        }
    }

    /**
     *
     * @return True if the data collection services are active by
     * checking whether the current time is within a safe-factored
     * PERIODIC_LENGTH of the previous time. In other words, if the
     * alarms are active, then the previous time stamp should be
     * greater than the safe (110%) interval of the PERIODIC_LENGTH.
     * Will logically return false when prevTimeStamp = 0.
     */
    public static boolean isRunning() {
        long prevTimeStamp = PreferencesWrapper.getLastAlarmTimestamp();
        long currentTimeStamp = System.currentTimeMillis();
        long interval = currentTimeStamp - prevTimeStamp;

        // The service is considered to still be running if the last periodic was within
        // the last period length (plus a TimeConstants.PERIODIC_SAFE_FACTOR tolerance)
        return interval < TimeConstants.PERIODIC_SAFE_INTERVAL;
    }

    /**
     * Start periodic events
     */
    private static void startPeriodics() {
        if(periodicIntent == null) {
            Intent alarmIntent = new Intent(app, PeriodicReceiver.class);
            periodicIntent = PendingIntent.getBroadcast(app, 0, alarmIntent, 0);
        }

        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                TimeConstants.PERIODIC_LENGTH,
                periodicIntent);

        Toast.makeText(app, "Alarms Set", Toast.LENGTH_SHORT).show();

        // Note that the periodic started at this time
        PreferencesWrapper.updateLastAlarmTimestamp();
    }

    /**
     * Stop periodic events
     */
    private static void stopPeriodics() {
        // Cancel the alarms
        if(periodicIntent != null) {
            AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            manager.cancel(periodicIntent);
        }

        // Stop the keep-alive services
        for(Class c : Periodics.KEEP_ALIVE) {
            Intent intent = new Intent(app, c);
            intent.setAction(AbstractKeepAliveService.ACTION_SHUTDOWN);
            app.startService(intent);
        }
    }

    /**
     * Launches service to get device hardware/software information
     */
    private static void recordDeviceSpecs() {
        Intent intent = new Intent(app, DeviceSpecsRecordingService.class);
        app.startService(intent);
    }
}
