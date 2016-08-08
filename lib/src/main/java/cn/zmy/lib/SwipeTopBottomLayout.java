package cn.zmy.lib;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.AbsListView;

/**
 * Created by zmy on 2016/8/8 0008.
 */
public class SwipeTopBottomLayout extends ViewGroup implements NestedScrollingParent
{
    //旋转View的默认背景
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    //旋转View的直径
    private static final int CIRCLE_DIAMETER = 40;
    //旋转View最大垂直偏移量
    private static final int MAX_OFFSET = 130;
    //旋转View可以执行刷新或者加载更多操作的最小偏移量
    private static final int EXECUTE_OFFSET = 80;
    //旋转View的最低透明度
    private static final int MIN_ALPHA = 60;
    //旋转View回到起始点动画的时长
    private static final int ANIMATE_TO_START_DURATION = 200;

    //通常为包含的第一个子View
    private View mainChild;
    //用于刷新的旋转View，处于上方
    private CircleImageView circleImageViewTop;
    //用于加载更多的旋转View，处于下方
    private CircleImageView circleImageViewBottom;

    private MaterialProgressDrawable progressDrawableTop;
    private MaterialProgressDrawable progressDrawableBottom;
    private NestedScrollingParentHelper nestedScrollingParentHelper;

    private boolean isRefreshing;//是否正在刷新
    private boolean isLoadingMore;//是否正在加载更多
    private boolean isAutoMoving;//是否处于移动状态
    private boolean isRefreshEnabled;//是否启用刷新功能
    private boolean isLoadMoreEnabled;//是否启用加载更多功能

    //旋转View的宽度
    private int circleWidth;
    //旋转View的高度
    private int circleHeight;

    //旋转View最大可移动的距离
    private int maxOffset;
    //旋转View需要执行刷新或加载更多最少需要移动的距离
    private int executeOffset;

    private OnRefreshListener onRefreshListener;
    private OnLoadMoreListener onLoadMoreListener;

    private MoveAnimation moveAnimation;

    public SwipeTopBottomLayout(Context context)
    {
        this(context, null);
    }

