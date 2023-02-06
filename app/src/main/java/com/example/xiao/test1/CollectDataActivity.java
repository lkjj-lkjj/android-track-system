package com.example.xiao.test1;

import static com.example.xiao.test1.MapShow.assetFilePath;
import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.example.xiao.test1.Service.GPSService;
import com.example.xiao.test1.Service.SensorService;
import com.example.xiao.test1.utils.Step;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CollectDataActivity extends AppCompatActivity {//implements SurfaceHolder.Callback

    private static final String TAG = "CollectDataActivity";
    private Boolean recordStart = false;
    //布局控件
    private final int READ_DATA = 1;
//    private TextView tvGps_status;
    private TextView tvContent;
    private TextView tvPath;
    private TextView wifiContent;
    private TextView wifiPath;

    private SensorService sensorService;
    private GPSService gpsService;

    //参数
//    private String longitude;
//    private String latitude;
//    private String speed;
//    private String bearing;
//    private String gpsTime;
//    private float[] angle = new float[3];
    private float[] acc = new float[3];
    private float[] mag = new float[3];
    private float[] gyr = new float[3];
    //    private float pre;
    private float[] gra = new float[3];
    private float[] l_acc = new float[3];
    private float[] rot = new float[4];
    private float[] g_rot = new float[3];
    private float ori;

    //采样间隔
    private static int COLLECT_INTERVAL = 20;

    //准备模型输入
    Module module;
    int i = 0;
    int second = 0;
    Tensor mask = Tensor.fromBlob(new float[]{1.0F}, new long[]{1,1});
    private float spd = 0.0f;
    private double spd_flow = MainActivity.spd_flow;
    private double curr_angle = -100000;
    private float[] acc_set = new float[150];
    private float[] gyr_set = new float[150];
    private float[] gyr_z = new float[50];
    long[] shape = {1, 150, 1};
    private boolean showGPS = MainActivity.showGPS;

    //文件保存路径
    private static final String path = Environment.getExternalStorageDirectory().getPath() + "/DataCollect/data_gps/";
    private String wifi_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DataCollect/data_wifi/";
    private String filePath;
    private String wifi_filePath;
    //    private String videoName;
    private SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault());
    //private SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    //private FileOperation fileOperation;
    private WifiManager wifiManager = null;    //Wifi管理器
    private IntentFilter mWifiStateFilter;
    Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
        @Override
        public int compare(ScanResult lhs, ScanResult rhs) {
            return (lhs.level > rhs.level ? -1 : (lhs.level == rhs.level ? 0 : 1));
        }
    };

    private String wifi_fileName;
    private List<LatLng> latLngs = new ArrayList<LatLng>();
    private List<LatLng> pre_latLngs = new ArrayList<LatLng>();
    Polyline polyline;
    Polyline pre_polyline;
    /** 地球半径 **/
    private static final double earthR = 6371e3;
    /** 180° **/
    private static final DecimalFormat df = new DecimalFormat("0.000000");

    private AMap aMap;
    MyLocationStyle myLocationStyle;
    public AMapLocationClient mLocationClient=null;

    Step stepCounter;
    int step = 0;
    private boolean moveCamera = true;

    public AMapLocationListener mapLocationListener=new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if(aMapLocation!=null)
            {
                if(aMapLocation.getErrorCode()==0)
                {
                    double latitude=aMapLocation.getLatitude();
                    double Longitude=aMapLocation.getLongitude();
                    String text="经度: "+Longitude+"\n"
                            +"纬度: "+latitude+"\n";
                    if(recordStart)
                        latLngs.add(new LatLng(latitude, Longitude));
                    if(moveCamera){
                        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(latitude, Longitude),18,0,0));
                        aMap.moveCamera(mCameraUpdate);
                        moveCamera = false;
                    }
                    System.out.println(text);
                }
                else
                {
                    Log.e("AmapError","location Error, ErrCode:"
                            + aMapLocation.getErrorCode() + ", errInfo:"
                            + aMapLocation.getErrorInfo());
                }
            }
        }
    };
    public AMapLocationClientOption mLocationOption=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_data);
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Button start = findViewById(R.id.CD_start);
        Button stop = findViewById(R.id.CD_stop);
