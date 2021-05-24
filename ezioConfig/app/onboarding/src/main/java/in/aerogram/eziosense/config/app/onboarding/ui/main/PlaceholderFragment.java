package in.aerogram.eziosense.config.onboarding.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import in.aerogram.eziosense.config.onboarding.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private ImageView mImageView;
    private TextView mMainTextView;
    private TextView mMainDescTextView;

    static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mImageView = view.findViewById(R.id.fp_main_iv);
        mMainTextView = view.findViewById(R.id.fp_main_text_tv);
        mMainDescTextView = view.findViewById(R.id.fp_main_text_desc_tv);
        setupView(getArguments().getInt(ARG_SECTION_NUMBER) - 1);
    }

    private void setupView(int pageNo) {
        switch (pageNo) {
            case 0:
                mImageView.setImageResource(R.drawable.ic_configure);
                mMainTextView.setText("Configure Ezio Devices");
                mMainDescTextView.setText("Ezio Configure is the Smart configuration app brought to you by Aerogram");
                break;
            case 1:
                mImageView.setImageResource(R.drawable.ic_router);
                mMainTextView.setText("Connect to Router");
                mMainDescTextView.setText("Configure your Ezio devices without actually making a direct connection to them");
                break;
            case 2:
                mImageView.setImageResource(R.drawable.ic_device);
                mMainTextView.setText("Make devices Smart");
                mMainDescTextView.setText("Smart configuration helps you configure your devices in no time");
                break;
        }
    }

}