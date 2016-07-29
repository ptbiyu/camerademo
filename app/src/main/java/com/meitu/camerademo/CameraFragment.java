package com.meitu.camerademo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.meitu.camera.CameraSize;
import com.meitu.camera.filter.FilterCameraFragment;
import com.meitu.camera.model.CameraConfig;
import com.meitu.camera.model.CameraModel;
import com.meitu.camera.model.CameraProcess;
import com.meitu.camera.model.CameraSetting;
import com.meitu.camera.ui.FaceView;
import com.meitu.camerademo.face.FaceDectectFunction;
import com.meitu.camerademo.face.IFaceDectectFunction;
import com.meitu.realtime.param.EffectParam;
import com.meitu.realtime.param.FilterParamater;
import com.meitu.realtime.param.OnlineMaterialsParam;
import com.meitu.realtime.parse.OnlineEffectParser;
import com.meitu.realtime.util.MTFilterOperation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by zby on 2016/7/18.
 */
public class CameraFragment extends FilterCameraFragment implements View.OnClickListener {

    private static final int CAMERA_RATIO_1_1 = 1;
    private static final int CAMERA_RATIO_4_3 = 2;
    private static final int CAMERA_RATIO_FULL = 3;


    private ImageView mIvBack, mIvFlash, mIvCameraSwitch, mIvCameraLevel, mIvCameraFilter,mIvCameraRationChange;

    private FaceView mFaceView;

    private CommonCameraProcess mCommonCamearProcess;

    private Activity mActivity;

    private String mCurFlashMode = Camera.Parameters.FLASH_MODE_OFF;

    private FilterParamater mFilterparameter;

    private EffectParam mEffectParam;

    /**
     * 底层滤镜列表
     */
    private ArrayList<OnlineMaterialsParam> mOnlineMaterialsParams;

    private int[] mFliterIds = {538, 118, 363};
    private int mCurrentFilterIndex = -1;
    private int mCurrentFilterId;

    private int mCurrentBeautyLevel = -1;

    private FaceDectectFunction mFaceDectectFunction;

