package us.michaelchen.compasslogger.deadline;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by Manar on 6/3/2016.
 */
public class DeadlineService extends IntentService {

    private static final String PREFS_NAME = "CompassLoggerPrefs";
    private static final String PREFS_UNINSTALL_DEADLINE = "uninstall_deadline";

    public DeadlineService() {
        super("DeadlineService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("DeadlineService", "The deadline service has successfully started.");
        checkDeadline();
    }


    /*Checks the current time against the deadline time.*/
    private void checkDeadline() {
        if (isPassedDeadline()) {
            startDeadlineActivity();
        }
    }


    /*Returns whether the deadline has been reached.*/
    private boolean isPassedDeadline() {
        long currentTime = currentTime = System.currentTimeMillis();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        long deadlineTime = prefs.getLong(PREFS_UNINSTALL_DEADLINE, 0);

        Log.d("DeadlineService", "CurrentTime: " + currentTime );
        Log.d("DeadlineService", "DeadlineTime: " + deadlineTime);

        return (currentTime > deadlineTime);

    }

    /*Starts the DeadLine Activity dialog to prompt the user to uninstall.*/
    private void startDeadlineActivity() {
        Intent deadlineDialog = new Intent(this, DeadlineActivity.class);
        deadlineDialog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(deadlineDialog);
        Log.d("DeadlineService", "Just started the DeadlineActivity");
    }

}

