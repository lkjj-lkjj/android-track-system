package com.example.xiao.test1.utils;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageData {

    public int[] pixels;
    public int w;
    public int h;

    ImageData() {
    }

    public ImageData(Bitmap bitmap) {
        this.w = bitmap.getWidth();
        this.h = bitmap.getHeight();
        //将bitmap类型转为int数组
        this.pixels = new int[this.w * this.h];
        bitmap.getPixels(this.pixels, 0, this.w, 0, 0, this.w, this.h);
    }

    public Bitmap getBitmap() {
        //int数组转为bitmap类型。
        try {
            Bitmap desImage = Bitmap.createBitmap(this.w, this.h, Bitmap.Config.ARGB_8888);
            desImage.setPixels(this.pixels, 0, this.w, 0, 0, this.w, this.h);
            return desImage;
        } catch (Exception e) {
        }
        return null;
    }

    public ImageData getImageData(Bitmap bitmap) {
        this.w = bitmap.getWidth();
        this.h = bitmap.getHeight();
        this.pixels = new int[w * h];
        bitmap.getPixels(this.pixels, 0, w, 0, 0, w, h);
        return this;
    }

    /**
     * 保存位图到本地
     *
     * @param bitmap
     * @param path   本地路径
     * @return void
     */
    public static void SavaImage(Bitmap bitmap, String path, String name) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            File filePic = new File(path + "/" + name + ".jpg");
            if (!filePic.exists()) {
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {

        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }

        final float[] values = new float[9];
        m.getValues(values);

        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);

        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);


        return bm1;
    }

    /**
     * 将彩色图转换为灰度图
     *
     * @param img 位图
     * @return 返回转换好的位图
     */
    public static Bitmap convertGrayImg(Bitmap img) {
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高

        int[] pixels = new int[width * height]; //通过位图的大小创建像素点数组

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int gray = pixels[width * i + j];

                int red = ((gray & 0x00FF0000) >> 16);
                int green = ((gray & 0x0000FF00) >> 8);
                int blue = (gray & 0x000000FF);

                gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                gray = alpha | (gray << 16) | (gray << 8) | gray;
                pixels[width * i + j] = gray;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }
}
