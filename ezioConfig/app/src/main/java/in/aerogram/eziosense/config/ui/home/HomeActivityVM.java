package in.aerogram.eziosense.config.ui.home;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.InetAddress;

import in.aerogram.eziosense.config.ui.helpers.LocationHelpers;
import in.aerogram.eziosense.config.utils.NetworkUtils;
import timber.log.Timber;

import static android.content.Context.WIFI_SERVICE;
import static in.aerogram.eziosense.config.utils.NetworkUtils.getAddress;
import static in.aerogram.eziosense.config.utils.NetworkUtils.getIPv4Address;
import static in.aerogram.eziosense.config.utils.NetworkUtils.getIPv6Address;
import static in.aerogram.eziosense.config.utils.NetworkUtils.getRawSsidBytesOrElse;
import static in.aerogram.eziosense.config.utils.NetworkUtils.getSsidString;
import static in.aerogram.eziosense.config.utils.NetworkUtils.is5G;

/**
 * @author rishabh-goel on 10-12-2020
 * @project ezioConfig
 */
public class HomeActivityVM extends AndroidViewModel {

    private final WifiManager mWifiManager;
    private WifiStateResult mWifiStateResult;
    private final MutableLiveData<WifiStateResult> wifiStateResultLiveData = new MutableLiveData<>();

    public HomeActivityVM(@NonNull Application application) {
        super(application);
        mWifiManager = (WifiManager) application.getSystemService(WIFI_SERVICE);
        mWifiStateResult = null;
        registerWifiStatesRecv();
    }

    public WifiStateResult getWifiStateResult() {
        if (null == mWifiStateResult) {
            boolean wifiConnected = NetworkUtils.isWifiConnected(mWifiManager) && LocationHelpers.isGpsEnabled(true);
            final String ssid = getSsidString(mWifiManager.getConnectionInfo());
            final String bssid = mWifiManager.getConnectionInfo().getBSSID();
            final byte[] ssidBytesOrElse = getRawSsidBytesOrElse(mWifiManager.getConnectionInfo(), ssid.getBytes());
            final InetAddress ipAddr = getWifiAddress();
            boolean is5G = is5G(mWifiManager.getConnectionInfo().getFrequency());
            mWifiStateResult = new WifiStateResult(ssid, bssid, ssidBytesOrElse, ipAddr, is5G, wifiConnected);
        }
        return mWifiStateResult;
    }

    public LiveData<WifiStateResult> getWifiStateResultLiveData() {
        return wifiStateResultLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        getApplication().unregisterReceiver(mReceiver);
    }

    // region Utility

    private InetAddress getWifiAddress() {
        int ipValue = mWifiManager.getConnectionInfo().getIpAddress();
        if (ipValue != 0) {
            return getAddress(mWifiManager.getConnectionInfo().getIpAddress());
        } else {
            final InetAddress iPv4Address = getIPv4Address();
            if (iPv4Address == null) {
                return getIPv6Address();
            }
            return iPv4Address;
        }
    }

    private void onWifiChanged() {
        Timber.i("Before Wifi changed %s", getWifiStateResult().toString());
        this.mWifiStateResult = null;
        this.mWifiStateResult = getWifiStateResult();
        wifiStateResultLiveData.setValue(this.mWifiStateResult);
        Timber.i("After Wifi changed %s", getWifiStateResult().toString());
    }

    // endregion

    // region Wifi

    private void registerWifiStatesRecv() {
        final IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        getApplication().registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION: // WIFI Toggled
                case LocationManager.PROVIDERS_CHANGED_ACTION: // GPS Toggled
                    onWifiChanged();
                    break;
            }
        }
    };

    static class WifiStateResult {
        private final String ssid;
        private final String bssid;
        private final byte[] ssidBytes;
        private final InetAddress address;
        private final boolean is5G;
        private final boolean isConnected;

        WifiStateResult(final String ssid, final String bssid, final byte[] ssidBytes, final InetAddress address,
                        final boolean is5G, final boolean isConnected) {
            this.ssid = ssid;
            this.bssid = bssid;
            this.ssidBytes = ssidBytes;
            this.address = address;
            this.is5G = is5G;
            this.isConnected = isConnected;
        }

        public String getSsid() {
            return ssid;
        }

        public String getBssid() {
            return bssid;
        }

        public byte[] getSsidBytes() {
            return ssidBytes;
        }

        public InetAddress getAddress() {
            return address;
        }

        public boolean is5G() {
            return is5G;
        }

        public boolean isConnected() {
            return isConnected;
        }

        @NonNull
        @Override
        public String toString() {
            return "SSID: " + ssid + " Bssid: " + bssid + " address: " + address.toString() + " is5G: " + is5G + " connected: " + isConnected;
        }

    }

    // endregion

}