//        Button setInterval = findViewById(R.id.CD_setInterval);
//        final EditText etInterval = findViewById(R.id.CD_Interval);
//        tvGps_status = findViewById(R.id.CD_GPS_status);

//        tvContent = findViewById(R.id.CD_content);
//        tvPath = findViewById(R.id.CD_Path);
//        wifiPath = findViewById(R.id.Wifi_Path);
//        wifiContent = findViewById(R.id.Wifi_content);
        // 获得WifiManager
        Context context = getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiStateFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mWifiStateReceiver, mWifiStateFilter);

        sensorService = new SensorService(CollectDataActivity.this);
        gpsService = new GPSService(CollectDataActivity.this, this);

        MapView mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();

        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(1000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.setMyLocationEnabled(true);
        aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        aMap.getUiSettings().setMyLocationButtonEnabled(true);

        try {
            module = Module.load(assetFilePath(this,"11spdTransEnc_weight_flat_seqLen1_None_ftrHz50.pt"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            mLocationClient = new AMapLocationClient(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mLocationClient.setLocationListener(mapLocationListener);
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mLocationClient.startLocation();


        //预测指针
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.pic2));
        final Marker marker = aMap.addMarker(new MarkerOptions().position(new LatLng(0,0)).icon(bitmapDescriptor));
        marker.setAnchor(0.5f,0.5f);

        ExecutorService ex  = Executors.newSingleThreadExecutor();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date = new Date();
                final String fileName = df1.format(date) + ".csv";
                File file = FileOperation.makeFilePath(path, fileName);
                filePath = file.getAbsolutePath();
                String dataFormat = "Sys_time,laccx,y,z,lacc_accu,grax,y,z,gra_accu,gyrx,y,z,gyr_accu,accx,y,z,acc_accu,magx,y,z,mag_accu,ori,rot_x,rot_y,rot_z,rot_s,rot_head_acc,rot_accu,grot_x,grot_y,grot_z,g_rot_s,g_rot_accu," +
                        "lon,lat,speed,bearing,gps_time";
                String[] content = {path,fileName,dataFormat};
                WriteWork writeWork = new WriteWork();
                writeWork.execute(content);
                String show = filePath + "\n" + dataFormat;
//                tvPath.setText(show);
                recordStart = true;
                stepCounter = new Step();
                Toast.makeText(CollectDataActivity.this, "开始追踪", Toast.LENGTH_LONG).show();
                //建立一个线程，用来定时记录数据
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sensorService.registerSensor();
                        int count = 0;
                        while (recordStart) {
                            try {
                                sleep(COLLECT_INTERVAL - 1);
                                Map<String, float[]> sensorValue = sensorService.getValue();
                                l_acc = (float[]) sensorValue.get("lacc");
                                gra = (float[]) sensorValue.get("gra");
                                gyr = (float[]) sensorValue.get("gyr");
                                acc = (float[]) sensorValue.get("acc");
                                mag = (float[]) sensorValue.get("mag");
                                rot = (float[]) sensorValue.get("rot");
                                g_rot = (float[]) sensorValue.get("g_rot");

                                step = stepCounter.stepCounter(acc);

                                float[] ori_temp = (float[]) sensorValue.get("ori");
                                if (ori_temp != null) {
                                    ori = ori_temp[0];
                                }
                                String dataString = System.currentTimeMillis() + ","
                                        + l_acc[0] + "," + l_acc[1] + "," + l_acc[2] + "," + l_acc[3] + ","
                                        + gra[0] + "," + gra[1] + "," + gra[2] + "," + gra[3] + ","
                                        + gyr[0] + "," + gyr[1] + "," + gyr[2] + "," + gyr[3] + ","
                                        + acc[0] + "," + acc[1] + "," + acc[2] + "," + acc[3] + ","
                                        + mag[0] + "," + mag[1] + "," + mag[2] + "," + mag[3] + ","
                                        + ori + ","
                                        + rot[0] + "," + rot[1] + "," + rot[2] + "," + rot[3] + "," + rot[4] + "," + rot[5] + ","
                                        + g_rot[0] + "," + g_rot[1] + "," + g_rot[2] + "," + g_rot[3] + "," + g_rot[4] + ","
                                        + gpsService.getDataString()+ "," + step;
//                                String data = sensorService.getDataString() + gpsService.getDataString();
                                String[] content = {path,fileName,dataString};
                                WriteWork writeWork = new WriteWork();
                                writeWork.execute(content);

                                //predict
//                                float ori_rad = (float) ((2*Math.PI*ori)/360);

                                for(int j = 0; j < 3; j++){
                                    acc_set[i*3+j] = acc[j];
                                    gyr_set[i*3+j] = gyr[j];
                                }
                                gyr_z[i] = gyr[2];
                                i += 1;

                                if(i == 50){
                                    second += 1;
                                    ex.execute(()->{
                                        if(latLngs.size() != 0){
                                            Tensor acc_tensor = Tensor.fromBlob(acc_set, shape);
                                            Tensor gyr_tensor = Tensor.fromBlob(gyr_set, shape);
                                            Tensor spd_tensor = Tensor.fromBlob(new float[]{spd}, new long[]{1,1,1});
                                            Tensor pred = module.forward(IValue.from(acc_tensor),IValue.from(gyr_tensor), IValue.from(spd_tensor), IValue.from(mask)).toTensor();
                                            float[] arr = pred.getDataAsFloatArray();
                                            spd += (arr[0] - spd_flow);
                                            double startLat;
                                            double startLong;

                                            if(pre_latLngs.size() == 0){
                                                startLat = latLngs.get(latLngs.size()-1).latitude;
                                                startLong = latLngs.get(latLngs.size()-1).longitude;
                                            }else{
                                                startLat = pre_latLngs.get(pre_latLngs.size()-1).latitude;
                                                startLong = pre_latLngs.get(pre_latLngs.size()-1).longitude;
                                            }
                                            if(curr_angle == -100000)
                                                curr_angle = ori;
                                            double angle = 0;
                                            for(int i = 0; i < 50; i++){
                                                angle += gyr_z[i] * 0.02;
                                            }
                                            angle = (-1) * angle * 180 / Math.PI;
                                            curr_angle += angle;
                                            System.out.println("==========================="+angle);
                                            Double[] result = calLocationByDistanceAndLocationAndDirection(curr_angle, startLong, startLat,spd);
                                            pre_latLngs.add(new LatLng(result[1], result[0]));
                                            marker.setPosition(new LatLng(result[1], result[0]));
                                            marker.setRotateAngle(360-(float)ori);
                                        }
                                    });
                                    i = 0;
                                    System.out.println(spd);
                                    System.out.println(second);
                                }
                                //end

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (++count == 25) {
                                Message message = new Message();
                                message.what = READ_DATA;
                                handler.sendMessage(message);
                                count = 0;
                            }
                        }
                    }
                }).start();
                wifi_fileName = df1.format(date) + ".csv";
                File wifi_file = FileOperation.makeFilePath(wifi_path, wifi_fileName);
                wifi_filePath = wifi_file.getAbsolutePath();
                String wifi_dataFormat = "bssid,ssid,level,frequency";
                String wifi_show = wifi_filePath + "\n" + wifi_dataFormat;
//                wifiPath.setText(wifi_show);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(recordStart){
                            wifiManager.startScan();
                            try {
                                sleep(2000);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordStart = false;
                sensorService.unregisterSensor();
                Toast.makeText(CollectDataActivity.this, "追踪结束", Toast.LENGTH_LONG).show();
            }
        });
    }

    public static Double[] calLocationByDistanceAndLocationAndDirection(double angle, double startLong,double startLat, double distance){
        Double[] result = new Double[2];
        //将距离转换成经度的计算公式
        double δ = distance/earthR;
        // 转换为radian，否则结果会不正确
        angle = Math.toRadians(angle);
        startLong = Math.toRadians(startLong);
        startLat = Math.toRadians(startLat);
        double lat = Math.asin(Math.sin(startLat)*Math.cos(δ)+Math.cos(startLat)*Math.sin(δ)*Math.cos(angle));
        double lon = startLong + Math.atan2(Math.sin(angle)*Math.sin(δ)*Math.cos(startLat),Math.cos(δ)-Math.sin(startLat)*Math.sin(lat));
        // 转为正常的10进制经纬度
        lon = Math.toDegrees(lon);
        lat = Math.toDegrees(lat);
        result[0] = lon;
        result[1] = lat;
        return result;
    }


    public static String sHA1(Context context){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length()-1);
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case READ_DATA:
                    ArrayList<String> tData = new ArrayList<>();
                    String time = String.valueOf(System.currentTimeMillis());
                    tData.add("time:" + time);
                    tData.add("\n" + "locate:" + gpsService.getDataString());

