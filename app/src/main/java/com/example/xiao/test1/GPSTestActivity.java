package com.example.xiao.test1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class GPSTestActivity extends AppCompatActivity {

    private static final String TAG = "GPSTestActivity";
    private TextView textViewGPS;
    private TextView tvGPS_status;
    private TextView tvSatellite_status;
    private LocationManager locationManager;
    private SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault());
    private SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private FileOperation fileOperation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpstest);

        textViewGPS = findViewById(R.id.textViewGPS);
        tvGPS_status = findViewById(R.id.GPS_status);
        tvSatellite_status = findViewById(R.id.Satellite_status);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 判断GPS是否正常启动  
        openGPSSetting();

        tvGPS_status.setText("等待搜索GPS...");
        tvSatellite_status.setText("搜索卫星...");
        textViewGPS.setText("设备位置信息:");

        // 为获取地理位置信息时设置查询条件  
        String bestProvider = locationManager.getBestProvider(getCriteria(), true);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            String filePath = "/sdcard/Test/";
            String fileName = df1.format(new Date());
            fileOperation = new FileOperation();
            fileOperation.setFilePath(filePath);
            fileOperation.setFileName(fileName);
            // 获取位置信息
            // 如果不设置查询要求，getLastKnownLocation方法传入的参数为LocationManager.GPS_PROVIDER
            Location location = locationManager.getLastKnownLocation(bestProvider);
            //updateView(location);
            // 监听状态
            locationManager.addGpsStatusListener(listener);
            // 绑定监听，有4个参数
            // 参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
            // 参数2，位置信息更新周期，单位毫秒
            // 参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
            // 参数4，监听
            // 备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新

            // 1秒更新一次，或最小位移变化超过1米更新一次；
            // 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 0, locationListener);
        } else {
            Toast.makeText(this, "无法开启GPS...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 跳转到系统的gps设置界面
     **/
    private void openGPSSetting() {
        if (locationManager
                .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS模块正常", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "请开启GPS！", Toast.LENGTH_SHORT).show();
        // 跳转到GPS的设置页面
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, 0); // 此为设置完成后返回到获取界面
    }

    // 位置监听  
    private LocationListener locationListener = new LocationListener() {

        /**
         * 位置信息变化时触发
         */
        public void onLocationChanged(Location location) {
            updateView(location);
            String time = df2.format(location.getTime());
            Log.i(TAG, "时间：" + time);
            Log.i(TAG, "经度：" + location.getLongitude());
            Log.i(TAG, "纬度：" + location.getLatitude());
            Log.i(TAG, "海拔：" + location.getAltitude());

        }

        /**
         * GPS状态变化时触发
         */
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "StatusChanged");
            switch (status) {
                // GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    //Log.i(TAG, "当前GPS状态为可见状态");
                    tvGPS_status.setText("当前GPS状态为可见状态");
                    break;
                // GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    //Log.i(TAG, "当前GPS状态为服务区外状态");
                    tvGPS_status.setText("当前GPS状态为服务区外状态");
                    break;
                // GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    //Log.i(TAG, "当前GPS状态为暂停服务状态");
                    tvGPS_status.setText("当前GPS状态为暂停服务状态");
                    break;
            }
        }

        /**
         * GPS开启时触发
         */
        public void onProviderEnabled(String provider) {
//            Location location = locationManager.getLastKnownLocation(provider);
//            updateView(location);
        }

        /**
         * GPS禁用时触发
         */
        public void onProviderDisabled(String provider) {
            updateView(null);
        }


    };

    // 状态监听  
    GpsStatus.Listener listener = new GpsStatus.Listener() {

        public void onGpsStatusChanged(int event) {
            switch (event) {
                // 第一次定位  
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i(TAG, "第一次定位");
                    tvSatellite_status.setText("第一次定位");
                    break;
                // 卫星状态改变  
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    //Log.i(TAG, "卫星状态改变");
                    tvSatellite_status.setText("卫星状态改变");
                    // 获取当前状态  
                    @SuppressLint("MissingPermission") GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    // 获取卫星颗数的默认最大值  
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    // 创建一个迭代器保存所有卫星  
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites()
                            .iterator();
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    tvSatellite_status.setText("搜索到：" + count + "颗卫星");
                    //System.out.println("搜索到：" + count + "颗卫星");
                    break;
                // 定位启动  
                case GpsStatus.GPS_EVENT_STARTED:
                    //Log.i(TAG, "定位启动");
                    tvSatellite_status.setText("定位启动");
                    break;
                // 定位结束  
                case GpsStatus.GPS_EVENT_STOPPED:
                    //Log.i(TAG, "定位结束");
                    tvSatellite_status.setText("定位结束");
                    break;
            }
        }
    };

    /**
     * 实时更新文本内容
     *
     * @param location
     */
    private void updateView(Location location) {
        if (location != null) {
            String time = String.valueOf(location.getTime());//df2.format(new Date());
            String bearing;
            String speed;
            if (location.hasBearing())
                bearing = String.valueOf(location.getBearing());
            else
                bearing = null;
            if (location.hasSpeed())
                speed = String.valueOf(location.getSpeed());
            else
                speed = null;
            String date = "时间：" + time
                    + "\t经度：" + String.valueOf(location.getLongitude())
                    + "\t纬度：" + String.valueOf(location.getLatitude())
                    + "\t速度：" + speed
                    + "\tbearing" + bearing;
            textViewGPS.setText(date);
            //fileOperation.write(date);
        }
    }

    /**
     * 返回查询条件
     *
     * @return
     */
    private Criteria getCriteria() {
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细  
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 设置是否要求速度  
        criteria.setSpeedRequired(false);
        // 设置是否允许运营商收费  
        criteria.setCostAllowed(false);
        // 设置是否需要方位信息  
        criteria.setBearingRequired(false);
        // 设置是否需要海拔信息  
        criteria.setAltitudeRequired(false);
        // 设置对电源的需求  
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }
}

