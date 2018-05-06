package com.android.reverse.apimonitor;

import android.os.Build;

import java.lang.reflect.Method;


import com.android.reverse.hook.HookParam;
import com.android.reverse.util.Logger;
import com.android.reverse.util.RefInvoke;

public class ConnectivityManagerHook extends ApiMonitorHook {

    @Override
    public void startHook() {

        if (Build.VERSION.SDK_INT <= 19) {

            Method setMobileDataEnabledmethod = RefInvoke.findMethodExact(
                    "android.net.ConnectivityManager", ClassLoader.getSystemClassLoader(),
                    "setMobileDataEnabled", boolean.class);
            hookhelper.hookMethod(setMobileDataEnabledmethod, new AbstractBahaviorHookCallBack() {

                @Override
                public void descParam(HookParam param) {
                    boolean status = (Boolean) param.args[0];
                    Logger.log("Set MobileDataEnabled = " + status);
                }
            });

        }

        
    }

}
