package com.meitu.camerademo;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.meitu.core.NativeLibrary;


public class CameraActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //没有初始化美颜实时效果失效
        NativeLibrary.ndkInit(this);
        setContentView(R.layout.activity_camera);
        if (savedInstanceState ==null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.rl_root,new CameraFragment()).commitAllowingStateLoss();
        }
    }
}
