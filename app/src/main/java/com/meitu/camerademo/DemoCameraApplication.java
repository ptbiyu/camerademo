package com.meitu.camerademo;

import com.meitu.camera.CameraApplication;
import com.meitu.library.application.BaseApplication;

/**
 * Created by zby on 2016/7/23.
 */
public class DemoCameraApplication extends BaseApplication{

    @Override
    public void onCreate() {
        super.onCreate();
        CameraApplication.setApplication(this);
    }
}
