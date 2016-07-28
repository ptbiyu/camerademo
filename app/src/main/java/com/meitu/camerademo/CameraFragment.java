package com.meitu.camerademo;


import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;

import com.meitu.camera.CameraSize;
import com.meitu.camera.filter.FilterCameraFragment;
import com.meitu.camera.model.CameraConfig;
import com.meitu.camera.model.CameraModel;
import com.meitu.camera.model.CameraProcess;
import com.meitu.realtime.param.EffectParam;
import com.meitu.realtime.util.MTFilterOperation;

import java.util.ArrayList;

/**
 * Created by zby on 2016/7/18.
 */
public class CameraFragment extends FilterCameraFragment implements View.OnClickListener {


    private ImageView mIvBack, mIvFlash, mIvCameraSwitch;

    private CommonCameraProcess mCommonCamearProcess;

    private Activity mActivity;

    private String mCurFlashMode = Camera.Parameters.FLASH_MODE_OFF;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        findView(view);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    private void findView(View view) {
        mIvBack = (ImageView) view.findViewById(R.id.iv_camera_back);
        mIvBack.setOnClickListener(this);

        mIvFlash = (ImageView) view.findViewById(R.id.iv_camera_flash);
        mIvFlash.setOnClickListener(this);

        mIvCameraSwitch = (ImageView) view.findViewById(R.id.iv_switch_camera);
        mIvCameraSwitch.setOnClickListener(this);
    }

    @Override
    protected CameraModel initFilterCameraModel() {
        return new CameraModel();
    }

    @Override
    protected CameraConfig initFilterCameraConfig() {
        mCameraConfig = new CameraConfig();
        mCameraConfig.mPreviewMode = CameraConfig.PREVIEW_MODE.GL_SURFACE_VIEW;
        mCameraConfig.mPreviewFrameLayoutResId = R.id.camera_previewframe_layout;
        mCameraConfig.mFocusLayoutResId = R.id.camera_focous_layout;
        mCameraConfig.mFaceLayoutResId = R.id.camera_faceview;
        mCameraConfig.mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
        mCameraConfig.isDefaultStartFrontCamera = false;
        mCameraConfig.mPreviewLayout = CameraConfig.PREVIEW_LAYOUT.INSIDE;//设置预览模式
        mCameraConfig.canStartPreviewInJpegCallback = false;//拍照后，如果要继续预览，设置为true
        mCameraConfig.isNeedAutoFocusBeforeTakePicture = true;//拍照的时候是否需要自动对焦后再拍照
        mCameraConfig.isPreviewSizesOderByAsc = false;//预览尺寸优先选最小的
        mCommonCamearProcess = new CommonCameraProcess();
        mCameraConfig.mCameraProcess = mCommonCamearProcess;
        return mCameraConfig;
    }

    @Override
    protected void onFilterPictureTaken(byte[] bytes, int i, int i1) {

    }

    @Override
    public com.meitu.realtime.param.EffectParam initEffectParam() {
        EffectParam effectParam = new EffectParam(538, 0, new MTFilterOperation(true, false, false), EffectParam.RealFilterTargetType.MT_TAKE_PHOTO);
        return effectParam;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_camera_back:
                mActivity.finish();
                break;
            case R.id.iv_camera_flash:
                changeFlashMode();
                break;
            case R.id.iv_switch_camera:
                switchCamera();
                break;
        }
    }

    private void changeFlashMode() {
        switch (mCurFlashMode){
            case Camera.Parameters.FLASH_MODE_OFF:
                mIvFlash.setImageResource(R.drawable.camera_flash_auto_iv_ic_sel);
                mCurFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
                break;
            case Camera.Parameters.FLASH_MODE_AUTO:
                mIvFlash.setImageResource(R.drawable.camera_flash_on_iv_ic_sel);
                mCurFlashMode = Camera.Parameters.FLASH_MODE_ON;
                break;
            case Camera.Parameters.FLASH_MODE_ON:
                mIvFlash.setImageResource(R.drawable.camera_flash_light_iv_ic_sel);
                mCurFlashMode = Camera.Parameters.FLASH_MODE_TORCH;
                break;
            case Camera.Parameters.FLASH_MODE_TORCH:
                mIvFlash.setImageResource(R.drawable.camera_flash_off_iv_ic_sel);
                mCurFlashMode = Camera.Parameters.FLASH_MODE_OFF;
                break;
        }
        switchFlash(mCurFlashMode);
    }


    private class CommonCameraProcess implements CameraProcess {
        @Override
        public void beforeOpenCamera() {

        }

        @Override
        public void afterOpenCamera() {

        }

        @Override
        public void beforeCloseCamera() {

        }

        @Override
        public void afterCloseCamera() {

        }

        @Override
        public void beforeStartPreview() {

        }

        @Override
        public void afterStartPreview() {

        }

        @Override
        public void beforeStopPreview() {

        }

        @Override
        public void afterStopPreview() {

        }

        @Override
        public CameraSize settingPictureSize(ArrayList<CameraSize> arrayList) {
            return null;
        }

        @Override
        public CameraSize settingPreviewSize(ArrayList<CameraSize> arrayList, CameraSize cameraSize) {
            return null;
        }

        @Override
        public void onPreviewFrame(byte[] bytes) {

        }

        @Override
        public void onCameraOpenSucess() {

        }

        @Override
        public void onCameraOpenFail() {

        }

        @Override
        public void onTakePictureFail() {

        }

        @Override
        public void onPreviewFrameLayoutChange(int i, int i1) {

        }
    }
}
