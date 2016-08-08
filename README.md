# 效果预览
# Gradle

```
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
        }
}

dependencies {
    compile 'com.github.a3349384:SwipeTopBottomLayout:-SNAPSHOT'
}
```

# 使用说明
## 设置下拉刷新监听器：

```
setOnRefreshListener();
```
## 设置上拉加载更多监听器：

```
setOnLoadMoreListener();
```

## 下拉刷新完成：

```
setRefreshing(false);
```
## 上拉加载更多完成：

```
setLoadingMore(false);
```
可以主动调用显示刷新、加载更多动画：

```
setRefreshing(true);
setLoadingMore(true);
```
# 注意事项
1、SwipeTopButtomRefresh中应只放一个Child View，并且该Child View必须实现了“NestedScrollingChild”接口。对于非“NestedScrollingChild”类型的Child，用SwipeRefreshLayout足以满足要求。
