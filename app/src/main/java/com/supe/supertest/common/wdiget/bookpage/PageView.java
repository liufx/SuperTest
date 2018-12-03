package com.supe.supertest.common.wdiget.bookpage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.supe.supertest.common.utils.ScreenUtils;
import com.supe.supertest.common.wdiget.bookpage.animation.CoverPageAnim;
import com.supe.supertest.common.wdiget.bookpage.animation.HorizonPageAnim;
import com.supe.supertest.common.wdiget.bookpage.animation.NonePageAnim;
import com.supe.supertest.common.wdiget.bookpage.animation.PageAnimation;
import com.supe.supertest.common.wdiget.bookpage.animation.ScrollPageAnim;
import com.supe.supertest.common.wdiget.bookpage.animation.SimulationPageAnim;
import com.supe.supertest.common.wdiget.bookpage.animation.SlidePageAnim;
import com.supe.supertest.common.wdiget.bookpage.show.ShowChar;
import com.supe.supertest.common.wdiget.bookpage.show.ShowLine;
import com.supermax.base.common.widget.toast.QsToast;

import java.util.List;
import java.util.Timer;


/**
 * @Author yinzh
 * @Date 2018/11/26 15:47
 * @Description
 */
public class PageView extends View {

    //滚动效果
    public final static int PAGE_MODE_SIMULATION = 0;
    public final static int PAGE_MODE_COVER = 1;
    public final static int PAGE_MODE_SLIDE = 2;
    public final static int PAGE_MODE_NONE = 3;
    public final static int PAGE_MODE_SCROLL = 4;

    private final static String TAG = "BookPageWidget";

    private int mViewWidth = 0; // 当前View的宽
    private int mViewHeight = 0; // 当前View的高

    private int mStartX = 0;
    private int mStartY = 0;
    private boolean isMove = false;
    //初始化参数
    private int mBgColor = 0xFFCEC29C;
    private int mPageMode = PAGE_MODE_COVER;

    //是否允许点击
    private boolean canTouch = true;
    //判断是否初始化完成
    private boolean isPrepare = false;
    //是否是长按事件
    private boolean isLongClick = false;
    //唤醒菜单的区域
    private RectF mCenterRect = null;

    private RectF mViewF = null;

    //动画类
    private PageAnimation mPageAnim;
    //动画监听类
    private PageAnimation.OnPageChangeListener mPageAnimListener = new PageAnimation.OnPageChangeListener() {
        @Override
        public boolean hasPrev() {
            return PageView.this.hasPrev();
        }

        @Override
        public boolean hasNext() {
            return PageView.this.hasNext();
        }

        @Override
        public void pageCancel() {
            mTouchListener.cancel();
            mPageLoader.pageCancel();
        }
    };

    //点击监听
    private TouchListener mTouchListener;
    //内容加载器
    private PageLoader mPageLoader;

    public PageView(Context context) {
        this(context, null);
    }

    public PageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 注册长按事件
     */
    private void init() {
        setOnLongClickListener(longClickListener);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        //重置图片的大小,由于w,h可能比原始的Bitmap更大，所以如果使用Bitmap.setWidth/Height()是会报错的。
        //所以最终还是创建Bitmap的方式。这种方式比较消耗性能，暂时没有找到更好的方法。
        setPageMode(mPageMode);
        //重置页面加载器的页面
        mPageLoader.setDisplaySize(w, h);
        //初始化完成
        isPrepare = true;
    }

    //设置翻页的模式
    public void setPageMode(int pageMode) {
        mPageMode = pageMode;
        //视图未初始化的时候，禁止调用
        if (mViewWidth == 0 || mViewHeight == 0) return;

        switch (pageMode) {
            case PAGE_MODE_SIMULATION:
                mPageAnim = new SimulationPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_COVER:
                mPageAnim = new CoverPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_SLIDE:
                mPageAnim = new SlidePageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_NONE:
                mPageAnim = new NonePageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_SCROLL:
                mPageAnim = new ScrollPageAnim(mViewWidth, mViewHeight, 0,
                        ScreenUtils.dpToPx(PageLoader.DEFAULT_MARGIN_HEIGHT), this, mPageAnimListener);
                break;
            default:
                mPageAnim = new SimulationPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
        }
    }

    public Bitmap getNextPage() {
        if (mPageAnim == null) return null;
        return mPageAnim.getNextBitmap();
    }

    public Bitmap getBgBitmap() {
        if (mPageAnim == null) return null;
        return mPageAnim.getBgBitmap();
    }


    public boolean autoPrevPage() {
        //滚动暂时不支持自动翻页
        if (mPageAnim instanceof ScrollPageAnim) {
            return false;
        } else {
            startPageAnim(PageAnimation.Direction.PRE);
            return true;
        }
    }

    public boolean autoNextPage() {
        if (mPageAnim instanceof ScrollPageAnim) {
            return false;
        } else {
            startPageAnim(PageAnimation.Direction.NEXT);
            return true;
        }
    }

    private void startPageAnim(PageAnimation.Direction direction) {
        if (mTouchListener == null) return;
        //是否正在执行动画
        abortAnimation();
        if (direction == PageAnimation.Direction.NEXT) {
            int x = mViewWidth;
            int y = mViewHeight;
            //设置点击点
            mPageAnim.setTouchPoint(x, y);
            //初始化动画
            mPageAnim.setStartPoint(x, y);
            //设置方向
            Boolean hasNext = hasNext();

            mPageAnim.setDirection(direction);
            if (!hasNext) {
                return;
            }
        } else {
            int x = 0;
            int y = mViewHeight;
            //初始化动画
            mPageAnim.setStartPoint(x, y);
            //设置点击点
            mPageAnim.setTouchPoint(x, y);
            mPageAnim.setDirection(direction);
            //设置方向方向
            Boolean hashPrev = hasPrev();
            if (!hashPrev) {
                return;
            }
        }
        mPageAnim.startAnim();
        this.postInvalidate();
    }

