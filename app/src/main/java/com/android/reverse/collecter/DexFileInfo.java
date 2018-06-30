package com.android.reverse.collecter;

public class DexFileInfo {

    private String dexPath;
    private long mCookie;
    private ClassLoader defineClassLoader;
    private String toStringResult;

    public DexFileInfo(String dexPath, long mCookie) {
        super();
        this.dexPath = dexPath;
        this.mCookie = mCookie;
    }

    public DexFileInfo(String dexPath, long mCookie, String toStringResult) {
        this(dexPath,mCookie);
        this.toStringResult = toStringResult;
    }

    public DexFileInfo(String dexPath, long mCookie, String toStringResult, ClassLoader classLoader) {
        this(dexPath, mCookie, toStringResult);
        this.defineClassLoader = classLoader;
    }

    public String getDexPath() {
        return dexPath;
    }

    public long getmCookie() {
        return mCookie;
    }

    public void setmCookie(long mCookie) {
        this.mCookie = mCookie;
    }

    public ClassLoader getDefineClassLoader() {
        return defineClassLoader;
    }

    public void setDefineClassLoader(ClassLoader defineClassLoader) {
        this.defineClassLoader = defineClassLoader;
    }

    public void setDexPath(String dexPath) {
        this.dexPath = dexPath;
    }


    public String getToStringResult() {
        return toStringResult;
    }
}
