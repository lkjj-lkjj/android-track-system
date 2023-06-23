package com.example.xiao.test1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.amap.api.maps.model.LatLng;
import com.example.xiao.test1.utils.PathData;

import org.pytorch.Module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TrackPath extends AppCompatActivity {

    InputStreamReader is = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_path);


        String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        int PERMISSION_CODE = 123;
        if(ActivityCompat.checkSelfPermission(TrackPath.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(TrackPath.this,PERMISSIONS ,PERMISSION_CODE );
        }

        LinearLayout layout = findViewById(R.id.layout);

        try {
            ArrayList<String> fileNames = getAllDataFileName(Environment.getExternalStorageDirectory().getPath() + "/DataCollect/collect_path/");
            for(String item : fileNames){
                Button button = new Button(this);
                button.setText(item);

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            File f = new File(Environment.getExternalStorageDirectory().getPath() + "/DataCollect/collect_path/" +button.getText());
                            BufferedReader reader = new BufferedReader(new FileReader(f));
                            reader.readLine();
                            String line = null;
                            MainActivity.TRACK_PATH_DATA.clear();
                            while((line = reader.readLine()) != null){
                                String[] latLng = line.split(",");
                                PathData temp = new PathData();
                                temp.latLng = new LatLng(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1]));
                                temp.turn = Integer.parseInt(latLng[2]);
                                temp.stair = Integer.parseInt(latLng[3]);
                                temp.steps = Integer.parseInt(latLng[4]);
                                temp.stage = Integer.parseInt(latLng[5]);
                                MainActivity.TRACK_PATH_DATA.add(temp);
                            }
                            reader.close();
                            Intent intent = new Intent(TrackPath.this, CollectDataActivity.class);
                            startActivity(intent);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                layout.addView(button, 0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public ArrayList<String> getAllDataFileName(String folderPath){
        ArrayList<String> fileList = new ArrayList<>();
        File file = new File(folderPath);
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                String fileName = tempList[i].getName();
                if (fileName.endsWith(".csv")){    //  根据自己的需要进行类型筛选
                    fileList.add(fileName);
                }
            }
        }
        return fileList;
    }
}