package com.android.reverse.collecter;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;

import com.android.reverse.hook.HookHelperFacktory;
import com.android.reverse.hook.HookHelperInterface;
import com.android.reverse.hook.HookParam;
import com.android.reverse.hook.MethodHookCallBack;
import com.android.reverse.smali.MemoryBackSmali;
import com.android.reverse.util.Logger;
import com.android.reverse.util.NativeFunction;
import com.android.reverse.util.RefInvoke;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

public class DexFileInfoCollecter {

    private static PathClassLoader pathClassLoader;// TODO 测试自定义Classloader的应用，如有热修复的QQ
    private static HashMap<Long, DexFileInfo> dynLoadedDexInfo = new HashMap<Long, DexFileInfo>();
    private static DexFileInfoCollecter collecter;
    private HookHelperInterface hookhelper = HookHelperFacktory.getHookHelper();
    private final static String DVMLIB_LIB = "dvmnative";

    private DexFileInfoCollecter() {

    }

    public static DexFileInfoCollecter getInstance() {
        if (collecter == null)
            collecter = new DexFileInfoCollecter();
        return collecter;
    }

    public void start() throws Throwable {

        pathClassLoader = (PathClassLoader) ModuleContext.getInstance().getBaseClassLoader();


        hookdefineClassNativeMethod();
        hookOpenDexFileNativeMethod();

    }


    public void hookdefineClassNativeMethod() {
        Method defineClassNativeMethod;

//            (Build.VERSION.SDK_INT >= 23)
//            private static native Class defineClassNative(String name, ClassLoader loader, Object cookie,DexFile dexFile)
        defineClassNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "defineClassNative",
                String.class, ClassLoader.class, Object.class, DexFile.class);

        if (defineClassNativeMethod == null) {
//                (Build.VERSION.SDK_INT >= 20)
            defineClassNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "defineClassNative",
                    String.class, ClassLoader.class, long.class);
        }

        if (defineClassNativeMethod == null) {
//            dalvik
            defineClassNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "defineClassNative",
                    String.class, ClassLoader.class, int.class);
        }

        if (defineClassNativeMethod == null) {
            Logger.log("error at " + DexFileInfoCollecter.class.getName() + "#" + "hookOpenDexFileNativeMethod() :");
            Logger.log("unable to find suit method to hook, may be the Zjdroid is too old");
        }

        hookhelper.hookMethod(defineClassNativeMethod, new MethodHookCallBack() {
            @Override
            public void beforeHookedMethod(HookParam param) {

            }

            @Override
            public void afterHookedMethod(HookParam param) {
                if (!param.hasThrowable()) {

                    long[] mCookies = parseMCookies(param.args[2]);
                    if (mCookies != null) {
                        for (int index = 0; index < mCookies.length; ++index) {
                            if (mCookies[index] != 0) {
                                setDefineClassLoader(mCookies[index], (ClassLoader) param.args[1]);
                            }
                        }
                    }
                }
            }

        });

    }

    public void hookOpenDexFileNativeMethod() {

        Method openDexFileNativeMethod = null;

        try {
//          (Build.VERSION.SDK_INT >= 23) art
//          private static native java.lang.Object dalvik.system.DexFile.openDexFileNative(java.lang.String,java.lang.String,int,java.lang.ClassLoader,dalvik.system.DexPathList$Element[])
            openDexFileNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "openDexFileNative",
                    String.class, String.class, int.class, ClassLoader.class, Class.forName("[Ldalvik.system.DexPathList$Element;"));
        } catch (ClassNotFoundException e) {
        }

