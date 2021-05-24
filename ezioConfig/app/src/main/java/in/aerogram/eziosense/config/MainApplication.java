package in.aerogram.eziosense.config;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

import static in.aerogram.eziosense.config.utils.ToastUtil.showToast;

/**
 * @author rishabh-goel on 14-10-2020
 * @project Base
 */
public class MainApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "MainApplication";

    private Activity mCurrActivity;

    private static MainApplication mainApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected @Nullable
                String createStackElementTag(@NotNull StackTraceElement element) {
                    return String.format(
                            "Class:%s: Line: %s, Method: %s",
                            super.createStackElementTag(element),
                            element.getLineNumber(),
                            element.getMethodName());
                }
            });
            writeLogToFile();
            showToast(TAG);
        } else {
            Timber.plant(new ReleaseTree());
        }
        mainApplication = this;
        registerActivityLifecycleCallbacks(this);
    }

    public static MainApplication getMainApplication() {
        return mainApplication;
    }

    //region ActivityLifecycleCallbacks

    @Override
    public void onActivityStopped(Activity activity) {
        Timber.d("Tracking Activity Stopped " + activity.getLocalClassName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Timber.d("Tracking Activity Started " + activity.getLocalClassName());
        mCurrActivity = activity;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Timber.d("Tracking Activity SaveInstanceState " + activity.getLocalClassName());
        mCurrActivity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Timber.d("Tracking Activity Resumed " + activity.getLocalClassName());
        mCurrActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Timber.d("Tracking Activity Paused " + activity.getLocalClassName());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Timber.d("Tracking Activity Destroyed" + activity.getLocalClassName());
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Timber.d("Tracking Activity Created" + activity.getLocalClassName());
        mCurrActivity = activity;
    }

    public Activity getCurrActivity() {
        return mCurrActivity;
    }

    //endregion

    // region Writing Logs in file

    private void writeLogToFile() {
        if (isExternalStorageWritable()) {
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/LogsFolder");
            File logDirectory = new File(appDirectory + "/log");
            File logFile = new File(logDirectory, "logcat" + System.currentTimeMillis() + ".txt");

            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (isExternalStorageReadable()) {
            // only readable
        } else {
            // not accessible
        }

    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    // endregion


}
