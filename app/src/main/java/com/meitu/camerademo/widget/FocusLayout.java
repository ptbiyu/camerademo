package com.meitu.camerademo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.meitu.camera.ui.FocusIndicator;
import com.meitu.camera.util.CameraUtil;
import com.meitu.camerademo.R;
import com.meitu.library.application.BaseApplication;
import com.meitu.library.util.device.DeviceUtils;

/**
 * Created by jsg on 2016/3/4.
 * 对焦控件，显示对焦以及对焦过程中的ui效果
 */
public class FocusLayout extends RelativeLayout implements FocusIndicator {

    private static final String TAG = FocusLayout.class.getSimpleName();
    /**
     * 整体view
     */
    private View mView;
    /**
     * 内外两个环
     */
    private ImageView imgViewOuter, imgViewInner;
    /**
     * 是否要显示对焦ui
     */
    private boolean isNeedShowFocusView;

    private Runnable mRunnable;

    public FocusLayout(Context context) {
        super(context);
        initView();
    }

    public FocusLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        mView = LayoutInflater.from(getContext()).inflate(R.layout.camera_demo_common_focuslayout, null);
        imgViewOuter = (ImageView) mView.findViewById(R.id.imgView_outer);
        imgViewInner = (ImageView) mView.findViewById(R.id.imgView_inner);
        mView.setVisibility(View.GONE);

        int w =
                (int) (getResources().getDrawable(R.drawable.camera_demo_common_focus_inner).getIntrinsicWidth() * DeviceUtils.getDensityValue());
        int h =
                (int) (getResources().getDrawable(R.drawable.camera_demo_common_focus_outer).getIntrinsicHeight() * DeviceUtils.getDensityValue());
        //添加兩個环的外部视图
        LayoutParams layoutParams = new LayoutParams(w, h);
        layoutParams.addRule(CENTER_IN_PARENT);
        addView(mView, layoutParams);
        //外部为2倍大小
        LayoutParams rootParams = new LayoutParams(w * 2, h * 2);
        setLayoutParams(rootParams);
    }

    @Override
    public void showStart() {
        //开始对焦
        if (!isNeedShowFocusView) {
            mView.setVisibility(View.GONE);
            return;
        }
        beginAnim();
    }

    @Override
    public void showSuccess() {
        //对焦成功
        if (!isNeedShowFocusView) {
            return;
        }
        hideFocusIndicatorView();
    }

    @Override
    public void showFail() {
        //对焦失败
        hideFocusIndicatorView();
    }

    @Override
    public void clear() {
        //清除对焦ui
        hideView();
    }

    @Override
    public void onTouch(final float xPos, final float yPos, final int previewWidth, final int previewHeight, final boolean isNeedShowFocusView) {
        //用户点击位置
        this.isNeedShowFocusView = isNeedShowFocusView;
        LayoutParams p = (LayoutParams) getLayoutParams();
        int left = CameraUtil.clamp((int) (xPos - getWidth() / 2), -getWidth() / 2, previewWidth);
        int top = CameraUtil.clamp((int) (yPos - getHeight() / 2), -getHeight() / 2, previewHeight);
        int right = 0;
        int bottom = 0;
        if (xPos + getWidth() / 2 > previewWidth) {
            right = (int) (previewWidth - (xPos + getWidth() / 2));
        }
        if (yPos + getHeight() / 2 > previewHeight) {
            bottom = (int) (previewHeight - (yPos + getHeight() / 2));
        }
        p.setMargins(left, top, right, bottom);
        // Disable "center" rule because we no longer want to put it in the
        // center.
        int[] rules = p.getRules();
        rules[RelativeLayout.CENTER_IN_PARENT] = 0;
        requestLayout();
    }

    @Override
    public void resetTouchFocus() {
        //重置对焦状态
        LayoutParams p = (LayoutParams) getLayoutParams();
        int[] rules = p.getRules();
        rules[RelativeLayout.CENTER_IN_PARENT] = RelativeLayout.TRUE;
        p.setMargins(0, 0, 0, 0);
    }

    private void hideFocusIndicatorView() {
        endAnim();
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    hideView();
                }
            };
        }
        postDelayed(mRunnable, 500);
    }

    /**
     * 开始对焦动画
     */
    public void beginAnim() {
        if (mRunnable != null) {
            removeCallbacks(mRunnable);
        }
        mView.setVisibility(View.VISIBLE);
        imgViewOuter.clearAnimation();
        imgViewInner.clearAnimation();
        imgViewInner.startAnimation(AnimationUtils.loadAnimation(BaseApplication.getApplication(),
                R.anim.camera_demo_common_focus_inner_visible));
        imgViewOuter.startAnimation(AnimationUtils.loadAnimation(BaseApplication.getApplication(),
                R.anim.camera_demo_common_focus_outer_visible));
    }

    /**
     * 对焦完成动画
     */
    public void endAnim() {
        imgViewOuter.startAnimation(AnimationUtils.loadAnimation(BaseApplication.getApplication(),
                R.anim.camera_demo_common_focus_inner_gone));
        imgViewInner.startAnimation(AnimationUtils.loadAnimation(BaseApplication.getApplication(),
                R.anim.camera_demo_common_focus_outer_gone));
    }

    /**
     * 隐藏对焦框
     */
    public void hideView() {
        if (imgViewOuter != null) {
            imgViewOuter.clearAnimation();
        }
        if (imgViewInner != null) {
            imgViewInner.clearAnimation();
        }
        if (mView != null) {
            mView.setVisibility(View.GONE);
        }
    }

}