//                    //获取当前经纬度坐标
//                    try {
//                        mLocationClient=new AMapLocationClient(getApplicationContext());
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    mLocationClient.setLocationListener(mapLocationListener);
//                    mLocationOption=new AMapLocationClientOption();
//                    mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
//                    mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//
//                    if(null!=mLocationClient)
//                    {
//                        mLocationClient.setLocationOption(mLocationOption);
//                        mLocationClient.stopLocation();
//                        mLocationClient.startLocation();
//                    }
//                    //end
                    try{
                        //draw track line
                        if(showGPS){

                            polyline = aMap.addPolyline(new PolylineOptions().
                                    addAll(latLngs)
                                    .width(20)
                                    .color(Color.argb(235, 1, 180, 247)));
                        }

                        pre_polyline = aMap.addPolyline(new PolylineOptions().
                                addAll(pre_latLngs)
                                .width(20)
                                .color(Color.argb(235, 1, 247, 100)));

//                        LatLng latLng = new LatLng(39.906901,116.397972);
//                        final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title("北京").snippet("DefaultMarker"));
                    }catch (Exception e1){
                     Log.e("line error", "can not draw line");
                    }

                    tData.add("\n" + "acc:" + acc[0] + "," + acc[1] + "," + acc[2] + "," + acc[3]);
                    tData.add("\n" + "mag:" + mag[0] + "," + mag[1] + "," + mag[2] + "," + mag[3]);
                    tData.add("\n" + "gyr:" + gyr[0] + "," + gyr[1] + "," + gyr[2] + "," + gyr[3]);
                    tData.add("\n" + "gra:" + gra[0] + "," + gra[1] + "," + gra[2] + "," + gra[3]);
                    tData.add("\n" + "l_acc:" + l_acc[0] + "," + l_acc[1] + "," + l_acc[2] + "," + l_acc[3]);
