package com.meitu.camerademo.face;

import android.app.Activity;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;

import com.meitu.camera.ui.FaceView;
import com.meitu.core.facedetect.FaceDetector;
import com.meitu.core.types.FaceData;

import java.util.ArrayList;


/**
 * Created by jsg on 2016/3/4.
 * 人脸识别
 */
public class FaceDectectFunction implements Runnable {
    private static final String TAG = FaceDectectFunction.class.getSimpleName();
    /**
     * 人脸识别ui
     */
    private FaceView mFaceView;
    /**
     * 是否停止人脸识别线程
     */
    private volatile boolean mIsRequestStop = false;
    /**
     * 是否暂停人脸识别
     */
    private volatile boolean mIsRequestPause = false;
    /**
     * 同步锁
     */
    private final Object mSync = new Object();
    /**
     * 预览数据
     */
    private volatile byte[] mPreviewData = null;
    /**
     * 外部Activity引用
     */
    private Activity mActivity;
    /**
     * 人脸识别
     */
    private FaceDetector mFaceDetector;
    /**
     * 外部回调
     */
    private IFaceDectectFunction mIFaceDectectFunction;
    /**
     * 预览尺寸
     */
    private Camera.Size mPreviewSize;
    /**
     * 是否后置摄像头
     */
    private boolean mIsBackCameraOpen;
    /**
     * 人脸识别数据和视图的数据比例
     */
    private float mAspectRadio;

    private int mPreviewXOut,mPrevewYOut;

    public FaceDectectFunction(Activity activity, FaceView faceView, IFaceDectectFunction faceDectectFunction) {
        mFaceView = faceView;
        mActivity = activity;
        mIFaceDectectFunction = faceDectectFunction;
        new Thread(this, TAG).start();
    }

    /**
     * 初始化人脸识别参数，相机开启成功、切换摄像头后调用
     *
     * @param previewSize
     * @param isBackCameraOpen
     */
    public void initParam(Camera.Size previewSize, boolean isBackCameraOpen, int previewXOut, int previewYOut) {
        mPreviewSize = previewSize;
        mIsBackCameraOpen = isBackCameraOpen;
        mPreviewXOut = previewXOut;
        mPrevewYOut = previewYOut;
        Log.d("zby log", "mPreviewXOut:" + mPreviewXOut + ",mPrevewYOut:" + mPrevewYOut+ ",mPreviewSize.width:" + mPreviewSize.width+ ",mPreviewSize.height:" + mPreviewSize.height);
        mAspectRadio = mPreviewXOut > 0 ? (float) mFaceView.getWidth() / mPreviewSize.height
                : (float) mFaceView.getHeight() / mPreviewSize.width;
        //mAspectRadio = (float) mFaceView.getHeight() / (float) mPreviewSize.width;
        mPreviewData = null;
        clearFaceView();
    }

    /**
     * 开始人脸识别
     */
    public void startFaceDectect() {
        synchronized (mSync) {
            mFaceDetector = FaceDetector.instance();
            mFaceDetector.faceDetect_init(mActivity);
            mIsRequestStop = false;
            mSync.notifyAll();
        }
    }

    /**
     * 停止人脸识别
     */
    public void stopFaceDectect() {
        synchronized (mSync) {
            if (mIsRequestStop) {
                return;
            }
            mFaceDetector = null;
            mActivity = null;
            mIsRequestStop = true;
            mSync.notifyAll();
        }
    }

    /**
     * 暂停人脸识别
     */
    public void pauseFaceDectect() {
        synchronized (mSync) {
            if (mIsRequestPause) {
                return;
            }
            clearFaceView();
            mIsRequestPause = true;
            mSync.notifyAll();
        }
    }

    /**
     * 恢复人脸识别
     */
    public void resumeFaceDectect() {
        synchronized (mSync) {
            mIsRequestPause = false;
            mSync.notifyAll();
        }
    }

    /**
     * 人脸识别是否暂停
     *
     * @return
     */
    public boolean isFaceDectectPause() {
        return mIsRequestPause;
    }

    /**
     * 清除人脸识别ui
     */
    private void clearFaceView() {
        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mFaceView.clear();
            }
        });
    }

    @Override
    public void run() {
        while (true) {
            if (mIsRequestStop) {
                //停止人脸识别
                break;
            } else {
                if (mIsRequestPause) {
                    synchronized (mSync) {
                        clearFaceView();
                        try {
                            mSync.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (mPreviewData != null) {
                        //人脸识别
                        final FaceData faceData =
                                mFaceDetector.faceDetect_NV12(mPreviewData, mPreviewSize.width, mPreviewSize.height,
                                        (mIFaceDectectFunction.getOrientation() + 90) % 360, mIsBackCameraOpen);
                        int faceCount = (faceData == null ? 0 : faceData.getFaceCount());
                        if (faceCount == 0) {
                            clearFaceView();
                        } else {
                            float dx = 0;
                            if (mPreviewXOut > 0) {
                                dx = -mPreviewXOut / 2;
                            } else {
                                dx = -mPrevewYOut / 2;
                            }
                            ArrayList<RectF> faceList = faceData.getFaceRectList();
                            // 人脸转换成具体的坐标区域，并绘制到预览界面
                            for (int i = 0; i < faceCount; i++) {
                                RectF rectF = faceList.get(i);
                                rectF.set((rectF.left * mAspectRadio),
                                        (rectF.top * mAspectRadio),
                                        (rectF.right * mAspectRadio),
                                        (rectF.bottom * mAspectRadio));
                                rectF.offset(mPreviewXOut,mPrevewYOut);
                                rectF.inset(dx,dx);
                            }
                            mFaceView.setFaces(faceList);
                        }
                    }
                    synchronized (mSync) {
                        try {
                            //人脸识别一次完就等待下一次数据到来再识别
                            mPreviewData = null;
                            if (mIsRequestPause) {
                                clearFaceView();
                            }
                            mSync.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 预览数据处理
     *
     * @param bytes
     */
    public void drain(final byte[] bytes) {
        if (mIsRequestStop || mIsRequestPause) {
            return;
        }
        synchronized (mSync) {
            mPreviewData = bytes;
            mSync.notifyAll();
        }
    }

}
