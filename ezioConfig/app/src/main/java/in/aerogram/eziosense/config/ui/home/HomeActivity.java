package in.aerogram.eziosense.config.ui.home;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.TouchNetUtil;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import in.aerogram.eziosense.config.R;
import in.aerogram.eziosense.config.databinding.ActivityHomeBinding;
import in.aerogram.eziosense.config.ui.BaseActivity;
import in.aerogram.eziosense.config.ui.helpers.LocationHelpers;
import in.aerogram.eziosense.config.ui.helpers.PermissionsHelper;
import in.aerogram.eziosense.config.ui.home.HomeActivityVM.WifiStateResult;
import timber.log.Timber;

import static in.aerogram.eziosense.config.ui.helpers.LocationHelpers.isGpsEnabled;
import static in.aerogram.eziosense.config.ui.helpers.PermissionsHelper.askPermission;
import static in.aerogram.eziosense.config.utils.ToastUtil.showToast;

/**
 * @author rishabh-goel on 09-12-2020
 * @project ezioConfig
 */
public class HomeActivity extends BaseActivity {

    private static final String TAG = "HomeActivity";

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private HomeActivityVM mViewModel;
    private ActivityHomeBinding mBinding;

    private EsptouchAsyncTask mTask;

    public static Intent start(final Context context, final String action) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (null != action && !"".equals(action)) intent.setAction(action);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkMandatoryPermissions()) askMandatoryPermissions();
        else setWifiResult();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTask != null) mTask.cancelEsptouch();
    }

    public void onInfoClick(View view) {
        Toast.makeText(this, "< Powered By Aerogram >", Toast.LENGTH_SHORT).show();
    }

    public void onConfirmClick(View view) {
        final WifiStateResult wifiStateResult = mViewModel.getWifiStateResult();
        if (!checkMandatoryPermissions()) askMandatoryPermissions();
        else if (null == wifiStateResult
                || !wifiStateResult.isConnected())
            showConnectWifi();
        else if (wifiStateResult.is5G()) showWifiNotSupported();
        else {
            final String inputSsid = mBinding.ahSsidTipet.getText().toString();
            final String inputPass = mBinding.ahPassTipet.getText().toString();
            Timber.i("Input SSID & Pass are %s, %s", inputSsid, inputPass);
            if (checkInputValues(inputSsid, inputPass)) {
                byte[] ssid = wifiStateResult.getSsidBytes() == null ? ByteUtil.getBytesByString(wifiStateResult.getSsid())
                        : wifiStateResult.getSsidBytes();
                byte[] password = inputPass == null ? null : ByteUtil.getBytesByString(inputPass);
                byte[] bssid = TouchNetUtil.parseBssid2bytes(wifiStateResult.getBssid());
                String devCountStr = "1";
                byte[] deviceCount = devCountStr == null ? new byte[0] : devCountStr.toString().getBytes();
                byte[] broadcast = {(byte) (true ? 1 : 0)};
                mTask = new EsptouchAsyncTask(this);
                mTask.execute(ssid, bssid, password, deviceCount, broadcast);
            }
        }
    }

    private void setupUI() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        mViewModel = new ViewModelProvider(this).get(HomeActivityVM.class);
    }

    private void observeViewModel() {
        mViewModel.getWifiStateResultLiveData().observe(this, wifiStateResult -> {
            if (!wifiStateResult.isConnected()) showConnectWifi();
            else if (wifiStateResult.is5G()) showWifiNotSupported();
            else {
                if (mTask != null) mTask.cancelEsptouch();
                setWifiResult();
            }
        });
    }


    // region Utility

    private boolean checkInputValues(final String ssid, final String pass) {
        if (null == ssid || ssid.equals("")) {
            mBinding.ahSsidTipl.setErrorEnabled(true);
            mBinding.ahSsidTipl.setError("Invalid SSID");
            return false;
        } else if (null == pass || pass.equals("") || pass.length() > 60) {
            mBinding.ahPassTipl.setErrorEnabled(true);
            mBinding.ahPassTipl.setError("Invalid Password");
            return false;
        } else {
            mBinding.ahSsidTipl.setErrorEnabled(false);
            mBinding.ahPassTipl.setErrorEnabled(false);
            return true;
        }
    }

    private void showConnectWifi() {
        mBinding.ahMessageTv.setText("Please Connect Wifi first and make sure GPS is enabled");
    }

    private void showWifiNotSupported() {
        mBinding.ahMessageTv.setText("This 5GHz band Wifi can't be used for Device Configuration!\nPlease connect to 2.4Ghz Band Wifi.");
    }

    private void setWifiResult() {
        final WifiStateResult wifiStateResult = mViewModel.getWifiStateResult();
        if (null != wifiStateResult && wifiStateResult.isConnected() && null != wifiStateResult.getSsid())
            mBinding.ahSsidTipet.setText(wifiStateResult.getSsid());
        mBinding.ahMessageTv.setText("");
    }

    // endregion

    // region Permissions

    private boolean isLocationPermissionGranted = false;
    private boolean isLocationPermissionDisabled = false;
    private boolean isGPSProviderEnabled = false;

    private boolean checkMandatoryPermissions() {
        isLocationPermissionGranted = PermissionsHelper.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION);
        isGPSProviderEnabled = isGpsEnabled(isLocationPermissionGranted);
        return isLocationPermissionGranted && isGPSProviderEnabled;
    }

    private void askMandatoryPermissions() {
        if (isLocationPermissionDisabled)
            Toast.makeText(this, "~ Location Permission Disabled by you! ~\nPlease Enable it under the App Settings first.", Toast.LENGTH_SHORT).show();
        else if (!isLocationPermissionGranted) askLocationPermission();
        else if (!isGPSProviderEnabled) getDeviceLocation();
    }

    public void askLocationPermission() {
        PermissionsHelper.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, permissionAskListener);
        if (!isLocationPermissionGranted) {
            askPermission(this, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            Toast.makeText(this, "Already Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private final PermissionsHelper.PermissionAskListener permissionAskListener = new PermissionsHelper.PermissionAskListener() {

        @Override
        public void onNeedPermission() {
            Timber.d("onNeedPermission");
            isLocationPermissionGranted = false;
            isLocationPermissionDisabled = false;
        }

        @Override
        public void onPermissionPreviouslyDenied() {
            Timber.d("onPermissionPreviouslyDenied");
            isLocationPermissionGranted = false;
            isLocationPermissionDisabled = false;
        }

        @Override
        public void onPermissionDisabled() {
            Timber.d("onPermissionDisabled");
            isLocationPermissionGranted = false;
            isLocationPermissionDisabled = true;
        }

        @Override
        public void onPermissionGranted() {
            Timber.d("onPermissionGranted");
            isLocationPermissionGranted = true;
            isLocationPermissionDisabled = false;
        }
    };

    public void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (isLocationPermissionGranted) {
            Task<LocationSettingsResponse> result =
                    LocationServices.getSettingsClient(this).checkLocationSettings(LocationHelpers.locationSettingRequest().build());
            result.addOnCompleteListener(task -> {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    isGPSProviderEnabled = true;
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        this,
                                        LocationRequest.PRIORITY_HIGH_ACCURACY);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                    isGPSProviderEnabled = false;
                }
            });
        } else {
            isGPSProviderEnabled = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted by User
                    isLocationPermissionDisabled = false;
                    isLocationPermissionGranted = true;
                } else {
                    // Permission Denied or Disabled by User
                    if (isLocationPermissionDisabled) {
                        Toast.makeText(this, "~ Location Permission Disabled by you! ~\nPlease Enable it under the App Settings first.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "~ Location Permission Denied ~", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // GPS
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        showToast("GPS Enabled!");
                        isGPSProviderEnabled = true;
                        break;
                    case Activity.RESULT_CANCELED:
                        isGPSProviderEnabled = false;
                        Toast.makeText(this, "~ GPS Access Denied ~", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
        }
    }

    // endregion

    // region ESP Task

    private static class EsptouchAsyncTask extends AsyncTask<byte[], IEsptouchResult, List<IEsptouchResult>> {

        private WeakReference<HomeActivity> mActivity;

        private final Object mLock = new Object();
        private ProgressDialog mProgressDialog;
        private AlertDialog mResultDialog;
        private IEsptouchTask mEsptouchTask;

        EsptouchAsyncTask(HomeActivity mActivity) {
            this.mActivity = new WeakReference<>(mActivity);
        }

        void cancelEsptouch() {
            cancel(true);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (mResultDialog != null) {
                mResultDialog.dismiss();
            }
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = mActivity.get();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(activity.getString(R.string.esptouch_configuring_message));
//            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(dialog -> {
                synchronized (mLock) {
                    if (mEsptouchTask != null) {
                        mEsptouchTask.interrupt();
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Close",
                    (dialog, which) -> {
                        synchronized (mLock) {
                            if (mEsptouchTask != null) {
                                mEsptouchTask.interrupt();
                            }
                        }
                    });
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(IEsptouchResult... values) {
            Context context = mActivity.get();
            if (context != null) {
                IEsptouchResult result = values[0];
                Timber.i("EspTouchResult: %s", result);
            }
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            HomeActivity activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                Context context = activity.getApplicationContext();
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, context);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(this::publishProgress);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            HomeActivity activity = mActivity.get();
            activity.mTask = null;
            mProgressDialog.dismiss();
            if (result == null) {
                mResultDialog = new AlertDialog.Builder(activity)
                        .setMessage(R.string.esptouch_configure_result_failed_port)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                return;
            }

            // check whether the task is cancelled and no results received
            IEsptouchResult firstResult = result.get(0);
            if (firstResult.isCancelled()) {
                return;
            }
            // the task received some results including cancelled while
            // executing before receiving enough results
            if (firstResult.isSuc()) {
                ArrayList<CharSequence> resultMsgList = new ArrayList<>(result.size());
                for (IEsptouchResult touchResult : result) {
                    String message = activity.getString(R.string.esptouch_configure_result_success_item,
                            touchResult.getBssid(), touchResult.getInetAddress().getHostAddress());
                    resultMsgList.add(message);
                }
                CharSequence[] items = new CharSequence[resultMsgList.size()];
                mResultDialog = new AlertDialog.Builder(activity)
                        .setTitle(R.string.esptouch_configure_result_success)
                        .setItems(resultMsgList.toArray(items), null)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
            }
        }
    }

    // endregion

}
