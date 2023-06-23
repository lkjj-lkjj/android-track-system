package com.example.xiao.test1;

import static com.example.xiao.test1.MapShow.assetFilePath;
import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
import com.example.xiao.test1.utils.PathData;
import com.example.xiao.test1.utils.Step;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CollectDataActivity extends AppCompatActivity {//implements SurfaceHolder.Callback

    private static final String TAG = "CollectDataActivity";
    private Boolean recordStart = false;
    //布局控件
    private final int READ_DATA = 1;
    private SensorService sensorService;
    private GPSService gpsService;
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
    private int differ;

    //准备模型输入
    Module module;
    int i = 0;
    int second = 0;
    Tensor mask = Tensor.fromBlob(new float[]{1.0F}, new long[]{1,1});
    private float spd = 0.0f;
    private double curr_angle = -100000;
    private float[] acc_set = new float[150];
    private float[] gyr_set = new float[150];
    private float[] gyr_z = new float[50];
    long[] shape = {1, 150, 1};

    //文件保存路径
    private static final String path = Environment.getExternalStorageDirectory().getPath() + "/DataCollect/data_gps/";
    private String filePath;
    private SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault());
    private List<LatLng> latLngs = new ArrayList<LatLng>();
    private List<LatLng> pre_latLngs = new ArrayList<LatLng>();
    private List<LatLng> track_latLngs = new ArrayList<LatLng>();
    private List<LatLng> modified_latLng = new ArrayList<LatLng>();
    Polyline polyline;
    Polyline pre_polyline;
    Polyline track_polyline;
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
    private short drawTrackPath = 0;
    private SensorEventHelper mSensorHelper;

    private Marker pointMarker;
    private boolean myPointShow = true;
    private int oldStep = 0;
    private int stepStayCount = 0;

    private TextView showDistance;
    private double distance;

    private List<LatLng> angelLatLngs = new ArrayList<>();

    private List<Marker> markerList = new ArrayList<>();


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
                    if(recordStart){
                        latLngs.add(new LatLng(latitude, Longitude));
                    }
                    if(drawTrackPath == 1){
                        track_latLngs.clear();
                        PathData end = MainActivity.TRACK_PATH_DATA.get(MainActivity.TRACK_PATH_DATA.size()-1);
                        System.out.println(end);
                        for(PathData item : MainActivity.TRACK_PATH_DATA){
                            track_latLngs.add(new LatLng(latitude+item.latLng.latitude-end.latLng.latitude,Longitude+item.latLng.longitude-end.latLng.longitude));
                        }
                        track_polyline = aMap.addPolyline(new PolylineOptions().
                                addAll(track_latLngs)
                                .width(20)
                                .color(Color.argb(235, 100, 100, 100)));
                        pointMarker = aMap.addMarker(new MarkerOptions().position(track_latLngs.get(0)).title("终点"));
                        putStairMarker();
                        drawTrackPath = 2;
                    }
                    if(moveCamera){
                        CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(latitude, Longitude),18,0,0));
                        aMap.moveCamera(mCameraUpdate);
                        moveCamera = false;
                    }
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

    public void putStairMarker(){
            StringBuilder stepInfo = new StringBuilder();
            StringBuilder stageInfo = new StringBuilder();
            MarkerOptions markerOptions = new MarkerOptions();
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.stair);
            int targetWidth = 100; // 目标宽度
            int targetHeight = 100; // 目标高度
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

            for(int i = 0; i < MainActivity.TRACK_PATH_DATA.size(); i++){
                if(MainActivity.TRACK_PATH_DATA.get(i).stair != 0){
                    stepInfo.append(MainActivity.TRACK_PATH_DATA.get(i).steps).append((MainActivity.TRACK_PATH_DATA.get(i).stair ==2)? "(下)" : "(上)").append(",");
                }
                if(MainActivity.TRACK_PATH_DATA.get(i).stage != 0){
                    stageInfo.append(MainActivity.TRACK_PATH_DATA.get(i).stage);
                    markerOptions.position(track_latLngs.get(i-6));
                    markerOptions.title("前方楼梯");
                    markerOptions.snippet("楼梯数："+stageInfo+"\n"+"预测阶数："+stepInfo);
                    Marker marker = aMap.addMarker(markerOptions);
                    markerList.add(marker);

                    stepInfo = new StringBuilder();
                    stageInfo = new StringBuilder();
                }
            }
    }

    AMap.OnMarkerClickListener mMarkerListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
            return true; // 返回:true 表示点击marker 后marker 不会移动到地图中心；返回false 表示点击marker 后marker 会自动移动到地图中心
        }
    };

    public void LineCorrection(int second){
        if(second-1 < track_latLngs.size()){
            modified_latLng.clear();
            double k = calAngle(track_latLngs.get(track_latLngs.size()-1),track_latLngs.get(track_latLngs.size()-second+differ),pre_latLngs.get(second-differ-1));
            for(LatLng item: track_latLngs){
                revolve(track_latLngs.get(track_latLngs.size()-1), item, k);
            }
        }else{
            modified_latLng.clear();
            double k = calAngle(track_latLngs.get(track_latLngs.size()-1),track_latLngs.get(0),pre_latLngs.get(second-differ-1));
            for(LatLng item: track_latLngs){
                revolve(track_latLngs.get(track_latLngs.size()-1), item, k);
            }
        }
        track_latLngs.clear();
        track_latLngs.addAll(modified_latLng);
        track_polyline.remove();
        track_polyline = aMap.addPolyline(new PolylineOptions().
                addAll(modified_latLng)
                .width(20)
                .color(Color.argb(235, 100, 100, 100)));

        pointMarker.remove();
        ClearAllPoint();
        putStairMarker();
        pointMarker = aMap.addMarker(new MarkerOptions().position(track_latLngs.get(0)).title("终点"));

    }
    public void ClearAllPoint(){
        for(Marker item: markerList){
            item.remove();
        }
    }
    public void LineCorrection(boolean left){
        modified_latLng.clear();
        for(LatLng item: track_latLngs){
            revolve(track_latLngs.get(track_latLngs.size()-1), item, left ? 1:-1);
        }
        track_latLngs.clear();
        track_latLngs.addAll(modified_latLng);
        track_polyline.remove();
        track_polyline = aMap.addPolyline(new PolylineOptions().
                addAll(modified_latLng)
                .width(20)
                .color(Color.argb(235, 100, 100, 100)));
        pointMarker.remove();
        ClearAllPoint();
        putStairMarker();
        pointMarker = aMap.addMarker(new MarkerOptions().position(track_latLngs.get(0)).title("终点"));

    }

    public double calAngle(LatLng p1, LatLng p2, LatLng p3)
    {
        double a1 = Math.atan2((p2.longitude - p1.longitude), (p2.latitude - p1.latitude)) * 180 / Math.PI;
        double a2 = Math.atan2((p3.longitude - p1.longitude), (p3.latitude - p1.latitude)) * 180 / Math.PI;
        return a1 - a2;
    }
    public void revolve(LatLng o, LatLng s, double k){
        k = Math.toRadians(k);
        double x2= (s.latitude-o.latitude)*Math.cos(k) +(s.longitude-o.longitude)*Math.sin(k)+o.latitude;
        double y2= -(s.latitude-o.latitude)*Math.sin(k) + (s.longitude-o.longitude)*Math.cos(k)+o.longitude;
        modified_latLng.add(new LatLng(x2,y2));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_data);
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Button start = findViewById(R.id.CD_start);
        Button stop = findViewById(R.id.CD_stop);
        Button turnleft = findViewById(R.id.turnleft);
        Button turnright = findViewById(R.id.turnright);
        Button showGPS = findViewById(R.id.showgps);
        showDistance = findViewById(R.id.textView9);

        sensorService = new SensorService(CollectDataActivity.this);
        gpsService = new GPSService(CollectDataActivity.this, this);

        MapView mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();
        aMap.setOnMarkerClickListener(mMarkerListener);

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
//        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
//        mLocationOption.setInterval(1000);
        //设置定位参数
        if(null != mLocationClient){
            mLocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            //启动定位
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }


        //预测指针
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.pic2));
        final Marker marker = aMap.addMarker(new MarkerOptions().position(new LatLng(0,0)).icon(bitmapDescriptor));
        marker.setAnchor(0.5f,0.5f);
        mSensorHelper = new SensorEventHelper(this);
        mSensorHelper.registerSensorListener();
        mSensorHelper.setCurrentMarker(marker);

        ExecutorService ex  = Executors.newSingleThreadExecutor();

        angelLatLngs.add(new LatLng(0,0));
        angelLatLngs.add(new LatLng(0,0));
        angelLatLngs.add(new LatLng(0,0));

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date = new Date();
                final String fileName = df1.format(date) + ".csv";
                File file = FileOperation.makeFilePath(path, fileName);
                filePath = file.getAbsolutePath();
                String dataFormat = "Sys_time,laccx,y,z,lacc_accu,grax,y,z,gra_accu,gyrx,y,z,gyr_accu,accx,y,z,acc_accu,magx,y,z,mag_accu,ori,rot_x,rot_y,rot_z,rot_s,rot_head_acc,rot_accu,grot_x,grot_y,grot_z,g_rot_s,g_rot_accu," +
                        "lon,lat,speed,bearing,gps_time,step";
                String[] content = {path,fileName,dataFormat};
                WriteWork writeWork = new WriteWork();
                writeWork.execute(content);
                recordStart = true;
                if(drawTrackPath == 0)
                    drawTrackPath = 1;
                stepCounter = new Step();
                Toast.makeText(CollectDataActivity.this, "开始追踪", Toast.LENGTH_LONG).show();
                //建立一个线程，用来定时记录数据
                new Thread(new Runnable() {
                    @SuppressLint("SetTextI18n")
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
                                if(step == oldStep){
                                    stepStayCount += 1;
                                }else{
                                    stepStayCount = 0;
                                    oldStep = step;
                                }
                                if(stepStayCount > 60){
                                    spd = 0;
                                }

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
                                            if(stepStayCount <= 60){
                                                spd += arr[0];//todo
                                            }
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
                                            else{
                                                double angle = 0;
                                                for(int i = 0; i < 50; i++){
                                                    angle += gyr_z[i] * 0.02;
                                                }
                                                angle = (-1) * angle * 180 / Math.PI;
                                                curr_angle += angle;
                                            }
                                            Double[] result = calLocationByDistanceAndLocationAndDirection(curr_angle, startLong, startLat,spd);

                                            LatLng temp1 = angelLatLngs.get(1);
                                            LatLng temp2 = angelLatLngs.get(2);
                                            angelLatLngs.set(0, temp1);
                                            angelLatLngs.set(1, temp2);
                                            angelLatLngs.set(2, new LatLng(result[1], result[0]));
                                            if(angelLatLngs.get(0).latitude != 0){
                                                double angle = calculateAngle(angelLatLngs.get(0), angelLatLngs.get(1), angelLatLngs.get(2));
                                                if(angle > 70){
                                                    spd = 0;
                                                }
                                            }
                                            pre_latLngs.add(new LatLng(result[1], result[0]));
                                            marker.setPosition(new LatLng(result[1], result[0]));
                                            Message message = new Message();
                                            message.what = 2;
                                            handler.sendMessage(message);
                                            distance = distance(result[1], result[0], track_latLngs.get(0).latitude, track_latLngs.get(0).longitude);
                                        }
                                    });
                                    i = 0;
                                    if(pre_latLngs.size() != 0){
                                        differ = second - pre_latLngs.size();
                                        if((second-differ) == 4)
                                            LineCorrection(second);
                                    }
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
        turnright.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LineCorrection(false);
            }
        });
        turnleft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LineCorrection(true);
            }
        });

        showGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPointShow = !myPointShow;
                if(myPointShow){
                    aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
                    aMap.setMyLocationEnabled(true);
//                    aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
                    aMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
                else{
                    aMap.setMyLocationEnabled(false);
                }
            }
        });
    }

    public static double calculateAngle(LatLng latLng1, LatLng latLng2, LatLng latLng3) {

        // 计算向量1
        double x1 = latLng2.latitude - latLng1.latitude;
        double y1 = latLng2.longitude - latLng1.longitude;

        // 计算向量2
        double x2 = latLng3.latitude - latLng1.latitude;
        double y2 = latLng3.longitude - latLng1.longitude;

        // 计算向量1和向量2的点积
        double dotProduct = x1 * x2 + y1 * y2;

        // 计算向量1和向量2的模
        double magnitude1 = Math.sqrt(x1 * x1 + y1 * y1);
        double magnitude2 = Math.sqrt(x2 * x2 + y2 * y2);

        // 计算角度（弧度）
        double angleRad = Math.acos(dotProduct / (magnitude1 * magnitude2));

        // 将弧度转换为角度
        double angleDeg = Math.toDegrees(angleRad);

        return angleDeg;
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

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6378137; // 地球半径，单位为米
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lon1) - Math.toRadians(lon2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * earthRadius;
        s = Math.round(s * 10000) / 10000.0; // 保留4位小数，单位为米
        return s;
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
                case READ_DATA:{
                    ArrayList<String> tData = new ArrayList<>();
                    String time = String.valueOf(System.currentTimeMillis());
                    tData.add("time:" + time);
                    tData.add("\n" + "locate:" + gpsService.getDataString());

                    try{
                        //draw track line
                        if(polyline != null)
                            polyline.remove();
                        if(myPointShow){
                            polyline = aMap.addPolyline(new PolylineOptions().
                                    addAll(latLngs)
                                    .width(20)
                                    .color(Color.argb(235, 1, 180, 247)));
                        }
                        if(pre_polyline != null)
                            pre_polyline.remove();
                        pre_polyline = aMap.addPolyline(new PolylineOptions().
                                addAll(pre_latLngs)
                                .width(20)
                                .color(Color.argb(235, 1, 247, 100)));

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
                }break;
                case 2:{
                    showDistance.setText(String.format("%.2f", distance) + " m");
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        sensorService.registerSensor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
}