//        dalvik
        if (openDexFileNativeMethod == null) {
            openDexFileNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "openDexFileNative",
                    String.class, String.class, int.class);
        }

        if (openDexFileNativeMethod == null) {
            Logger.log("error at " + DexFileInfoCollecter.class.getName() + "#" + "hookOpenDexFileNativeMethod() :");
            Logger.log("unable to find suit method to hook, may be the Zjdroid is too old");
        } else {

            hookhelper.hookMethod(openDexFileNativeMethod, new MethodHookCallBack() {
                @Override
                public void beforeHookedMethod(HookParam param) {

                }

                @Override
                public void afterHookedMethod(HookParam param) {

                    String dexPath = (String) param.args[0];

                    long[] mCookies = parseMCookies(param.getResult());
                    if (mCookies != null) {
                        for (int index = 0; index < mCookies.length; ++index) {
                            if (mCookies[index] != 0) {
                                dynLoadedDexInfo.put(mCookies[index], new DexFileInfo(dexPath, mCookies[index]));
                            }
                            Logger.log("openDexFileNative() is invoked with filepath:" + param.args[0] + " result: long[" + index + "]" + mCookies[index]);
                        }
                    }
                }
            });
        }
    }

    /**
     * @param mCookies
     * @return 所有可用的dexFile的mCookie
     */

    public static long[] parseMCookies(Object mCookies) {

        if (mCookies instanceof Integer) {
            return new long[]{(Integer) mCookies};
        } else if (mCookies instanceof Long) {
            return new long[]{(Long) mCookies};
        } else if (mCookies instanceof long[]) {
            long[] cookies = ((long[]) mCookies);
            long[] longs = new long[cookies.length - 1];
//            sdk23开始的art虚拟机中，mCookie为long[],其中第一个为oatFile，余下的为(o)dexFile
//            摘自源码：
//            constexpr size_t kOatFileIndex = 0;
//            constexpr size_t kDexFileIndexStart = 1;
            System.arraycopy(cookies, 1, longs, 0, longs.length);
            return longs;
        } else {
            //没有满足的情况
            Logger.log("bad mCookies at " + DexFileInfoCollecter.class.getName() + "#" + "parseMCookies(Object) :" + mCookies);
            return null;
        }


    }

    public HashMap<Long, DexFileInfo> dumpDexFileInfo() {
        HashMap<Long, DexFileInfo> dexs = new HashMap<Long, DexFileInfo>(dynLoadedDexInfo);
        Object dexPathList = RefInvoke.getFieldOjbect("dalvik.system.BaseDexClassLoader", pathClassLoader, "pathList");
        Object[] dexElements = (Object[]) RefInvoke.getFieldOjbect("dalvik.system.DexPathList", dexPathList, "dexElements");
        DexFile dexFile = null;
        for (int i = 0; i < dexElements.length; i++) {
            dexFile = (DexFile) RefInvoke.getFieldOjbect("dalvik.system.DexPathList$Element", dexElements[i], "dexFile");
            String mFileName = (String) RefInvoke.getFieldOjbect("dalvik.system.DexFile", dexFile, "mFileName");
            Object mCookie = RefInvoke.getFieldOjbect("dalvik.system.DexFile", dexFile, "mCookie");

            long[] mCookies = parseMCookies(mCookie);
            if (mCookies != null) {

                for (int index = 0; index < mCookies.length; ++index) {
                    if (mCookies[index] != 0) {
                        DexFileInfo dexinfo = new DexFileInfo(mFileName, mCookies[index], dexElements[i].toString(), pathClassLoader);
                        dexs.put(mCookies[index], dexinfo);
                    }
                }
            }
        }
        return dexs;
    }

    public String[] dumpLoadableClass(String dexPath) {
        int mCookie = Integer.parseInt(dexPath);
//		int mCookie = this.getCookie(dexPath);
        if (mCookie != 0) {
            return (String[]) RefInvoke.invokeStaticMethod("dalvik.system.DexFile", "getClassNameList", new Class[]{int.class},
                    new Object[]{mCookie});
        } else {
            Logger.log("the cookie is not right");
        }
        return null;

    }

    public void backsmaliDexFile(String filename, String dexPath) {
        File file = new File(filename);
        try {
            if (!file.exists())
                file.createNewFile();

            MemoryBackSmali.disassembleDexFile(Long.parseLong(dexPath), filename);
//			int mCookie = this.getCookie(dexPath);
//			if (mCookie != 0) {
//				MemoryBackSmali.disassembleDexFile(mCookie, filename);
//			} else {
//				Logger.log("the cookie is not right");
//			}


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dumpDexFile(String filename, long mCookie) {
        File file = new File(filename);
        try {
            if (!file.exists())
                file.createNewFile();
//			int mCookie = this.getCookie(dexPath);
            if (mCookie != 0) {
                FileOutputStream out = new FileOutputStream(file);
                ByteBuffer data = NativeFunction.dumpDexFileByCookie(mCookie, ModuleContext.getInstance().getApiLevel());
                data.order(ByteOrder.LITTLE_ENDIAN);
                byte[] buffer = new byte[8192];
                data.clear();
                while (data.hasRemaining()) {
                    int count = Math.min(buffer.length, data.remaining());
                    data.get(buffer, 0, count);
                    try {
                        out.write(buffer, 0, count);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                Logger.log("the cookie is not right");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 已被废弃
     *
     * @param dexPath
     * @return
     */
    private Object getCookie(String dexPath) {

        if (dynLoadedDexInfo.containsKey(dexPath)) {
            DexFileInfo dexFileInfo = dynLoadedDexInfo.get(dexPath);
            return dexFileInfo.getmCookie();
        } else {
            Object dexPathList = RefInvoke.getFieldOjbect("dalvik.system.BaseDexClassLoader", pathClassLoader, "pathList");
            Object[] dexElements = (Object[]) RefInvoke.getFieldOjbect("dalvik.system.DexPathList", dexPathList, "dexElements");
            DexFile dexFile = null;
            for (int i = 0; i < dexElements.length; i++) {
                dexFile = (DexFile) RefInvoke.getFieldOjbect("dalvik.system.DexPathList$Element", dexElements[i], "dexFile");
                String mFileName = (String) RefInvoke.getFieldOjbect("dalvik.system.DexFile", dexFile, "mFileName");
                if (mFileName.equals(dexPath)) {
                    return RefInvoke.getFieldOjbect("dalvik.system.DexFile", dexFile, "mCookie");
                }

            }
            return 0;
        }

    }

    private void setDefineClassLoader(long mCookie, ClassLoader classLoader) {
        Iterator<DexFileInfo> dexinfos = dynLoadedDexInfo.values().iterator();
        DexFileInfo info = null;
        while (dexinfos.hasNext()) {
            info = dexinfos.next();
            if (mCookie == info.getmCookie()) {
                if (info.getDefineClassLoader() == null)
                    info.setDefineClassLoader(classLoader);
            }
        }
    }

}