//                    tData.add("press:" + pre);
                    tData.add("\n" + "orientation:" + ori);
                    tData.add("\n" + "rot:" + rot[0] + "," + rot[1] + "," + rot[2] + "," + rot[3] + "," + rot[4] + "," +rot[5]);
                    tData.add("\n" + "g_rot" + g_rot[0] + "," + g_rot[1] + "," + g_rot[2] + "," + g_rot[3] + "," + g_rot[4]);
                    tData.add("\n" + "Step:" + step);
                    String data = listToString(tData);
//                    String data_save = String.valueOf(System.currentTimeMillis()) + "," + l_acc[0] + "," + l_acc[1] + "," + l_acc[2] + ","
//                            + ori + "," + gra[0] + "," + gra[1] + "," + gra[2] + ","
//                            + gyr[0] + "," + gyr[1] + "," + gyr[2] + ","
//                            + mag[0] + "," + mag[1] + "," + mag[2] + ","
//                            +longitude + "," +latitude + "," +speed;
                    //Log.i(TAG,data);
//                    tvContent.setText(data);
                    //Log.i(TAG,"1:"+String.valueOf(System.currentTimeMillis()));
//                    String[] content = {data_save, filePath};
//                    WriteWork writeWork = new WriteWork();
//                    writeWork.execute(content);
                    //fileOperation.write(data);
                    //Log.i(TAG,"2:"+String.valueOf(System.currentTimeMillis()));
            }
        }
    };

    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)){
                if(recordStart){
//                    Toast.makeText(CollectDataActivity.this, "Wifi记录开始", Toast.LENGTH_LONG).show();
                    List<ScanResult> results = ScanWifiInfo();
                    wifiSaveToFile(results);
                }
            }else {
                Log.e("WiFi", "Wifi didn't update.");
            }
        }
    };


    private void wifiSaveToFile(List<ScanResult> results){
        File file = FileOperation.makeFilePath(wifi_path, wifi_fileName);
        filePath = file.getAbsolutePath();
        String dataString = "";
        dataString = String.valueOf(System.currentTimeMillis());
//        String[] timeStamp = {wifi_path,wifi_fileName,dataString};
//        WriteWifiWork writeWork = new WriteWifiWork();
//        writeWork.execute(timeStamp);
        try {
            for (ScanResult result : results){
                dataString += ",";
                dataString += "["+result.BSSID+";"+result.SSID+";"+result.level+";"+result.frequency+"]";
            }
            String[] data = {wifi_path,wifi_fileName,dataString};
            WriteWork writeData = new WriteWork();
            writeData.execute(data);
            Context context = getApplicationContext();
//            Toast toast = Toast.makeText(context, "记录成功",Toast.LENGTH_SHORT);
//            toast.show();
//            dataString = "end"+"\n";
//            String[] footer = {wifi_path,wifi_fileName,dataString};
//            WriteWifiWork writeData = new WriteWifiWork();
//            writeData.execute(footer);
        }
        catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            Context context = getApplicationContext();
//            Toast toast = Toast.makeText(context, "记录失败", Toast.LENGTH_SHORT);
//            toast.show();
        }
    }

    private List<ScanResult> ScanWifiInfo(){
        StringBuilder scanBuilder= new StringBuilder();
        scanBuilder.append("time: "+System.currentTimeMillis()+"\n");
        List<ScanResult> scanResults=wifiManager.getScanResults();//搜索到的设备列表
        int count = 0;
        for (ScanResult scanResult : scanResults) {
            scanBuilder.append(String.format("%-20s",scanResult.BSSID)+
                    ","+String.format("%-20s",scanResult.SSID)+","+scanResult.level+"\n");
            count++;
            if(count >= 10)break;
        }
//        wifiContent.setText(scanBuilder);
        return scanResults;
    }


    private static String listToString(List<String> list) {
        if (list == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean first = true;
        //第一个前面不拼接","
        for (String string : list) {
            if (first) {
                first = false;
            } else {
                result.append(";");
            }
            result.append(string);
        }
        return result.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorService.registerSensor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        releaseMediaRecorder();
//        releaseCamera();
        gpsService.removeUpdates();
        sensorService.unregisterSensor();
    }

    private static class WriteWork extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            FileOperation fileOperation = new FileOperation(strings[0],strings[1]);
            fileOperation.writeCsv(strings[2]);
            return null;
        }
    }

    private static class WriteWifiWork extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            FileOperation fileOperation = new FileOperation(strings[0],strings[1]);
            fileOperation.write(strings[2]);
            return null;
        }
    }
}