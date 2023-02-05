package com.example.xiao.test1.Service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

public class GPSService {

    private Context context;
    private Activity activity;

    private LocationManager locationManager;

    //参数
    private String gpsTime;
    public String longitude;
    public String latitude;
    private String speed;
    private String bearing;

    public GPSService(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        locationManager = (LocationManager) this.activity.getSystemService(Context.LOCATION_SERVICE);
        openGPSSetting();
        startGPS();
    }

    private void startGPS() {
        if (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 监听状态
            locationManager.addGpsStatusListener(gpsListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            gpsTime = String.valueOf(location.getTime());
            longitude = String.valueOf(location.getLongitude());
            latitude = String.valueOf(location.getLatitude());
            if (location.hasBearing())
                bearing = String.valueOf(location.getBearing());
            else
                bearing = null;
            if (location.hasSpeed())
                speed = String.valueOf(location.getSpeed());
            else
                speed = null;
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
    private GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
//                    tvGps_status.setText("定位启动");
                    Log.i("GpsStatus", "定位启动");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
//                    tvGps_status.setText("定位结束");
                    Log.i("GpsStatus", "定位结束");
                    break;
            }
        }
    };

    /**
     * 跳转到系统的gps设置界面
     **/
    private void openGPSSetting() {
        if (locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, "GPS模块正常", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(context, "请开启GPS！", Toast.LENGTH_SHORT).show();
        // 跳转到GPS的设置页面
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(intent, 0); // 此为设置完成后返回到获取界面
    }

    public String getDataString() {
        return longitude + "," + latitude + "," + speed + "," + bearing + "," + gpsTime;
    }

    public void removeUpdates() {
        locationManager.removeUpdates(locationListener);
    }
}
