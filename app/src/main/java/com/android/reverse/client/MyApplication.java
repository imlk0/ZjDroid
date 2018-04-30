package com.android.reverse.client;

import android.app.Application;
import android.support.multidex.MultiDex;

/**
 * Created by imlk on 2018/4/27.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);

    }
}
