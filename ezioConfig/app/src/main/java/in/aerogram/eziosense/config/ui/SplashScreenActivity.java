package in.aerogram.eziosense.config.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;

import java.util.ArrayList;

import in.aerogram.eziosense.config.data.local.pref.PreferencesUtil;
import in.aerogram.eziosense.config.ui.home.HomeActivity;

/**
 * @author rishabh-goel on 14-10-2020
 * @project Base
 */
public class SplashScreenActivity extends AppCompatActivity {

    private static final String FEATURE_ONBOARDING = "onboarding";

    private SplitInstallManager splitInstallManager;
    private SplitInstallRequest splitInstallRequest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splitInstallManager = SplitInstallManagerFactory.create(this);
        splitInstallRequest = SplitInstallRequest
                .newBuilder()
                .addModule(FEATURE_ONBOARDING)
                .build();
        if (PreferencesUtil.isUserFirstTime(this)) processOnBoardingFeature();
        else {
            startActivity(HomeActivity.start(this, null));
            removeOnBoardingFeature();
            finish();
        }
    }

    private void processOnBoardingFeature() {
        splitInstallManager.startInstall(splitInstallRequest).addOnCompleteListener(task -> {
            if (task.isSuccessful()) navigateToOnBoarding();
            else processOnBoardingFeature();
        });
    }

    private void removeOnBoardingFeature() {
        final ArrayList<String> list = new ArrayList<>();
        list.add(FEATURE_ONBOARDING);
        splitInstallManager.deferredUninstall(list).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) removeOnBoardingFeature();
//            else finish();
        });
    }

    private void navigateToOnBoarding() {
        Intent intent = new Intent();
        intent.setClassName(this, "in.aerogram.eziosense.config.onboarding.PagerActivity");
        startActivity(intent);
        finish();
    }

}
