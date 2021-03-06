package us.michaelchen.compasslogger.periodicservices.datarecording;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

import us.michaelchen.compasslogger.utils.BetterLocation;
import us.michaelchen.compasslogger.utils.DataTimeFormat;
import us.michaelchen.compasslogger.utils.TimeConstants;

/**
 * Created by ioreyes on 5/27/16.
 */
public class LocationRecordingService extends AbstractRecordingService {
    private static final String FIX_TIME_KEY = "fixTime";
    private static final String LATITUDE_KEY = "lat";
    private static final String LONGITUDE_KEY = "lon";
    private static final String PROVIDER_KEY = "provider";

    private Location bestLocation = null;
    private boolean hasPermissions = false;
    private LocationManager locationManager = null;

    private final LocationListener LOCATION_LISTENER = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            bestLocation = BetterLocation.compare(location, bestLocation);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    public LocationRecordingService() {
        super("LocationRecordingService");
    }


    @Override
    protected String broadcastKey() {
        return "location";
    }

    @Override
    protected Map<String, Object> readData(Intent intent) {
        registerLocationListener();
        long endTime = System.currentTimeMillis() + TimeConstants.MAX_SENSOR_TIME;
        while(hasPermissions && System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(TimeConstants.SENSOR_DATA_POLL_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        unregisterLocationListener();

        Map<String, Object> data = new HashMap<>();
        if(bestLocation != null) {
            data.put(FIX_TIME_KEY, DataTimeFormat.format(bestLocation.getTime()));
            data.put(LATITUDE_KEY, bestLocation.getLatitude());
            data.put(LONGITUDE_KEY, bestLocation.getLongitude());
            data.put(PROVIDER_KEY, bestLocation.getProvider());
        }

        return data;
    }

    /**
     * Starts updates from the location service
     */
    private void registerLocationListener() {
        hasPermissions = false;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasPermissions = true;

            for(String provider : locationManager.getAllProviders()) {
                // Register the listener to all providers
                locationManager.requestLocationUpdates(provider, 0, 0.0f, LOCATION_LISTENER);

                // Get the best last known location from all the providers
                Location loc = locationManager.getLastKnownLocation(provider);
                bestLocation = BetterLocation.compare(loc, bestLocation);
            }
        }
    }

    /**
     * Stops updates from the location service
     */
    private void unregisterLocationListener() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(LOCATION_LISTENER);
        }
    }
}
