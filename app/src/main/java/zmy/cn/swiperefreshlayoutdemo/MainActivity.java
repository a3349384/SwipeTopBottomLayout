package zmy.cn.swiperefreshlayoutdemo;

import android.content.Context;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import cn.zmy.lib.SwipeTopBottomLayout;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity
{
    private RecyclerView recyclerView;
    private SwipeTopBottomLayout mySwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySwipeRefreshLayout = (SwipeTopBottomLayout) findViewById(R.id.swipeTopBottomLayout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new TestAdapter(this));

        mySwipeRefreshLayout.setOnRefreshListener(new SwipeTopBottomLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                Observable.create(new Observable.OnSubscribe<Object>()
                {
                    @Override
                    public void call(Subscriber<? super Object> subscriber)
                    {
                        try
                        {
                            Thread.sleep(2000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }

                        subscriber.onNext("");
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Object>()
                        {
                            @Override
                            public void onCompleted()
                            {
                                mySwipeRefreshLayout.setRefreshing(false);
                            }

                            @Override
                            public void onError(Throwable e)
                            {

                            }

                            @Override
                            public void onNext(Object o)
                            {

                            }
                        });
            }
        });

        mySwipeRefreshLayout.setOnLoadMoreListener(new SwipeTopBottomLayout.OnLoadMoreListener()
        {
            @Override
            public void onLoadMore()
            {
                Observable.create(new Observable.OnSubscribe<Object>()
                {
                    @Override
                    public void call(Subscriber<? super Object> subscriber)
                    {
                        try
                        {
                            Thread.sleep(2000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }

                        subscriber.onNext("");
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Object>()
                        {
                            @Override
                            public void onCompleted()
                            {
                                mySwipeRefreshLayout.setLoadingMore(false);
                            }

                            @Override
                            public void onError(Throwable e)
                            {

                            }

                            @Override
                            public void onNext(Object o)
                            {

                            }
                        });
            }
        });

    }

    public static class TestAdapter extends RecyclerView.Adapter
    {
        private Context context;
        private ArrayList<String> items;

        public TestAdapter(Context context)
        {
            this.context = context;
            items = new ArrayList<>();
            for (int i = 0; i < 20; i++)
            {
                items.add("item " + i);
            }
        }

        @Override
        public int getItemCount()
        {
            return items.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            return new ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.item,parent,false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
        {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.textView.setText(items.get(position));
        }

        class ItemViewHolder extends RecyclerView.ViewHolder
        {
            private TextView textView;
            public ItemViewHolder(View itemView)
            {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.textView);
            }
        }

    }
}
