package com.android.reverse.mod;

import android.content.pm.ApplicationInfo;

import com.android.reverse.apimonitor.ApiMonitorHookManager;
import com.android.reverse.collecter.DexFileInfoCollecter;
import com.android.reverse.collecter.LuaScriptInvoker;
import com.android.reverse.collecter.ModuleContext;
import com.android.reverse.util.Logger;


import java.io.File;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import top.imlk.xpmodulemultidex.XMMultiDex;

public class ReverseXposedModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String ZJDROID_PACKAGENAME = "com.android.reverse";

    public static String MODULE_PATH;


    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {


        XMMultiDex.install(ReverseXposedModule.class.getClassLoader(),MODULE_PATH,lpparam.appInfo);


//        addNativeLibDic();



        // TODO Auto-generated method stub
        if (lpparam.appInfo == null ||
                (lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
            return;
        } else if (lpparam.isFirstApplication && !ZJDROID_PACKAGENAME.equals(lpparam.packageName)) {
            Logger.PACKAGENAME = lpparam.packageName;
            Logger.log("the package = " + lpparam.packageName + " has hook");
            Logger.log("the app target id = " + android.os.Process.myPid());
            PackageMetaInfo pminfo = PackageMetaInfo.fromXposed(lpparam);
            ModuleContext.getInstance().initModuleContext(pminfo);
            DexFileInfoCollecter.getInstance().start();
            LuaScriptInvoker.getInstance().start();
            ApiMonitorHookManager.getInstance().startMonitor();
        } else {

        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;

    }


    public void addNativeLibDic() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {


        java.lang.reflect.Field pathListField = Class.forName("dalvik.system.BaseDexClassLoader").getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathList = pathListField.get(this.getClass().getClassLoader());

        java.lang.reflect.Field nativeLibraryDirectoriesField = Class.forName("dalvik.system.DexPathList").getDeclaredField("nativeLibraryDirectories");
        nativeLibraryDirectoriesField.setAccessible(true);

        if (nativeLibraryDirectoriesField.getType().isArray()) {

            File[] oldFiles = (File[]) nativeLibraryDirectoriesField.get(pathList);

            File[] newFiles = new File[oldFiles.length + 1];

            newFiles[0] = new File(MODULE_PATH + "!/lib/armeabi");

            for (int i = 0; i < oldFiles.length; ++i) {

                newFiles[i + 1] = oldFiles[i];
            }
            nativeLibraryDirectoriesField.set(pathList, newFiles);

        } else if (nativeLibraryDirectoriesField.getType() == List.class) {

            List nativeLibrarys = (List) nativeLibraryDirectoriesField.get(pathList);

            nativeLibrarys.add(new File(MODULE_PATH + "!/lib/armeabi"));


        } else {
            XposedBridge.log("ZjDroid: bad nativeLibraryDirectories type -> " + nativeLibraryDirectoriesField.getType());
        }

    }
}
