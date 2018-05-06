package com.android.reverse.apimonitor;

import java.lang.reflect.Method;
import com.android.reverse.hook.HookParam;
import com.android.reverse.util.Logger;
import com.android.reverse.util.RefInvoke;

public class AudioRecordHook extends ApiMonitorHook {

	@Override
	public void startHook() {
		Method startRecordingMethod = RefInvoke.findMethodExact(
				"android.media.AudioRecord", ClassLoader.getSystemClassLoader(),
				"startRecording");
		hookhelper.hookMethod(startRecordingMethod, new AbstractBahaviorHookCallBack() {
			
			@Override
			public void descParam(HookParam param) {
				Logger.log_behavior("Audio Recording ->");
			}
		});
		
	}

}
