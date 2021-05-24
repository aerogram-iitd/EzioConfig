package in.aerogram.eziosense.config.onboarding;

import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import in.aerogram.eziosense.config.onboarding.ui.main.SectionsPagerAdapter;
import in.aerogram.eziosense.config.data.local.pref.PreferencesUtil;

public class PagerActivity extends AppCompatActivity {

    private static final String TAG = "PagerActivity";
    
    @ColorRes
    public int[] COLOR_LIST;
    
    final ArgbEvaluator evaluator = new ArgbEvaluator();
    private ImageView zero, one, two;
    private ImageView[] indicators;
    
    private int page = 0;   //  to track page position
    
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager2 mViewPager;
    private Button mSkipBtn;
    private Button mNextBtn;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black_trans80));
        }
        setContentView(R.layout.activity_pager);
        final int color1 = ContextCompat.getColor(this, R.color.green);
        final int color2 = ContextCompat.getColor(this, R.color.grey);
        final int color3 = ContextCompat.getColor(this, R.color.purple);
        COLOR_LIST = new int[]{color1, color2, color3};
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getLifecycle());
        mViewPager = findViewById(R.id.ap_view_pager);
        mNextBtn = findViewById(R.id.ibb_nxt_btn);
        setupNxtButton();
        mSkipBtn = findViewById(R.id.ibb_skip_btn);
        setupSkipButton();
        zero = findViewById(R.id.ibb_indicator0_iv);
        one = findViewById(R.id.ibb_indicator1_iv);
        two = findViewById(R.id.ibb_indicator2_iv);
        indicators = new ImageView[]{zero, one, two};
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(page);
        updateIndicators(page);
        setupPageChangeListener();
    }
    
    private void setupNxtButton() {
        mNextBtn.setOnClickListener(v -> {
            // Last Page
            if (page == COLOR_LIST.length - 1 || getResources().getString(R.string.finish).contentEquals(mNextBtn.getText())) {
               navigateToSetupActivity();
                return;
            }
            page += 1;
            mViewPager.setCurrentItem(page, true);
        });
    }
    
    private void setupSkipButton() {
        mSkipBtn.setOnClickListener(v -> navigateToSetupActivity());
    }
    
    private void setupPageChangeListener() {
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                int colorUpdate = (Integer) evaluator.evaluate(positionOffset, COLOR_LIST[position], COLOR_LIST[position == COLOR_LIST.length - 1 ? position : position + 1]);
                mViewPager.setBackgroundColor(colorUpdate);
            }
    
            @Override
            public void onPageSelected(int position) {
                page = position;
                updateIndicators(page);
                switch (position) {
                    case 0:
                        mViewPager.setBackgroundColor(COLOR_LIST[0]);
                        break;
                    case 1:
                        mViewPager.setBackgroundColor(COLOR_LIST[1]);
                        break;
                    case 2:
                        mViewPager.setBackgroundColor(COLOR_LIST[2]);
                        break;
                }
                if (position == COLOR_LIST.length - 1) {
                    mNextBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    mNextBtn.setText(R.string.finish);
                }
            }
        });
    }
    
    void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setBackgroundResource(
                    i == position ? R.drawable.ic_radio_button_checked_black_24dp : R.drawable.ic_radio_button_unchecked_black_24dp
            );
        }
    }

    private void navigateToSetupActivity() {
        PreferencesUtil.userFirstTimeComplete(this);
        Intent intent = new Intent();
        intent.setClassName(this, "in.aerogram.eziosense.config.ui.home.HomeActivity");
        startActivity(intent);
        finish();
    }

}