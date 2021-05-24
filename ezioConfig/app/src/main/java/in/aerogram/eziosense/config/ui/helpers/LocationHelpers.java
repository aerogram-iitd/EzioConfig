package in.aerogram.eziosense.config.ui.helpers;

import android.location.Address;
import android.location.Geocoder;
import android.provider.Settings;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static in.aerogram.eziosense.config.MainApplication.getMainApplication;

/**
 * @author rishabh-goel on 14-10-2020
 * @project Base
 */
public final class LocationHelpers {

    private LocationHelpers() {

    }

    public static boolean isGpsEnabled(final boolean isLocationPermissionGranted) {
        final int locationMode;
        try {
            locationMode = Settings.Secure.getInt(getMainApplication().getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        switch (locationMode) {
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                return true;
            case Settings.Secure.LOCATION_MODE_OFF:
            default:
                return false;
        }
    }


    public static String getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(getMainApplication(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && addresses.size() > 0) {
                Address obj = addresses.get(0);
                return obj.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCityName(double lat, double lng) {
        Geocoder geocoder = new Geocoder(getMainApplication(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && addresses.size() > 0) {
                Address obj = addresses.get(0);
                return obj.getAdminArea();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static LocationRequest getLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(30000);
//        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    public static LocationSettingsRequest.Builder locationSettingRequest() {
        return new LocationSettingsRequest.Builder()
                .addLocationRequest(getLocationRequest());
    }

}
