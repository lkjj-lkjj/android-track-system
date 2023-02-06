package com.example.xiao.test1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CollectPath extends AppCompatActivity {
    private static final String TAG = "SensorTest";


    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetic;
    private Sensor mGyroscope;
    private Sensor mPressure;
    private Sensor mGravity;
    private Sensor mLinear_accelerometer;
    private Sensor mOrientation;

    private SensorListener mSensorListener;

    private long timestamp;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] angle = new float[3];

    private float[] acc = new float[3];
    private float[] mag = new float[3];
    private float[] gyr = new float[3];
    private float pre;
    private float[] gra = new float[3];
    private float[] l_acc = new float[3];
    private float[] ori = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_path);

        Button start = findViewById(R.id.start_collect);
        Button stop = findViewById(R.id.stop_collect);


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // 初始化传感器
        mSensorListener = new SensorListener();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinear_accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册传感器监听函数
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorListener, mMagnetic, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorListener, mPressure, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorListener, mGravity, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorListener, mLinear_accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorListener, mOrientation, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 注销监听函数
        mSensorManager.unregisterListener(mSensorListener);
    }

    private class SensorListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    // 读取加速度传感器数值，values数组0,1,2分别对应x,y,z轴的加速度
                    acc[0] = event.values[0];
                    acc[1] = event.values[1];
                    acc[2] = event.values[2];
                    String accelerometer = "重力加速度传感器(m/s^2)\n" + "x:"
                            + (acc[0]) + "\n" + "y:"
                            + (acc[1]) + "\n" + "z:"
                            + (acc[2]);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mag[0] = event.values[0];
                    mag[1] = event.values[1];
                    mag[2] = event.values[2];
                    String magnetic = "磁场传感器(uT,micro-Tesla)\n" + "x:"
                            + (mag[0]) + "\n" + "y:"
                            + (mag[1]) + "\n" + "z:"
                            + (mag[2]);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (timestamp != 0) {
                        final float dT = (event.timestamp - timestamp) * NS2S;

                        angle[0] += event.values[0] * dT;
                        angle[1] += event.values[1] * dT;
                        angle[2] += event.values[2] * dT;

                        gyr[0] = (float) Math.toDegrees(angle[0]);
                        gyr[1] = (float) Math.toDegrees(angle[1]);
                        gyr[2] = (float) Math.toDegrees(angle[2]);
                    }
                    String gyroscope = "陀螺仪传感器(度)\n" + "x:"
                            + (gyr[0]) + "\n" + "y:"
                            + (gyr[1]) + "\n" + "z:"
                            + (gyr[2]);
                    timestamp = event.timestamp;
                    break;
                case Sensor.TYPE_PRESSURE:
                    pre = event.values[0];
                    String pressure = "压力传感器（hPa）\n" + pre;
                    break;
                case Sensor.TYPE_GRAVITY:
                    gra[0] = event.values[0];
                    gra[1] = event.values[1];
                    gra[2] = event.values[2];
                    String gravity = "重力传感器(m/s^2)\n" + "x:"
                            + (gra[0]) + "\n" + "y:"
                            + (gra[1]) + "\n" + "z:"
                            + (gra[2]);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    l_acc[0] = event.values[0];
                    l_acc[1] = event.values[1];
                    l_acc[2] = event.values[2];
                    String linear_acceleration = "线性加速度传感器(m/s^2)\n" + "x:"
                            + ((int) l_acc[0]) + "\n" + "y:"
                            + ((int) l_acc[1]) + "\n" + "z:"
                            + ((int) l_acc[2]);
                    break;
                case Sensor.TYPE_ORIENTATION:
//                    ori[0] = event.values[0];
//                    ori[1] = event.values[1];
//                    ori[2] = event.values[2];
//                    String orientation = "方向传感器(度)\n" + "x:"
//                            + (mag[0]) + "\n" + "y:"
//                            + (mag[1]) + "\n" + "z:"
//                            + (mag[2]);
                    ori[0] = calculateOrientation();
                    String orientation = "方向传感器(度)\n" + ori[0];
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(TAG, "onAccuracyChanged");
        }

    }

    private float calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[16];
        SensorManager.getRotationMatrix(R, null, acc, mag);
        SensorManager.getOrientation(R, values);
        // 要经过一次数据格式的转换，转换为度
        values[0] = (float) Math.toDegrees(values[0]);
        if (values[0] < 0)
            values[0] += 360;
        return values[0];
    }



}