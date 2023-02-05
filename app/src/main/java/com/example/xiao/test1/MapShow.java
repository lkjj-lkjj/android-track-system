package com.example.xiao.test1;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MapShow extends AppCompatActivity {
    private Module module = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        try {
            module = Module.load(assetFilePath(this,"11spdTransEnc_weight_flat_seqLen1_None_ftrHz50.pt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        long[] shape = {1, 150, 1};
        float[] array = new float[150];
        for(int i = 0; i< 150; i++){
            array[i] = 0.0F;
        }
        Tensor acc = Tensor.fromBlob(array, shape);
        Tensor gy = Tensor.fromBlob(array, shape);
        Tensor mask = Tensor.fromBlob(new float[]{1.0F}, new long[]{1,1});
        Tensor spd = Tensor.fromBlob(new float[]{3.0F}, new long[]{1,1,1});

        Tensor pred = module.forward(IValue.from(acc),IValue.from(gy), IValue.from(spd), IValue.from(mask)).toTensor();
        float[] arr = pred.getDataAsFloatArray();
        System.out.println(Arrays.toString(arr));


    }
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}