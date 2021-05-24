package in.aerogram.eziosense.config.ui.helpers;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import in.aerogram.eziosense.config.data.local.pref.PreferencesUtil;

/**
 * @author rishabh-goel on 14-10-2020
 * @project Base
 */
public final class PermissionsHelper {

    private PermissionsHelper() {
    }

    /*
     * Check if version is marshmallow and above.
     * Used in deciding to ask runtime permission
     * */
    public static boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private static boolean shouldAskPermission(final Activity context, final String permission) {
        if (shouldAskPermission()) {
            return !isPermissionGranted(context, permission);
        }
        return false;
    }

    public static boolean isPermissionGranted(final Activity context, final String permission) {
        int permissionResult = ActivityCompat.checkSelfPermission(context, permission);
        return permissionResult == PackageManager.PERMISSION_GRANTED;
    }

    public static void checkPermission(final Activity context, final String permission, final PermissionAskListener listener) {
        /*
         * If permission is not granted
         * */
        if (shouldAskPermission(context, permission)) {
            /*
             * If permission denied previously
             * */
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                listener.onPermissionPreviouslyDenied();
            } else {
                /*
                 * Permission denied or first time requested
                 * */
                if (PreferencesUtil.isFirstTimeAskingPermission(context, permission)) {
                    PreferencesUtil.firstTimeAskingPermission(context, permission, false);
                    listener.onNeedPermission();
                } else {
                    /*
                     * Handle the feature without permission or ask user to manually allow permission
                     * */
                    listener.onPermissionDisabled();
                }
            }
        } else {
            listener.onPermissionGranted();
        }
    }

    public static void askPermission(final Activity context, final int requestCode, final String... permissions) {
        ActivityCompat.requestPermissions(context, permissions, requestCode);
    }

    /*
     * Callback on various cases on checking permission
     *
     * 1.  Below M, runtime permission not needed. In that case onPermissionGranted() would be called.
     *     If permission is already granted, onPermissionGranted() would be called.
     *
     * 2.  Above M, if the permission is being asked first time onNeedPermission() would be called.
     *
     * 3.  Above M, if the permission is previously asked but not granted, onPermissionPreviouslyDenied()
     *     would be called.
     *
     * 4.  Above M, if the permission is disabled by device policy or the user checked "Never ask again"
     *     check box on previous request permission, onPermissionDisabled() would be called.
     * */
    public interface PermissionAskListener {
        /*
         * Callback to ask permission
         * */
        void onNeedPermission();

        /*
         * Callback on permission denied
         * */
        void onPermissionPreviouslyDenied();

        /*
         * Callback on permission "Never show again" checked and denied
         * */
        void onPermissionDisabled();

        /*
         * Callback on permission granted
         * */
        void onPermissionGranted();
    }


}