    public void setBgColor(int color) {
        mBgColor = color;
    }

    public void canTouchable(boolean touchable) {
        canTouch = touchable;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //绘制背景
        canvas.drawColor(mBgColor);

        //绘制动画
        mPageAnim.draw(canvas);
    }

    private Timer timer = null;

    private float Down_X = -1, Down_Y = -1;

    private OnLongClickListener longClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            QsToast.show("我是长按事件");
            if (Down_X > 0 && Down_Y > 0) {// 说明还没释放，是长按事件
//                isLongClick = true;
//                postInvalidate();
            }
            return false;
        }
    };


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (!canTouch && event.getAction() != MotionEvent.ACTION_DOWN) return true;

        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = x;
                mStartY = y;

                Down_X = x;
                Down_Y = y;
                isMove = false;
//                if(!isLongClick){
                canTouch = mTouchListener.onTouch();//  onTouch 事件。
//                    isLongClick = false;
//                }
                mPageAnim.onTouchEvent(event);

                TxtPage curPage = mPageLoader.getCurPage(mPageLoader.getPagePos());
                List<ShowLine> showLines = curPage.showLines;
                for (int m = 0; m < showLines.size(); m++) {
                    if(y < showLines.get(m).lintHeight){// 确定行
                        if(m == 0){
                         char firstChar = showLines.get(m).CharsData.get(0).charData;
                         char lastChar = showLines.get(m).CharsData.get(showLines.get(m).CharsData.size() -1 ).charData;
                        } else {
                            if(y > showLines.get(m -1).lintHeight && y < showLines.get(m).lintHeight){
                               Log.i("PageView Log", showLines.get(m).getLineData());
                            }

                        }

                    }

                }

                break;
            case MotionEvent.ACTION_MOVE:
                //判断是否大于最小滑动值。
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (!isMove) {
                    isMove = Math.abs(mStartX - event.getX()) > slop || Math.abs(mStartY - event.getY()) > slop;
                }

                //如果滑动了，则进行翻页。
                if (isMove) {
                    mPageAnim.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                Release();
                if (!isMove) {
                    //设置中间区域范围
                    if (mCenterRect == null) {
                        mCenterRect = new RectF(mViewWidth / 5, mViewHeight / 3,
                                mViewWidth * 4 / 5, mViewHeight * 2 / 3);
                    }

                    //是否点击了中间
                    if (mCenterRect.contains(x, y)) {
                        if (mTouchListener != null && !isLongClick) {
                            mTouchListener.center();
                            isLongClick = false;
                        }
                        return true;
                    }

                    //设置标注区域范围  ----818458----986----332-----
//                    if(mViewF == null){
//                        mViewF = new RectF(818,332,986,458);
//                    }
//
//                    Log.i("FBReader","x===" + x + "yyy" + y);
//
//                    if(mViewF.contains(x,y) && !isLongClick){
//                        mTouchListener.center();
//                        isLongClick = false;
//                        return true;
//                    }
                    isLongClick = false;
                }
                mPageAnim.onTouchEvent(event);
                break;
        }
        return true;
    }

    private void Release() {
        Down_X = -1;// 释放
        Down_Y = -1;
    }

    //判断是否下一页存在
    private boolean hasNext() {
        Boolean hasNext = false;
        if (mTouchListener != null) {
            hasNext = mTouchListener.nextPage();
            //加载下一页
            if (hasNext) {
                hasNext = mPageLoader.next();
            }
        }
        return hasNext;
    }

    //判断是否存在上一页
    private boolean hasPrev() {
        Boolean hasPrev = false;
        if (mTouchListener != null) {
            hasPrev = mTouchListener.prePage();
            //加载下一页
            if (hasPrev) {
                hasPrev = mPageLoader.prev();
            }
        }
        return hasPrev;
    }

    @Override
    public void computeScroll() {
        //进行滑动
        mPageAnim.scrollAnim();
        super.computeScroll();
    }

    //如果滑动状态没有停止就取消状态，重新设置Anim的触碰点
    public void abortAnimation() {
        mPageAnim.abortAnim();
    }

    public boolean isPrepare() {
        return isPrepare;
    }

    public boolean isRunning() {
        return mPageAnim.isRunning();
    }

    public void setTouchListener(TouchListener mTouchListener) {
        this.mTouchListener = mTouchListener;
    }

    public void drawNextPage() {
        if (mPageAnim instanceof HorizonPageAnim) {
            ((HorizonPageAnim) mPageAnim).changePage();
        }
        mPageLoader.onDraw(getNextPage(), false);
    }

    /**
     * 刷新当前页(主要是为了ScrollAnimation)
     */
    public void refreshPage() {
        if (mPageAnim instanceof ScrollPageAnim) {
            ((ScrollPageAnim) mPageAnim).refreshBitmap();
        }
        drawCurPage(false);
    }

    //refreshPage和drawCurPage容易造成歧义,后面需要修改

    /**
     * 绘制当前页。
     *
     * @param isUpdate
     */
    public void drawCurPage(boolean isUpdate) {
        mPageLoader.onDraw(getNextPage(), isUpdate);
    }

    //获取PageLoader
    public PageLoader getPageLoader(boolean isLocal) {
        if (mPageLoader == null) {
            if (isLocal) {
                mPageLoader = new LocalPageLoader(this);
            } else {
                mPageLoader = new NetPageLoader(this);
            }
        }
        return mPageLoader;
    }

    public interface TouchListener {
        void center();

        boolean onTouch();

        boolean prePage();

        boolean nextPage();

        void cancel();
    }
}
