package com.meitu.camerademo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.meitu.camera.CameraSize;
import com.meitu.camera.event.PreviewFrameLayoutEvent;
import com.meitu.camera.event.RequestLayoutCameraPreviewEvent;
import com.meitu.camera.filter.FilterCameraFragment;
import com.meitu.camera.model.CameraConfig;
import com.meitu.camera.model.CameraModel;
import com.meitu.camera.model.CameraProcess;
import com.meitu.camera.ui.FaceView;
import com.meitu.camera.ui.PreviewFrameLayout;
import com.meitu.camera.util.CameraUtil;
import com.meitu.camera.util.ExifUtil;
import com.meitu.camerademo.bean.PictureData;
import com.meitu.camerademo.face.FaceDectectFunction;
import com.meitu.camerademo.face.IFaceDectectFunction;
import com.meitu.core.types.NativeBitmap;
import com.meitu.core.util.CacheUtil;
import com.meitu.library.util.bitmap.BitmapUtils;
import com.meitu.library.util.device.DeviceUtils;
import com.meitu.realtime.param.EffectParam;
import com.meitu.realtime.param.FilterParamater;
import com.meitu.realtime.param.OnlineMaterialsParam;
import com.meitu.realtime.parse.OnlineEffectParser;
import com.meitu.realtime.util.MTFilterOperation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by zby on 2016/7/18.
 */
public class CameraFragment extends FilterCameraFragment implements View.OnClickListener {

    private static final int CAMERA_RATIO_1_1 = 1;
    private static final int CAMERA_RATIO_4_3 = 2;
    private static final int CAMERA_RATIO_FULL = 3;

    private ImageView mIvBack, mIvFlash, mIvCameraSwitch, mIvCameraLevel, mIvCameraFilter, mIvCameraRationChange,
        mIvTakePicture;

    /**
     *
     */
    private PreviewFrameLayout mCameraPreviewLayout;

    private View mViewTopCover, mViewBottomCover;

    private RelativeLayout mRlTopBar;
    private LinearLayout mLlBottomBar;

    private ImageView mIvAlumb;
    private ProgressBar mPbSaveImage;

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

    /**
     * 预览的宽高
     */
    private int mPreviewWidth, mPreviewHeight;

    private int mScreenWidth, mScreenHeight;

    private int mPreviewXOut, mPreviewYOut;

    private RectF rect = new RectF(0, 0, 1, 1);

