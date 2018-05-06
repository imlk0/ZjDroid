package com.android.reverse.apimonitor;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import com.android.reverse.hook.HookParam;
import com.android.reverse.util.Logger;
import com.android.reverse.util.RefInvoke;


public class NetWorkHook extends ApiMonitorHook {

	@Override
	public void startHook() {
		// HttpURLConnection
		Method openConnectionMethod = RefInvoke.findMethodExact("java.net.URL", ClassLoader.getSystemClassLoader(), "openConnection");
		hookhelper.hookMethod(openConnectionMethod, new AbstractBahaviorHookCallBack() {
			@Override
			public void descParam(HookParam param) {
				URL url = (URL) param.thisObject;
				Logger.log_behavior("Connect to URL ->");
				Logger.log_behavior("The URL = " + url.toString());
			}
		});

		if(Build.VERSION.SDK_INT < 23){
			httpHook = new ApacheHttpHook();

			httpHook.startHook();

		}



	}

	public ApiMonitorHook httpHook;



}
