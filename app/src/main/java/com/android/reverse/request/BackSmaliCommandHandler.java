package com.android.reverse.request;

import android.os.Build;

import com.android.reverse.collecter.DexFileInfoCollecter;
import com.android.reverse.collecter.ModuleContext;
import com.android.reverse.util.Logger;

public class BackSmaliCommandHandler implements CommandHandler {

    private String mCookie;

    public BackSmaliCommandHandler(String mCookie) {
        this.mCookie = mCookie;
    }

    @Override
    public void doAction() {

        if (Build.VERSION.SDK_INT > 19) {
            //TODO 增加art虚拟机中backsmali的支持
            Logger.log("Forbidden ! in Art, ZjDroid was unable to back smali dex file automaticly!");
            return;
        }

        String filename = ModuleContext.getInstance().getAppContext().getFilesDir() + "/dexfile" + mCookie + ".dex";
        DexFileInfoCollecter.getInstance().backsmaliDexFile(filename, mCookie);
        Logger.log("the dexfile data save to =" + filename);
    }

}