    private int mCurrentRatio = CAMERA_RATIO_4_3;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        findView(view);
        loadOnlineMaterialsParams();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        mFaceDectectFunction = new FaceDectectFunction(mActivity, mFaceView, new IFaceDectectFunction() {
            @Override
            public int getOrientation() {
                return CameraFragment.this.getOrientation();
            }
        });
        mFaceDectectFunction.startFaceDectect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFaceDectectFunction.stopFaceDectect();
    }

    private void findView(View view) {
        mIvBack = (ImageView) view.findViewById(R.id.iv_camera_back);
        mIvBack.setOnClickListener(this);

        mIvFlash = (ImageView) view.findViewById(R.id.iv_camera_flash);
        mIvFlash.setOnClickListener(this);

        mIvCameraSwitch = (ImageView) view.findViewById(R.id.iv_switch_camera);
        mIvCameraSwitch.setOnClickListener(this);

        mIvCameraLevel = (ImageView) view.findViewById(R.id.iv_camera_beauty_level);
        mIvCameraLevel.setOnClickListener(this);

        mIvCameraFilter = (ImageView) view.findViewById(R.id.iv_camera_filters);
        mIvCameraFilter.setOnClickListener(this);

        mFaceView = (FaceView) view.findViewById(R.id.camera_faceview);
        mFaceView.initFaceDrawable(R.drawable.face_rect,R.drawable.face_rect);

        mIvCameraRationChange = (ImageView) view.findViewById(R.id.iv_switch_picture_ratio);
        mIvCameraRationChange.setOnClickListener(this);
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
        mCameraConfig.mPreviewLayout = CameraConfig.PREVIEW_LAYOUT.INSIDE;// 设置预览模式
        mCameraConfig.canStartPreviewInJpegCallback = false;// 拍照后，如果要继续预览，设置为true
        mCameraConfig.isNeedAutoFocusBeforeTakePicture = true;// 拍照的时候是否需要自动对焦后再拍照
        mCameraConfig.isPreviewSizesOderByAsc = false;// 预览尺寸优先选最小的
        mCommonCamearProcess = new CommonCameraProcess();
        mCameraConfig.mCameraProcess = mCommonCamearProcess;
        return mCameraConfig;
    }

    @Override
    protected void onFilterPictureTaken(byte[] bytes, int i, int i1) {

    }

    @Override
    public com.meitu.realtime.param.EffectParam initEffectParam() {
        // 初始化时没有任何效果
        mFilterparameter = new FilterParamater();
        mEffectParam =
            new EffectParam(0, 0, new MTFilterOperation(false, false, false),
                EffectParam.RealFilterTargetType.MT_TAKE_PHOTO, 0.8f);
        return mEffectParam;
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
            case R.id.iv_camera_beauty_level:
                setCameraBeautyLevel();
                break;
            case R.id.iv_camera_filters:
                setCameraBeautyFilter();
                break;
            case R.id.iv_switch_picture_ratio:
                changeCameraRatio();
                break;
        }
    }

    /**
     * 设置美颜滤镜
     */
    private void setCameraBeautyFilter() {
        mCurrentFilterId = mFliterIds[(++mCurrentFilterIndex) % mFliterIds.length];
        mEffectParam =
            new EffectParam(getOnlineMaterialsParam(), new MTFilterOperation(true, true, true),
                EffectParam.RealFilterTargetType.MT_TAKE_PHOTO);

        //使用这个方法切换滤镜没有暗角和虚化效果
      /*  mEffectParam =
                new EffectParam(mCurrentFilterId,0, new MTFilterOperation(true, true, true),
                        EffectParam.RealFilterTargetType.MT_TAKE_PHOTO,1f);*/
        changeFilter(mEffectParam);
    }

    /**
     * 设置美颜等级
     */
    private void setCameraBeautyLevel() {
        mCurrentBeautyLevel = (mCurrentBeautyLevel + 1) % 7;
        Toast.makeText(mActivity, "Beauty Level:" + (mCurrentBeautyLevel + 1), Toast.LENGTH_LONG).show();
        mEffectParam =
            new EffectParam(0, 0, new MTFilterOperation(true, false, false),
                EffectParam.RealFilterTargetType.MT_TAKE_PHOTO, 0.8f);
        changeFilter(mEffectParam);
        mFilterparameter.int_value = mCurrentBeautyLevel;
        changeFilterParamater(mFilterparameter);
    }

    /**
     * 切换闪光灯
     */
    private void changeFlashMode() {
        switch (mCurFlashMode) {
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

    /**
     * 切换照相机预览比例
     */
    private void changeCameraRatio() {
        switch (mCurrentRatio){
            case CAMERA_RATIO_1_1:
                mCurrentRatio = CAMERA_RATIO_FULL;
                mIvCameraRationChange.setImageResource(R.drawable.camera_picture_ratio_full_iv_ic_sel);
                break;
            case CAMERA_RATIO_4_3:
                mIvCameraRationChange.setImageResource(R.drawable.camera_picture_ratio_11_iv_ic_sel);
                mCurrentRatio = CAMERA_RATIO_1_1;
                break;
            case CAMERA_RATIO_FULL:
                mIvCameraRationChange.setImageResource(R.drawable.camera_picture_ratio_43_iv_ic_sel);
                mCurrentRatio = CAMERA_RATIO_4_3;
                break;
        }
    }

    /**
     * 加载滤镜列表
     */
    private void loadOnlineMaterialsParams() {

        InputStream inputStream = null;
        try {
            inputStream = mActivity.getAssets().open("style/filter/realfilter.plist");
            mOnlineMaterialsParams = OnlineEffectParser.parseOnlineFilterConfigArray(inputStream, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取素材配置信息
     *
     * @return
     */
    private OnlineMaterialsParam getOnlineMaterialsParam() {
        OnlineMaterialsParam onlineMaterialsParam = null;

        for (OnlineMaterialsParam param : mOnlineMaterialsParams) {
            if (param.getFilterid() == mCurrentFilterId) {
                onlineMaterialsParam = param;
                break;
            }
        }
        return onlineMaterialsParam;
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
              mFaceDectectFunction.drain(bytes);
        }

        @Override
        public void onCameraOpenSucess() {
            mFaceDectectFunction.initParam(CameraSetting.getOptimalCameraPreviewSize(isFrontCameraOpen()), isBackCameraOpen());
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
