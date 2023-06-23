package com.example.xiao.test1;

import static com.example.xiao.test1.MapShow.assetFilePath;
import static java.lang.Thread.sleep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
import com.example.xiao.test1.Service.SensorService;
import com.example.xiao.test1.utils.Step;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CollectPath extends AppCompatActivity {
    private String path = Environment.getExternalStorageDirectory().getPath() + "/DataCollect/collect_path/";
    private SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault());
    private String filePath;
    private boolean recordStart = false;
    private SensorService sensorService;
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
    Module module;
    int i = 0;
    int second = 0;
    Tensor mask = Tensor.fromBlob(new float[]{1.0F}, new long[]{1,1});
    private float spd = 0.0f;
    private double curr_angle = -100000;
    private float[] acc_set = new float[150];
    private float[] gyr_set = new float[150];
    private float[] gyr_z = new float[50];
    private float[] pre = new float[1];
    private List<LatLng> latLngs = new ArrayList<LatLng>();
    private List<LatLng> pre_latLngs = new ArrayList<LatLng>();
    long[] shape = {1, 150, 1};
    /** 地球半径 **/
    private static final double earthR = 6371e3;
    Step stepCounter;
    int step = 0;
    private int oldStep = 0;
    private int stepStayCount = 0;
    private List<Double> accz = new ArrayList<>();

    //检测楼梯相关变量
    private static final double THRESHOLD = 15;

    int stepCount = 0;
    double total = 0;
    boolean isPeak = false;
    int nextStepDiff = 0;
    int beginI = 0;
    int endI = 0;
    int stage = 0;
    int stageDiff = 0;

    int writeStairStep = 0;
    int writeStage = 0;

    boolean onStair = false;

    int updown = 0;
    float beginPre = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_path);

        Button start = findViewById(R.id.start_collect);
        Button stop = findViewById(R.id.stop_collect);
        sensorService = new SensorService(CollectPath.this);
        ExecutorService ex  = Executors.newSingleThreadExecutor();

        try {
            module = Module.load(assetFilePath(this,"11spdTransEnc_weight_flat_seqLen1_None_ftrHz50.pt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        latLngs.add(new LatLng(0,0));
        accz.add(0, (double) 0);
        accz.add(0, (double) 0);
        accz.add(0, (double) 0);


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date = new Date();
                final String fileName = df1.format(date) + ".csv";
                File file = FileOperation.makeFilePath(path, fileName);
                filePath = file.getAbsolutePath();
                String dataFormat = "lat,lng,turn,stair,steps,stage\n0,0,0,0,0,0";
                String[] content = {path,fileName,dataFormat};
                CollectPath.WriteWork writeWork = new CollectPath.WriteWork();
                writeWork.execute(content);
                recordStart = true;
                stepCounter = new Step();
                Toast.makeText(CollectPath.this, "开始记录", Toast.LENGTH_SHORT).show();

                stepCount = 0;
                total = 0;
                isPeak = false;
                nextStepDiff = 0;
                beginI = 0;
                endI = 0;
                stage = 0;
                stageDiff = 0;
                writeStairStep = 0;
                writeStage = 0;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sensorService.registerSensor();
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
                                pre = (float[]) sensorValue.get("pre");

                                step = stepCounter.stepCounter(acc);
                                //检测楼梯和拐角
                                double temp1 = accz.get(1);
                                double temp2 = accz.get(2);
                                accz.set(0, temp1);
                                accz.set(1, temp2);
                                accz.set(2, (double)acc[2]);

                                nextStepDiff++;
                                stageDiff ++;
                                if(stageDiff>=300 && stage>=1){
//                                    System.out.println("阶数："+stage);
                                    writeStage = stage;
                                    stage = 0;
                                }

                                if(nextStepDiff >= 65 && stepCount != 0){
                                    if(stepCount<=3){
//                                        System.out.println("数据差值：" + nextStepDiff);
//                                        System.out.println("剔除步数：" + stepCount+"\n");
                                        total = 0;
                                        stepCount = 0;
                                        nextStepDiff = 0;
                                    }else{
//                                        System.out.println("+++++++++++++ "+beginI);
//                                        System.out.println("数据差值：" + nextStepDiff);
//                                        System.out.println("步数：" + stepCount);
//                                        System.out.println("平均振幅：" +total/stepCount);
//                                        System.out.println("------------- "+endI+"\n");
                                        writeStairStep = stepCount;
                                        if(pre[0] - beginPre >0)
                                            updown = 1;
                                        else
                                            updown = 2;
                                        stepCount = 0;
                                        nextStepDiff = 0;
                                        total = 0;
                                        stage += 1;
                                    }
                                }

                                if (accz.get(1) > accz.get(0) && accz.get(1) > accz.get(2) && accz.get(1) > THRESHOLD) {
                                    if (!isPeak) {
                                        onStair = true;
                                        stageDiff = 0;
                                        nextStepDiff = 0;

                                        endI = i+2;

                                        total += accz.get(1);
                                        stepCount++;
                                        if(stepCount == 1){
                                            beginI = i+2;
                                            beginPre = pre[0];
                                        }
                                        isPeak = true;
                                    }
                                } else {
                                    isPeak = false;
                                }

                                //结束



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
                                            pre_latLngs.add(new LatLng(result[1], result[0]));
                                            //写入文件
                                            String dataString;
                                            if(writeStairStep != 0){
                                                dataString = result[1].toString()+","+result[0].toString()+",0,"+updown+","+writeStairStep+",0";
                                                updown = 0;
                                                writeStairStep = 0;
                                            }else if (writeStage != 0) {
                                                dataString = result[1].toString()+","+result[0].toString()+",0,"+updown+",0,"+writeStage;
                                                updown = 0;
                                                writeStage = 0;
                                            }else{
                                                dataString = result[1].toString()+","+result[0].toString()+",0,0,0,0";
                                            }
                                            String[] content = {path,fileName,dataString};
                                            WriteWork writeWork = new WriteWork();
                                            writeWork.execute(content);
                                        }
                                    });
                                    i = 0;
                                }
                                //end

                            } catch (InterruptedException e) {
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
                Toast.makeText(CollectPath.this, "记录结束", Toast.LENGTH_SHORT).show();
                recordStart = false;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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