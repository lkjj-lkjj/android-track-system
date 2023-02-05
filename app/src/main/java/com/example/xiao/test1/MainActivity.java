package com.example.xiao.test1;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.MapsInitializer;


public class MainActivity extends AppCompatActivity {

    public static double spd_flow = 0;
    public static boolean showGPS = true;
    private static final int REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";
    private static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button sensorTest = findViewById(R.id.SensorTest);


        sensorTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SensorTestActivity.class);
                startActivity(intent);
            }
        });
        Button gpsTest = findViewById(R.id.GPSTest);
        gpsTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GPSTestActivity.class);
                startActivity(intent);
            }
        });
        Button collectData = findViewById(R.id.CollectData);
        collectData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CollectDataActivity.class);
                startActivity(intent);
            }
        });


        Button mapButton = findViewById(R.id.map_show);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapShow.class);
                startActivity(intent);
            }
        });

        Button button = findViewById(R.id.button);
        button.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText spdtext = findViewById(R.id.input);
                spd_flow = Double.parseDouble(spdtext.getText().toString());
            }
        }));

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(showGPS){
                    showGPS = false;
                    Toast.makeText(MainActivity.this, "GPS轨迹关闭", Toast.LENGTH_SHORT).show();
                }else{
                    showGPS = true;
                    Toast.makeText(MainActivity.this, "GPS轨迹打开", Toast.LENGTH_SHORT).show();
                }
            }
        }));

        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果 API level 是大于等于 23(Android 6.0) 时

            //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,
                    Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                //申请权限
                ActivityCompat.requestPermissions(
                        this,
                        PERMISSIONS,
                        REQUEST_CODE
                );
                Toast.makeText(this, "正在授权！", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
                Log.i("FileOperation", "checkPermission: 已经授权！");
            }
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i(TAG,"onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Log.i(TAG,"onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Log.i(TAG,"onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.i(TAG,"onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.i(TAG,"onResume");
    }
}
