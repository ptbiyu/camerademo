package com.meitu.camerademo.bean;

import android.graphics.Bitmap;

/**
 * Created by jsg on 2016/3/7.
 */
public class PictureData {
    //拍照模式
    public static byte[] pictureByte = null;
    //截屏数据
    public static Bitmap bitmap = null;
    //exif
    public static int exif = 0;
    //手机旋转角度
    public static int rotation = 0;
    //是否后置摄像头
    public static boolean isBackCameraOpen = false;

    public byte[] getPictureByte() {
        return pictureByte;
    }

    public void setPictureByte(byte[] pictureByte) {
        this.pictureByte = pictureByte;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getExif() {
        return exif;
    }

    public void setExif(int exif) {
        this.exif = exif;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public boolean isBackCameraOpen() {
        return isBackCameraOpen;
    }

    public void setBackCameraOpen(boolean backCameraOpen) {
        isBackCameraOpen = backCameraOpen;
    }
}
