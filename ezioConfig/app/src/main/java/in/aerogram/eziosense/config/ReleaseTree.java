package in.aerogram.eziosense.config;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import timber.log.Timber;

/**
 * @author rishabh-goel on 14-10-2020
 * @project Base
 */
public class ReleaseTree extends Timber.Tree {

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {

        if (priority == Log.ERROR || priority == Log.WARN) {
            // TODO Send Errors to Crashlytics
        }

    }

}
