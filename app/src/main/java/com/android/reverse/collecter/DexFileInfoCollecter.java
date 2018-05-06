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

    private static PathClassLoader pathClassLoader;
    private static HashMap<String, DexFileInfo> dynLoadedDexInfo = new HashMap<String, DexFileInfo>();
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


//        private static native java.lang.Object dalvik.system.DexFile.openDexFileNative(java.lang.String,java.lang.String,int,java.lang.ClassLoader,dalvik.system.DexPathList$Element[])

        if (Build.VERSION.SDK_INT > 22) {

            Method art_23_openDexFileNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "openDexFileNative",
                    String.class, String.class, int.class, ClassLoader.class, Class.forName("[Ldalvik.system.DexPathList$Element;"));

            hookhelper.hookMethod(art_23_openDexFileNativeMethod, new MethodHookCallBack() {
                @Override
                public void beforeHookedMethod(HookParam param) {

                }

                @Override
                public void afterHookedMethod(HookParam param) {

                    String dexPath = (String) param.args[0];
                    Object mCookie = param.getResult();

                    long[] longs = (long[]) mCookie;

//						constexpr size_t kOatFileIndex = 0;
//						constexpr size_t kDexFileIndexStart = 1;

                    for (int index = 1; index < longs.length; ++index) {
                        if (longs[index] != 0) {
                            dynLoadedDexInfo.put(mCookie + "", new DexFileInfo(dexPath, longs[index]));
                        }
                        Logger.log("openDexFileNative() is invoked with filepath:" + param.args[0] + " result: long[" + index + "]" + longs[index]);

                    }


                }
            });


        } else {

            Method openDexFileNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "openDexFileNative",
                    String.class, String.class, int.class);

            hookhelper.hookMethod(openDexFileNativeMethod, new MethodHookCallBack() {

                @Override
                public void beforeHookedMethod(HookParam param) {

                }

                @Override
                public void afterHookedMethod(HookParam param) {
                    String dexPath = (String) param.args[0];
                    Object mCookie = param.getResult();

                    //long or int

                    if (Long.parseLong(mCookie.toString()) != 0) {
                        dynLoadedDexInfo.put(mCookie + "", new DexFileInfo(dexPath, Long.parseLong(mCookie.toString())));
                    }
                    Logger.log("openDexFileNative() is invoked with filepath:" + param.args[0] + " result:" + Long.parseLong(mCookie.toString()));

                }
            });

        }


        if (Build.VERSION.SDK_INT > 22) {

//            private static native Class defineClassNative(String name, ClassLoader loader, Object cookie,DexFile dexFile)


            Method art_23_defineClassNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "defineClassNative",
                    String.class, ClassLoader.class, Object.class, DexFile.class);
            hookhelper.hookMethod(art_23_defineClassNativeMethod, new MethodHookCallBack() {

                @Override
                public void beforeHookedMethod(HookParam param) {

                }

                @Override
                public void afterHookedMethod(HookParam param) {
                    if (!param.hasThrowable()) {
                        long[] longs = (long[]) param.args[2];
                        for (int index = 1; index < longs.length; ++index) {
                            if (longs[index] != 0) {
                                setDefineClassLoader(longs[index], (ClassLoader) param.args[1]);
                            }
                        }
                    }
                }
            });

        } else if (Build.VERSION.SDK_INT > 19) {

            Method art_20_defineClassNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "defineClassNative",
                    String.class, ClassLoader.class, long.class);
            hookhelper.hookMethod(art_20_defineClassNativeMethod, new MethodHookCallBack() {

                @Override
                public void beforeHookedMethod(HookParam param) {

                }

                @Override
                public void afterHookedMethod(HookParam param) {
                    if (!param.hasThrowable()) {
                        long mCookie = (long) param.args[2];
                        setDefineClassLoader(mCookie, (ClassLoader) param.args[1]);
                    }
                }
            });

        } else {


            Method defineClassNativeMethod = RefInvoke.findMethodExact("dalvik.system.DexFile", ClassLoader.getSystemClassLoader(), "defineClassNative",
                    String.class, ClassLoader.class, int.class);
            hookhelper.hookMethod(defineClassNativeMethod, new MethodHookCallBack() {

                @Override
                public void beforeHookedMethod(HookParam param) {

                }

                @Override
                public void afterHookedMethod(HookParam param) {
                    if (!param.hasThrowable()) {
                        int mCookie = (Integer) param.args[2];
                        setDefineClassLoader(mCookie, (ClassLoader) param.args[1]);
                    }
                }
            });
        }


    }

    public HashMap<String, DexFileInfo> dumpDexFileInfo() {
        HashMap<String, DexFileInfo> dexs = new HashMap<String, DexFileInfo>(dynLoadedDexInfo);
        Object dexPathList = RefInvoke.getFieldOjbect("dalvik.system.BaseDexClassLoader", pathClassLoader, "pathList");
        Object[] dexElements = (Object[]) RefInvoke.getFieldOjbect("dalvik.system.DexPathList", dexPathList, "dexElements");
        DexFile dexFile = null;
        for (int i = 0; i < dexElements.length; i++) {
            dexFile = (DexFile) RefInvoke.getFieldOjbect("dalvik.system.DexPathList$Element", dexElements[i], "dexFile");
            String mFileName = (String) RefInvoke.getFieldOjbect("dalvik.system.DexFile", dexFile, "mFileName");
            Object mCookie = RefInvoke.getFieldOjbect("dalvik.system.DexFile", dexFile, "mCookie");
            if (mCookie instanceof long[]) {
                long[] longs = (long[]) mCookie;

//						constexpr size_t kOatFileIndex = 0;
//						constexpr size_t kDexFileIndexStart = 1;

                for (int index = 1; index < longs.length; ++index) {
                    DexFileInfo dexinfo = new DexFileInfo(mFileName, longs[index], dexElements[i].toString(), pathClassLoader);
                    dexs.put(longs[index] + "", dexinfo);
                }
            } else {
                //long or int
                DexFileInfo dexinfo = new DexFileInfo(mFileName, Long.parseLong(mCookie.toString()), dexElements[i].toString(), pathClassLoader);
//			dexs.put(mFileName, dexinfo);
                dexs.put(mCookie + "", dexinfo);//改用
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

    public void dumpDexFile(String filename, String mCookie_str) {
        File file = new File(filename);
        try {
            if (!file.exists())
                file.createNewFile();
            long mCookie = Long.parseLong(mCookie_str);
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
