package in.aerogram.eziosense.config.data.local.pref;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * @author rishabh-goel on 14-10-2020
 * @project Base
 */
public class PreferencesUtil {

    private static final String TAG = "PreferencesUtil";

    private static final String PREFS_FILE_NAME = "YOUR_NAME_preference";

    private static final String PREF_USER_FIRST_TIME = TAG + "userfirsttime";

    public static void firstTimeAskingPermission(Context context, String permission, boolean isFirstTime) {
        SharedPreferences sharedPreference = context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
        sharedPreference.edit().putBoolean(permission, isFirstTime).apply();
    }

    public static boolean isFirstTimeAskingPermission(Context context, String permission) {
        return context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(permission, true);
    }

    public static boolean isUserFirstTime(Context context) {
        return context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(PREF_USER_FIRST_TIME, true);
    }

    public static void userFirstTimeComplete(Context context) {
        context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).edit().putBoolean(PREF_USER_FIRST_TIME, false).apply();
    }

}