    /**
     * 用来执行连拍的线程池
     */
    private ThreadPoolExecutor executor = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        findView(view);
        loadOnlineMaterialsParams();
        mScreenWidth = DeviceUtils.getScreenWidth();
        mScreenHeight = DeviceUtils.getScreenHeight();
        executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        String orignalDirectory = Environment.getExternalStorageDirectory() + "/DCIM/CameraDemo/";
        File file = new File(orignalDirectory);
        if (!file.exists()) {
            file.mkdirs();
        }
        return view;
    }

    /**
     * @param activity
     */
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
        mFaceView.initFaceDrawable(R.drawable.face_rect, R.drawable.face_rect);

        mIvCameraRationChange = (ImageView) view.findViewById(R.id.iv_switch_picture_ratio);
        mIvCameraRationChange.setOnClickListener(this);

        mCameraPreviewLayout = (PreviewFrameLayout) view.findViewById(R.id.camera_previewframe_layout);
        mViewTopCover = view.findViewById(R.id.view_top_cover);
        mViewBottomCover = view.findViewById(R.id.view_bottom_cover);

        mRlTopBar = (RelativeLayout) view.findViewById(R.id.rl_top_bar);
        mLlBottomBar = (LinearLayout) view.findViewById(R.id.ll_bottom_bar);

        mIvTakePicture = (ImageView) view.findViewById(R.id.iv_take_picture);
        mIvTakePicture.setOnClickListener(this);

        mIvAlumb = (ImageView) view.findViewById(R.id.iv_album);
        mPbSaveImage = (ProgressBar) view.findViewById(R.id.pb_save_image);

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
        mCameraConfig.isDefaultStartFrontCamera = true;
        mCameraConfig.mPreviewLayout = CameraConfig.PREVIEW_LAYOUT.CROP;// 设置预览模式 中间裁剪，所以有可能溢出
        mCameraConfig.canStartPreviewInJpegCallback = false;// 拍照后，如果要继续预览，设置为true
        mCameraConfig.isNeedAutoFocusBeforeTakePicture = true;// 拍照的时候是否需要自动对焦后再拍照
        // mCameraConfig.isPreviewSizesOderByAsc = false;// 预览尺寸优先选最小的
        mCommonCamearProcess = new CommonCameraProcess();
        mCameraConfig.mCameraProcess = mCommonCamearProcess;
        return mCameraConfig;
    }

    /**
     * 拍照数据返回
     *
     * @param jpegData 照片原始数据
     * @param exif     照片的exif信息
     * @param rotation 屏幕旋转角度
     */
    @Override
    protected void onFilterPictureTaken(final byte[] jpegData, final int exif, final int rotation) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (jpegData == null)
                    return;

                PictureData data = new PictureData();
                data.pictureByte = jpegData;
                data.exif = exif;
                data.rotation = rotation;

                Log.d("zby log", "rotation:" + rotation + ",exif:" + exif);
                int[] sizes = getBitmapSize(data.pictureByte);
                Bitmap bitmap =
                    CameraUtil.getBitmapFromByte(data.pictureByte, isBackCameraOpen(), data.rotation, false,
                        Math.max(sizes[0], sizes[1]));
                Log.d("zby log", "bitmap:" + bitmap.getWidth() + ",height:" + bitmap.getHeight() + "," + rect.bottom
                    + "," + rect.top);
                if (BitmapUtils.isAvailableBitmap(bitmap)) {
                    final Bitmap bitmapTemp =
                        BitmapUtils.cropBitmap(bitmap, (int) (rect.left * bitmap.getWidth()),
                            (int) (rect.top * bitmap.getHeight()),
                            (int) ((rect.right - rect.left) * bitmap.getWidth()),
                            (int) ((rect.bottom - rect.top) * bitmap.getHeight()), false);
                    BitmapUtils.release(bitmap);
                    data.bitmap = bitmapTemp;
                    Log.d("zby log","stat:");
                    long start = System.currentTimeMillis();
                    saveBitmap(bitmapTemp, data.exif);
                    long end = System.currentTimeMillis();
                    Log.d("zby log","duration:"+(end - start));
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvAlumb.setImageBitmap(bitmapTemp);
                            mPbSaveImage.setVisibility(View.GONE);
                        }
                    });

                } else {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPbSaveImage.setVisibility(View.GONE);
                        }
                    });
                }

            }
        });

    }

    /**
     * 获取bitmap大小
     *
     * @param data
     * @return
     */
    public static int[] getBitmapSize(byte[] data) {
        if (data == null) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return new int[] {options.outWidth, options.outHeight};
    }

    /**
     * 保存图片到存储卡中
     *
     *
     * */
    public void saveBitmap(Bitmap bitmap, int exif) {
        final String orignalPath = Environment.getExternalStorageDirectory() + "/DCIM/CameraDemo/" + "CameraDemo" + System.currentTimeMillis() + ".jpeg";
        Log.d("zby log","stat: 1");
       /* try {
            FileOutputStream out = new FileOutputStream(new File(orignalPath));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        NativeBitmap nativeBitmap = NativeBitmap.createBitmap();
        nativeBitmap.setImage(bitmap);

        boolean save = CacheUtil.saveImageSD(nativeBitmap, orignalPath, 100);
        Log.d("zby log","stat: 2");
        if (exif != -1) {
            ExifUtil.setExifOrientation(orignalPath, exif);
        }
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
            case R.id.iv_take_picture:
                if (!isEnableProcessCamera())
                    return;
                mPbSaveImage.setVisibility(View.VISIBLE);
                takePicture(false, false);
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

        // 使用这个方法切换滤镜没有暗角效果？
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
        switch (mCurrentRatio) {
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

        if (mCurrentRatio == CAMERA_RATIO_4_3 || mCurrentRatio == CAMERA_RATIO_FULL) {
            changePreviewSize();
        } else {
            resetCameraPreviewSize();
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
            Camera.Size previewSize = getCameraModel().getParameters().getPreviewSize();
            mFaceDectectFunction.initParam(previewSize, isBackCameraOpen(), mPreviewXOut, mPreviewYOut);
        }

        @Override
        public void onCameraOpenFail() {

        }

        @Override
        public void onTakePictureFail() {

        }

        @Override
        public void onPreviewFrameLayoutChange(final int previewLayoutWidth, final int previewLayoutHeight) {
            mPreviewWidth = previewLayoutWidth;
            mPreviewHeight = previewLayoutHeight;
            Log.d("zby log", "mPreviewWidth:" + mPreviewWidth + ",mPreviewHeight:" + mPreviewHeight);
        }
    }

    @Override
    public CameraSize settingPreviewSize(ArrayList arrayList, CameraSize pictureSize) {
        if (arrayList == null || pictureSize == null) {
            return super.settingPreviewSize(arrayList, pictureSize);
        }

        ArrayList<CameraSize> removeSizes = new ArrayList<>();

        for (CameraSize previewSize : (ArrayList<CameraSize>) arrayList) {
            if (previewSize.width * previewSize.height > mScreenWidth * mScreenHeight) {
                removeSizes.add(previewSize);
            }
        }

        if (!removeSizes.isEmpty()) {
            arrayList.removeAll(removeSizes);
        }

        float bestPreviewRatio = (float) pictureSize.width / pictureSize.height;

        CameraSize previewSize = null;

        for (CameraSize cameraSize : (ArrayList<CameraSize>) arrayList) {

            if ((float) cameraSize.width / cameraSize.height == bestPreviewRatio
                && cameraSize.width * cameraSize.height <= mScreenWidth * mScreenHeight) {
                previewSize = cameraSize;
                break;
            }

        }

        // Log.v("zby log", "settingPreviewSize previewSize = " + previewSize.width + "," + previewSize.height);

        return previewSize;
    }

    @Override
    public CameraSize settingPictureSize(ArrayList arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return super.settingPictureSize(arrayList);
        }

        List<Camera.Size> previewSizes = getCameraModel().getParameters().getSupportedPreviewSizes();

        ArrayList<Camera.Size> removeSizes = new ArrayList<>();
        for (Camera.Size previewSize : previewSizes) {
            // Log.d("zby log", "preview size = " + previewSize.width + "," + previewSize.height);
            if (previewSize.width * previewSize.height > mScreenWidth * mScreenHeight) {
                removeSizes.add(previewSize);
            }
        }

        if (!removeSizes.isEmpty()) {
            previewSizes.remove(removeSizes);
        }

        float bestPreviewRatio =
            !isFrontCameraOpen() && mCurrentRatio == CAMERA_RATIO_FULL ? mScreenHeight / mScreenWidth : 4f / 3;

        CameraSize bestPictureSize = null;
        float poor = 0.0001f;

        do {
            for (CameraSize pictureSize : (ArrayList<CameraSize>) arrayList) {

                if (Math.abs(bestPreviewRatio - (float) pictureSize.width / pictureSize.height) < poor) {

                    boolean find = false;

                    for (Camera.Size previewSize : previewSizes) {
                        if ((float) previewSize.height / previewSize.width == (float) pictureSize.height
                            / pictureSize.width) {
                            // Log.d("zby log", "best preview size = " + previewSize.width + "," + previewSize.height);
                            find = true;
                            break;
                        }
                    }

                    if (find) {
                        bestPictureSize = pictureSize;
                        break;
                    }
                }

            }
            poor += 0.0005;
        } while (bestPictureSize == null);

        return bestPictureSize;
    }

    @Override
    public void onEvent(PreviewFrameLayoutEvent previewFrameLayoutEvent) {
        super.onEvent(previewFrameLayoutEvent);
        // 但预览控件的尺寸发生变化的时候会回调
        // 设置 mCameraConfig.mPreviewLayout = CameraConfig.PREVIEW_LAYOUT.INSIDE 才会触发预览控件大小发生变化
    }

    @Override
    public void onEvent(RequestLayoutCameraPreviewEvent requestLayoutCameraPreviewEvent) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetCameraPreviewSize();
            }
        });
    }

    private void resetCameraPreviewSize() {
        if (isFrontCameraOpen()) {
            mIvFlash.setVisibility(View.GONE);
        } else {
            mIvFlash.setVisibility(View.VISIBLE);
        }
        switch (mCurrentRatio) {
            case CAMERA_RATIO_1_1:
                changeUi11();
                break;
            case CAMERA_RATIO_4_3:
                changeUi43();
                break;
            case CAMERA_RATIO_FULL:
                changeUiFull();
                break;
        }
    }

    private void changeUi11() {
        mPreviewXOut = 0;
        mPreviewYOut = 0;
        if (getCameraModel() == null)
            return;

        Camera.Size previewSize = getCameraModel().getParameters().getPreviewSize();
        int previewWidth = previewSize.height;
        int previewHeight = previewSize.width;

        // 计算预览框的大小和偏移量
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCameraPreviewLayout.getLayoutParams();
        params.width = mScreenWidth;
        params.height = mScreenWidth * previewHeight / previewWidth;

        int minBottomHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.camera_bottom_min_height);
        int minTopHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.camera_top_height);
        // 计算预览框的偏移量
        int topMargin = mScreenHeight - params.height - minBottomHeight;
        if (topMargin > 0) {
            if (topMargin > minTopHeight) {
                topMargin = minTopHeight;
            }
        } else {
            topMargin = 0;
        }
        int bottomMargin = mScreenHeight - params.height - topMargin;
        if (bottomMargin < 0) {
            bottomMargin = 0;
        }
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        mCameraPreviewLayout.setLayoutParams(params);

        float previewLayoutRatio = params.height / mScreenWidth;
        float previewRatio = previewHeight / previewWidth;

        if (previewLayoutRatio > previewRatio) {
            mPreviewXOut = (params.height / previewHeight * previewWidth - mScreenWidth) / 2;
        } else {
            mPreviewYOut = (mScreenWidth / previewWidth * previewHeight - params.height) / 2;
        }

        if (mPreviewXOut < 0) {
            mPreviewXOut = 0;
        }

        if (mPreviewYOut < 0) {
            mPreviewYOut = 0;
        }

        // 计算顶部遮盖和底部这概览的高度
        int overSumHegiht = mScreenHeight - mScreenWidth - minTopHeight - minBottomHeight;

        if (overSumHegiht < 0) {
            overSumHegiht = 0;
        }

        // 求出扣除预览、顶部栏和底部栏之外的剩余高度，3等分，顶部这概览三分之一，底部这概览三分之二
        int topCoverHeight = overSumHegiht / 3;
        int bottomCoverHeight = topCoverHeight * 2;

        if (topCoverHeight > 0) {
            RelativeLayout.LayoutParams topParams = (RelativeLayout.LayoutParams) mViewTopCover.getLayoutParams();
            topParams.height = topCoverHeight;
            mViewTopCover.setLayoutParams(topParams);
            mViewTopCover.setVisibility(View.VISIBLE);
        } else {
            mViewTopCover.setVisibility(View.GONE);
        }

        if (bottomCoverHeight > 0) {
            RelativeLayout.LayoutParams bottomParams = (RelativeLayout.LayoutParams) mViewBottomCover.getLayoutParams();
            bottomParams.height = bottomCoverHeight;
            mViewBottomCover.setLayoutParams(bottomParams);
            mViewBottomCover.setVisibility(View.VISIBLE);
        } else {
            mViewBottomCover.setVisibility(View.GONE);
        }

        mRlTopBar.setBackgroundColor(this.getResources().getColor(R.color.white));
        mLlBottomBar.setBackgroundColor(this.getResources().getColor(R.color.white));

        float x = (float) previewWidth / (2 * mPreviewXOut + params.width) * mPreviewXOut / previewWidth;
        Log.d("zby log", "x:" + x);

        float y =
            (float) previewHeight / (2 * mPreviewYOut + params.height)
                * (mPreviewYOut + topCoverHeight + minTopHeight - topMargin) / previewHeight;
        Log.d("zby log", "y:" + y);

        float z =
            (float) previewHeight / (2 * mPreviewYOut + params.height)
                * (mPreviewYOut + (bottomCoverHeight + minBottomHeight - bottomMargin)) / previewHeight;
        Log.d("zby log", "z:" + z);

        rect.left = x;
        rect.top = y;
        rect.right = 1 - x;
        rect.bottom = 1 - z;

        // Log.d("zby log", "dy:" + dy + ",topCoverHeight:" + topCoverHeight + ",bottomCoverHeight:" +
        // bottomCoverHeight);

    }

    private void changeUi43() {
        mPreviewXOut = 0;
        mPreviewYOut = 0;
        if (getCameraModel() == null)
            return;

        Camera.Size previewSize = getCameraModel().getParameters().getPreviewSize();
        int previewWidth = previewSize.height;
        int previewHeight = previewSize.width;

        // 计算预览框的大小和偏移量
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCameraPreviewLayout.getLayoutParams();
        params.width = mScreenWidth;
        params.height = mScreenWidth * previewHeight / previewWidth;

        int minBottomHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.camera_bottom_min_height);
        int minTopHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.camera_top_height);
        // 计算预览框的偏移量
        int topMargin = mScreenHeight - params.height - minBottomHeight;
        if (topMargin > 0) {
            if (topMargin > minTopHeight) {
                topMargin = minTopHeight;
            }
        } else {
            topMargin = 0;
        }
        int bottomMargin = mScreenHeight - params.height - topMargin;
        if (bottomMargin < 0) {
            bottomMargin = 0;
        }
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        mCameraPreviewLayout.setLayoutParams(params);

        float previewLayoutRatio = params.height / mScreenWidth;
        float previewRatio = previewHeight / previewWidth;

        if (previewLayoutRatio > previewRatio) {
            mPreviewXOut = (params.height / previewHeight * previewWidth - mScreenWidth) / 2;
        } else {
            mPreviewYOut = (mScreenWidth / previewWidth * previewHeight - params.height) / 2;
        }

        if (mPreviewXOut < 0) {
            mPreviewXOut = 0;
        }

        if (mPreviewYOut < 0) {
            mPreviewYOut = 0;
        }

        // 计算顶部遮盖和底部这概览的高度
        int overSumHegiht = mScreenHeight - mScreenWidth / 3 * 4 - minTopHeight - minBottomHeight;

        if (overSumHegiht < 0) {
            overSumHegiht = 0;
        }

        // 求出扣除预览、顶部栏和底部栏之外的剩余高度，3等分，顶部这概览三分之一，底部这概览三分之二
        int topCoverHeight = overSumHegiht / 3;
        int bottomCoverHeight = topCoverHeight * 2;

        if (topCoverHeight > 0) {
            RelativeLayout.LayoutParams topParams = (RelativeLayout.LayoutParams) mViewTopCover.getLayoutParams();
            topParams.height = topCoverHeight;
            mViewTopCover.setLayoutParams(topParams);
            mViewTopCover.setVisibility(View.VISIBLE);
        } else {
            topCoverHeight = 0;
            mViewTopCover.setVisibility(View.GONE);
        }

        if (bottomCoverHeight > 0) {
            RelativeLayout.LayoutParams bottomParams = (RelativeLayout.LayoutParams) mViewBottomCover.getLayoutParams();
            bottomParams.height = bottomCoverHeight;
            mViewBottomCover.setLayoutParams(bottomParams);
            mViewBottomCover.setVisibility(View.VISIBLE);
        } else {
            bottomCoverHeight = 0;
            mViewBottomCover.setVisibility(View.GONE);
        }

        mRlTopBar.setBackgroundColor(this.getResources().getColor(R.color.white));
        mLlBottomBar.setBackgroundColor(this.getResources().getColor(R.color.white));

        float x = (float) previewWidth / (2 * mPreviewXOut + params.width) * mPreviewXOut / previewWidth;
        Log.d("zby log", "x:" + x);

        float y =
            (float) previewHeight / (2 * mPreviewYOut + params.height)
                * (mPreviewYOut + topCoverHeight + minTopHeight - topMargin) / previewHeight;
        Log.d("zby log", "y:" + y);

        float z =
            (float) previewHeight / (2 * mPreviewYOut + params.height)
                * (mPreviewYOut + (bottomCoverHeight + minBottomHeight - bottomMargin)) / previewHeight;
        Log.d("zby log", "z:" + z);

        rect.left = x;
        rect.top = y;
        rect.right = 1 - x;
        rect.bottom = 1 - z;

        // Log.d("zby log", "changeUi43 dy:" + dy + ",topCoverHeight:" + topCoverHeight + ",bottomCoverHeight:" +
        // bottomCoverHeight);

    }

    private void changeUiFull() {
        mPreviewXOut = 0;
        mPreviewYOut = 0;
        if (getCameraModel() == null)
            return;

        Camera.Size previewSize = getCameraModel().getParameters().getPreviewSize();
        int previewWidth = previewSize.height;
        int previewHeight = previewSize.width;

        // 计算预览框的大小和偏移量
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCameraPreviewLayout.getLayoutParams();
        params.width = mScreenWidth;
        params.height = mScreenHeight;
        params.setMargins(0, 0, 0, 0);
        mCameraPreviewLayout.setLayoutParams(params);

        float previewLayoutRatio = (float) mScreenHeight / mScreenWidth;
        float previewRatio = (float) previewHeight / previewWidth;

        if (previewLayoutRatio > previewRatio) {
            mPreviewXOut = (int) ((mScreenHeight / previewRatio - mScreenWidth) / 2);
        } else {
            mPreviewYOut = (int) ((mScreenWidth * previewRatio - mScreenHeight) / 2);
        }

        if (mPreviewXOut < 0) {
            mPreviewXOut = 0;
        }

        if (mPreviewYOut < 0) {
            mPreviewYOut = 0;
        }

        mRlTopBar.setBackgroundColor(this.getResources().getColor(R.color.color_white_85));
        mLlBottomBar.setBackgroundColor(this.getResources().getColor(android.R.color.transparent));
        mViewTopCover.setVisibility(View.GONE);
        mViewBottomCover.setVisibility(View.GONE);

        float x = (float) previewWidth / (2 * mPreviewXOut + params.width) * mPreviewXOut / previewWidth;
        Log.d("zby log", "x:" + x);

        float y = (float) previewHeight / (2 * mPreviewYOut + params.height) * mPreviewYOut / previewHeight;
        Log.d("zby log", "y:" + y);

        rect.left = x;
        rect.top = y;
        rect.right = 1 - x;
        rect.bottom = 1 - y;
    }

}
