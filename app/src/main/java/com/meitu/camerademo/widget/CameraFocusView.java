package com.meitu.camerademo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.meitu.camera.ui.FocusIndicator;
import com.meitu.camera.util.CameraUtil;
import com.meitu.camerademo.R;

/**
 * Created by zby on 2016/7/28.
 */
public class CameraFocusView extends RelativeLayout implements FocusIndicator {

    private ImageView mIvFocus;

    private Animation mAnimation;

    private Runnable mRunable;

    public CameraFocusView(Context context) {
        super(context);
        initView(context);
    }

    public CameraFocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mIvFocus = new ImageView(context);
        mIvFocus.setImageResource(R.drawable.camera_focus);
        mIvFocus.setVisibility(View.INVISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //少了这句没有显示出来
        params.addRule(CENTER_IN_PARENT);
        addView(mIvFocus,params);
    }


    @Override
    public void showStart() {
        beginAnim();
    }

    @Override
    public void showSuccess() {
        hideFocusView();
    }

    @Override
    public void showFail() {
        hideFocusView();
    }

    @Override
    public void clear() {
        mIvFocus.clearAnimation();
        mIvFocus.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onTouch(final float xPos, final float yPos, final int previewWidth, final int previewHeight, final boolean isNeedShowFocusView) {
        //用户点击位置
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

    private void beginAnim() {
        if (mRunable != null){
            mIvFocus.removeCallbacks(mRunable);
        }
        if (mAnimation ==  null){
            mAnimation = AnimationUtils.loadAnimation(getContext(),R.anim.camera_focus_anim);
        }
        mIvFocus.setVisibility(View.VISIBLE);
        mIvFocus.clearAnimation();
        mIvFocus.startAnimation(mAnimation);
    }

    private void hideFocusView() {
        mIvFocus.clearAnimation();
        //等待500毫秒后隐藏对焦框
        if (mRunable == null){
            mRunable = new Runnable() {
                @Override
                public void run() {
                    mIvFocus.setVisibility(View.INVISIBLE);
                }
            };
        }
        mIvFocus.postDelayed(mRunable,500);
    }

}
