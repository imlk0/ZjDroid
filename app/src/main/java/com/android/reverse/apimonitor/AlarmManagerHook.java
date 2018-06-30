package com.android.reverse.apimonitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.WorkSource;

import com.android.reverse.hook.HookParam;
import com.android.reverse.util.Logger;
import com.android.reverse.util.RefInvoke;


public class AlarmManagerHook extends ApiMonitorHook {

    @Override
    public void startHook() {

        if (Build.VERSION.SDK_INT >= 24) {

//            private void setImpl(@AlarmType int type, long triggerAtMillis, long windowMillis,
//            long intervalMillis, int flags, PendingIntent operation, final OnAlarmListener listener,
//            String listenerTag, Handler targetHandler, WorkSource workSource,
//                    AlarmClockInfo alarmClock) {

            Method setImplmethod = RefInvoke.findMethodExact(
                    "android.app.AlarmManager", ClassLoader.getSystemClassLoader(),
                    "setImpl", int.class, long.class, long.class, long.class, int.class, PendingIntent.class, AlarmManager.OnAlarmListener.class, String.class, Handler.class, WorkSource.class, AlarmManager.AlarmClockInfo.class);
            hookhelper.hookMethod(setImplmethod, new AbstractBahaviorHookCallBack() {

                @Override
                public void descParam(HookParam param) {
                    Logger.log_behavior("The Alarm Information:");
                    PendingIntent intent = (PendingIntent) param.args[5];
                    descPendingIntent(intent);
                    Logger.log_behavior("TriggerAtMillis = " + param.args[1]);
                    Logger.log_behavior("windowMillis = " + param.args[2]);
                    Logger.log_behavior("intervalMillis = " + param.args[3]);
                    Logger.log_behavior("flags = " + param.args[4]);
                    Logger.log_behavior("listener.class = " + param.args[6].getClass().getName());
                    Logger.log_behavior("listenerTag = " + param.args[7]);
                    Logger.log_behavior("Handler.class = " + param.args[8].getClass().getName());

                }
            });


        } else if (Build.VERSION.SDK_INT >= 20) {

            Method setImplmethod = RefInvoke.findMethodExact(
                    "android.app.AlarmManager", ClassLoader.getSystemClassLoader(),
                    "setImpl", int.class, long.class, long.class, long.class, PendingIntent.class, WorkSource.class, AlarmManager.AlarmClockInfo.class);
            hookhelper.hookMethod(setImplmethod, new AbstractBahaviorHookCallBack() {

                @Override
                public void descParam(HookParam param) {
                    Logger.log_behavior("The Alarm Information:");
                    PendingIntent intent = (PendingIntent) param.args[4];
                    descPendingIntent(intent);
                    Logger.log_behavior("TriggerAtMillis = " + param.args[1]);
                    Logger.log_behavior("windowMillis = " + param.args[2]);
                    Logger.log_behavior("intervalMillis = " + param.args[3]);

                }
            });
        } else {


            Method setImplmethod = RefInvoke.findMethodExact(
                    "android.app.AlarmManager", ClassLoader.getSystemClassLoader(),
                    "setImpl", int.class, long.class, long.class, long.class, PendingIntent.class, WorkSource.class);
            hookhelper.hookMethod(setImplmethod, new AbstractBahaviorHookCallBack() {

                @Override
                public void descParam(HookParam param) {
                    Logger.log_behavior("The Alarm Information:");
                    PendingIntent intent = (PendingIntent) param.args[4];
                    descPendingIntent(intent);
                    Logger.log_behavior("TriggerAtMillis = " + param.args[1]);
                    Logger.log_behavior("windowMillis = " + param.args[2]);
                    Logger.log_behavior("intervalMillis = " + param.args[3]);

                }
            });
        }

    }

    private void descPendingIntent(PendingIntent pintent) {
        Method getIntentMethod = RefInvoke.findMethodExact(
                "android.app.PendingIntent", ClassLoader.getSystemClassLoader(),
                "getIntent");
        try {
            Intent intent = (Intent) getIntentMethod.invoke(pintent, new Object[]{});
            ComponentName cn = intent.getComponent();
            if (cn != null) {
                Logger.log_behavior("The ComponentName = " + cn.getPackageName() + "/" + cn.getClassName());
            }
            Logger.log_behavior("The Intent Action = " + intent.getAction());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

}