    public SwipeTopBottomLayout(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public SwipeTopBottomLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
        {
            //4.4以下需要多一些宽高度，否则可能无法完全隐藏旋转View
            this.circleWidth = dipToPx(CIRCLE_DIAMETER + 8);
            this.circleHeight = dipToPx(CIRCLE_DIAMETER + 8);
        }
        else
        {
            this.circleWidth = dipToPx(CIRCLE_DIAMETER);
            this.circleHeight = dipToPx(CIRCLE_DIAMETER);
        }
        this.nestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        this.moveAnimation = new MoveAnimation();

        setWillNotDraw(true);
        setRefreshEnabled(true);
        setLoadMoreEnabled(true);
        setMaxOffset(dipToPx(MAX_OFFSET));
        setExecuteOffset(dipToPx(EXECUTE_OFFSET));

        onCircleImageViewCreated();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        if (mainChild == null)
        {
            getMainChild();
        }
        if (mainChild == null)
        {
            return;
        }

        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        mainChild.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        int circleTopWidth = circleImageViewTop.getMeasuredWidth();
        int circleTopHeight = circleImageViewTop.getMeasuredHeight();
        circleImageViewTop.layout((width / 2 - circleTopWidth / 2), -circleTopHeight, (width / 2 - circleTopWidth / 2) + this.circleWidth, 0);

        int circleBottomWidth = circleImageViewBottom.getMeasuredWidth();
        int circleBottomHeight = circleImageViewBottom.getMeasuredHeight();
        circleImageViewBottom.layout((width / 2 - circleBottomWidth / 2), height, (width / 2 - circleTopWidth / 2) + this.circleWidth, height + circleBottomHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mainChild == null)
        {
            getMainChild();
        }
        if (mainChild == null)
        {
            return;
        }
        mainChild.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        circleImageViewTop.measure(MeasureSpec.makeMeasureSpec(circleWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(circleHeight, MeasureSpec.EXACTLY));
        circleImageViewBottom.measure(MeasureSpec.makeMeasureSpec(circleWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(circleHeight, MeasureSpec.EXACTLY));
    }

    @Override
    public void onViewAdded(View child)
    {
        //保证CircleImageView总是处于最上层
        if (!(child instanceof CircleImageView))
        {
            int index = indexOfChild(child);
            if (index != 0)
            {
                removeView(child);
                addView(child, 0);
            }
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b)
    {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mainChild instanceof AbsListView) || (mainChild != null && !ViewCompat.isNestedScrollingEnabled(mainChild)))
        {
            // Nope.
        }
        else
        {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes)
    {
        return isEnabled() && !isAutoMoving && !isRefreshing && !isLoadingMore && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public int getNestedScrollAxes()
    {
        return ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes)
    {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed)
    {
        Log.i("onNestedPreScroll",String.format("dy=%d",dy));
        if (dy > 0 && isTopCircleShowing())
        {
            moveTopCircleImageView(-dy);
            consumed[1] = dy;
        }

        if (dy < 0 && isBottomCircleShowing())
        {
            moveBottomCircleImageView(-dy);
            consumed[1] = dy;
        }
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY)
    {
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed)
    {
        return false;
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed)
    {
        Log.i("onNestedScroll", String.format("dyUnconsumed=%d", dyUnconsumed));
        if (isRefreshEnabled && dyUnconsumed < 0 && !canChildScrollUp())
        {
            moveTopCircleImageView(-dyUnconsumed);
        }

        if (isLoadMoreEnabled && dyUnconsumed > 0 && !canChildScrollDown())
        {
            moveBottomCircleImageView(-dyUnconsumed);
        }
    }

    @Override
    public void onStopNestedScroll(View target)
    {
        nestedScrollingParentHelper.onStopNestedScroll(target);
        if (isTopCircleShowing())
        {
            finishTop();
        }

        if (isBottomCircleShowing())
        {
            finishBottom();
        }
    }

    /**
     * 创建圆形旋转View
     */
    protected void onCircleImageViewCreated()
    {
        progressDrawableTop = new MaterialProgressDrawable(getContext(), this);
        progressDrawableTop.setBackgroundColor(CIRCLE_BG_LIGHT);
        progressDrawableBottom = new MaterialProgressDrawable(getContext(), this);
        progressDrawableBottom.setBackgroundColor(CIRCLE_BG_LIGHT);

        circleImageViewTop = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER / 2);
        circleImageViewTop.setImageDrawable(progressDrawableTop);
        addView(circleImageViewTop);

        circleImageViewBottom = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER / 2);
        circleImageViewBottom.setImageDrawable(progressDrawableBottom);
        addView(circleImageViewBottom);
    }

    protected void getMainChild()
    {
        if (getChildCount() > 0)
        {
            for (int i = 0; i < getChildCount(); i++)
            {
                View view = getChildAt(i);
                if (!(view instanceof CircleImageView))
                {
                    this.mainChild = view;
                    break;
                }
            }
        }
    }

    protected void moveTopCircleImageView(int yOffset)
    {
        int totalOffset = getTopTotalOffset();
        if (totalOffset >= maxOffset && yOffset >= 0)
        {
            return;
        }

        int offset = (int) (yOffset * getOffsetPercent(totalOffset) + 0.5f);
        ViewCompat.offsetTopAndBottom(circleImageViewTop, offset);

        progressDrawableTop.showArrow(true);

        float endTrim = getEndTrim(totalOffset);
        Log.i("endTrim",String.format("endTrim=%f",endTrim));
        progressDrawableTop.setStartEndTrim(0, endTrim);
        progressDrawableTop.setAlpha(getTopAlpha(totalOffset));
        progressDrawableTop.setRotation(getRotation(totalOffset));
        progressDrawableTop.setArrowScale(getArrowScale(totalOffset));
        Log.i("moveTop", String.format("yOffset=%d,offset=%d", yOffset, offset));
    }

    protected void moveBottomCircleImageView(int yOffset)
    {
        int totalOffset = getBottomOffset();
        if (totalOffset >= maxOffset && yOffset <= 0)
        {
            return;
        }

        int offset = (int) (yOffset * getOffsetPercent(totalOffset) + 0.5f);
        ViewCompat.offsetTopAndBottom(circleImageViewBottom, offset);

        progressDrawableBottom.showArrow(true);
        progressDrawableBottom.setStartEndTrim(0, getEndTrim(totalOffset));
        progressDrawableBottom.setAlpha(getTopAlpha(totalOffset));
        progressDrawableBottom.setRotation(getRotation(totalOffset));
        progressDrawableBottom.setArrowScale(getArrowScale(totalOffset));
        Log.i("moveBottom", String.format("yOffset=%d,offset=%d", yOffset, offset));
    }

    protected void finishTop()
    {
        int totalOffset = getTopTotalOffset();
        if (totalOffset == 0)
        {
            return;
        }

        this.progressDrawableTop.showArrow(false);
        this.isAutoMoving = true;

        moveAnimation.reset();
        moveAnimation.setDuration(ANIMATE_TO_START_DURATION);
        moveAnimation.setInterpolator(new DecelerateInterpolator(2f));
        if (totalOffset < executeOffset)
        {
            //还未达到执行刷新的距离
            moveAnimation.setup(circleImageViewTop,-this.circleHeight);
            circleImageViewTop.clearAnimation();
            circleImageViewTop.setAnimationListener(new HasMoveToStartPositionListener());
            circleImageViewTop.startAnimation(moveAnimation);
        }
        else
        {
            //执行刷新
            moveAnimation.setup(circleImageViewTop,this.executeOffset - this.circleHeight);
            circleImageViewTop.clearAnimation();
            circleImageViewTop.setAnimationListener(new HasMoveToCorrectTopPositionListener());
            circleImageViewTop.startAnimation(moveAnimation);
        }
    }

    protected void finishBottom()
    {
        int totalOffset = getBottomOffset();
        if (totalOffset == 0)
        {
            return;
        }

        this.progressDrawableBottom.showArrow(false);
        this.isAutoMoving = true;

        moveAnimation.reset();
        moveAnimation.setDuration(ANIMATE_TO_START_DURATION);
        moveAnimation.setInterpolator(new DecelerateInterpolator(2f));
        if (totalOffset < executeOffset)
        {
            //还未达到执行加载更多的距离
            moveAnimation.setup(circleImageViewBottom,getHeight());
            circleImageViewBottom.clearAnimation();
            circleImageViewBottom.setAnimationListener(new HasMoveToStartPositionListener());
            circleImageViewBottom.startAnimation(moveAnimation);
        }
        else
        {
            //执行加载更多
            moveAnimation.setup(circleImageViewBottom,getHeight() - this.executeOffset);
            circleImageViewBottom.clearAnimation();
            circleImageViewBottom.setAnimationListener(new HasMoveToCorrectBottomPositionListener());
            circleImageViewBottom.startAnimation(moveAnimation);
        }
    }

    public boolean canChildScrollUp()
    {
        return ViewCompat.canScrollVertically(mainChild, -1);
    }

    public boolean canChildScrollDown()
    {
        return ViewCompat.canScrollVertically(mainChild, 1);
    }

    public int getExecuteOffset()
    {
        return executeOffset;
    }

    public void setExecuteOffset(int executeOffset)
    {
        this.executeOffset = executeOffset;
    }

    public int getMaxOffset()
    {
        return maxOffset;
    }

    public void setMaxOffset(int maxOffset)
    {
        this.maxOffset = maxOffset;
    }

    public boolean isRefreshEnabled()
    {
        return isRefreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled)
    {
        isRefreshEnabled = refreshEnabled;
    }

    public boolean isLoadMoreEnabled()
    {
        return isLoadMoreEnabled;
    }

    public void setLoadMoreEnabled(boolean loadMoreEnabled)
    {
        isLoadMoreEnabled = loadMoreEnabled;
    }

    public boolean isRefreshing()
    {
        return this.isRefreshing;
    }

    public void setRefreshing(boolean refreshing)
    {
        if (this.isRefreshing == refreshing)
        {
            return;
        }
        if (refreshing)
        {
            this.isRefreshing = true;
            ViewCompat.offsetTopAndBottom(circleImageViewTop, executeOffset - circleImageViewTop.getTop());

            ScaleAnimation animation = new ScaleAnimation(0f,1f,0f,1f, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            animation.setDuration(150);

            circleImageViewTop.clearAnimation();
            circleImageViewTop.setAnimationListener(new HasScaleUpTopListener());
            circleImageViewTop.startAnimation(animation);
        }
        else
        {
            this.isRefreshing = false;
            ScaleAnimation animation = new ScaleAnimation(1f,0f,1f,0f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            animation.setDuration(150);

            circleImageViewTop.clearAnimation();
            circleImageViewTop.setAnimationListener(new HasScaleDownTopListener());
            circleImageViewTop.startAnimation(animation);
        }
    }

    public boolean isLoadingMore()
    {
        return isLoadingMore;
    }

    public void setLoadingMore(boolean loadingMore)
    {
        if (this.isLoadingMore == loadingMore)
        {
            return;
        }
        if (loadingMore)
        {
            this.isLoadingMore = true;
            ViewCompat.offsetTopAndBottom(circleImageViewBottom, getHeight() - executeOffset - circleImageViewBottom.getTop());

            ScaleAnimation animation = new ScaleAnimation(0f,1f,0f,1f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            animation.setDuration(150);

            circleImageViewBottom.clearAnimation();
            circleImageViewBottom.setAnimationListener(new HasScaleUpBottomListener());
            circleImageViewBottom.startAnimation(animation);
        }
        else
        {
            this.isLoadingMore = false;
            ScaleAnimation animation = new ScaleAnimation(1f,0f,1f,0f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            animation.setDuration(150);

            circleImageViewBottom.clearAnimation();
            circleImageViewBottom.setAnimationListener(new HasScaleDownBottomListener());
            circleImageViewBottom.startAnimation(animation);
        }
    }

    public void setProgressBackgroundColorSchemeResource(@ColorRes int colorRes)
    {
        setProgressBackgroundColorSchemeColor(getResources().getColor(colorRes));
    }

    public void setProgressBackgroundColorSchemeColor(@ColorInt int color)
    {
        circleImageViewTop.setBackgroundColor(color);
        circleImageViewBottom.setBackgroundColor(color);
        progressDrawableTop.setBackgroundColor(color);
        progressDrawableBottom.setBackgroundColor(color);
    }

    public void setColorSchemeResources(@ColorRes int... colorResIds) {
        final Resources res = getResources();
        int[] colorRes = new int[colorResIds.length];
        for (int i = 0; i < colorResIds.length; i++)
        {
            colorRes[i] = res.getColor(colorResIds[i]);
        }
        setColorSchemeColors(colorRes);
    }

    @ColorInt
    public void setColorSchemeColors(int... colors)
    {
        progressDrawableTop.setColorSchemeColors(colors);
        progressDrawableBottom.setColorSchemeColors(colors);
    }

    public OnRefreshListener getOnRefreshListener()
    {
        return onRefreshListener;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener)
    {
        this.onRefreshListener = onRefreshListener;
    }

    public OnLoadMoreListener getOnLoadMoreListener()
    {
        return onLoadMoreListener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener)
    {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void reset()
    {
        circleImageViewTop.clearAnimation();
        circleImageViewBottom.clearAnimation();
        progressDrawableTop.stop();
        progressDrawableTop.setAlpha(255);
        progressDrawableBottom.stop();
        progressDrawableBottom.setAlpha(255);
        ViewCompat.offsetTopAndBottom(circleImageViewTop,-circleHeight - circleImageViewTop.getTop());
        ViewCompat.offsetTopAndBottom(circleImageViewBottom,getHeight() - circleImageViewBottom.getTop());
    }

    private int dipToPx(float dp)
    {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private boolean isTopCircleShowing()
    {
        return circleImageViewTop.getTop() > -this.circleHeight;
    }

    private boolean isBottomCircleShowing()
    {
        return circleImageViewBottom.getTop() < getHeight();
    }

    private int getTopTotalOffset()
    {
        int currentTop = circleImageViewTop.getTop();
        int totalOffset = currentTop + this.circleHeight;
        return totalOffset;
    }

    private int getBottomOffset()
    {
        int currentTop = circleImageViewBottom.getTop();
        int totalOffset = getHeight() - currentTop;
        return totalOffset;
    }

    private float getOffsetPercent(int currentOffset)
    {
        if (currentOffset < executeOffset)
        {
            return 1f;
        }

        return 0.5f;
    }

    private int getTopAlpha(int currentOffset)
    {
        if (currentOffset >= executeOffset)
        {
            return 255;
        }

        int alpha = MIN_ALPHA + (int) ((200f - MIN_ALPHA)/ executeOffset * currentOffset);
        Log.i("getTopAlpha","Yes " + alpha);
        return alpha;
    }

    private int getRotation(int currentOffset)
    {
        if (currentOffset <= executeOffset)
        {
            return (int) (360 * (float)currentOffset/(float) executeOffset) - 180;
        }

        return (int) (90 * (float)(currentOffset - executeOffset)/(float)(maxOffset - executeOffset)) - 180;
    }

    private float getEndTrim(int currentOffset)
    {
        if (currentOffset >= executeOffset)
        {
            return 0.8f;
        }

        return (float) currentOffset/(float) executeOffset * 0.8f;
    }

    private float getArrowScale(int currentOffset)
    {
        if (currentOffset >= executeOffset)
        {
            return 1.0f;
        }

        if (currentOffset < this.circleHeight)
        {
            return 0f;
        }

        return (float) (currentOffset - this.circleHeight) / (float) (executeOffset - this.circleHeight);
    }

    class MoveAnimation extends Animation
    {
        private View target;
        private int startTop;
        private int endTop;
        private int a;//斜率

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t)
        {
            int offset = ((int) (this.a * interpolatedTime) + startTop) - target.getTop();
            ViewCompat.offsetTopAndBottom(target,offset);
        }

        public void setup(View target, int endTop)
        {
            this.target = target;
            this.endTop = endTop;
            this.startTop = target.getTop();
            this.a = this.endTop - this.startTop;
        }
    }

    class HasMoveToCorrectTopPositionListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            isAutoMoving = false;
            isRefreshing = true;
            progressDrawableTop.setStartEndTrim(0,0.8f);
            progressDrawableTop.setRotation(0);
            progressDrawableTop.showArrow(false);
            progressDrawableTop.start();
            if (onRefreshListener != null)
            {
                onRefreshListener.onRefresh();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    }

    class HasMoveToCorrectBottomPositionListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            isAutoMoving = false;
            isLoadingMore = true;
            progressDrawableBottom.setStartEndTrim(0,0.8f);
            progressDrawableBottom.setRotation(0);
            progressDrawableBottom.showArrow(false);
            progressDrawableBottom.start();
            if (onLoadMoreListener != null)
            {
                onLoadMoreListener.onLoadMore();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    }

    class HasMoveToStartPositionListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            isAutoMoving = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    }

    class HasScaleDownTopListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            isRefreshing = false;
            reset();
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    }

    class HasScaleDownBottomListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            isLoadingMore = false;
            reset();
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    }

    class HasScaleUpTopListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            isRefreshing = true;
            progressDrawableTop.start();
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    }

    class HasScaleUpBottomListener implements Animation.AnimationListener
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            isLoadingMore = true;
            progressDrawableBottom.start();
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    }

    public interface OnRefreshListener
    {
        void onRefresh();
    }

    public interface OnLoadMoreListener
    {
        void onLoadMore();
    }
}
