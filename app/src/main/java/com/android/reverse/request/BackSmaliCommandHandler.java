package com.android.reverse.request;

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
        // TODO Auto-generated method stub
        String filename = ModuleContext.getInstance().getAppContext().getFilesDir() + "/dexfile" + mCookie + ".dex";
        DexFileInfoCollecter.getInstance().backsmaliDexFile(filename, mCookie);
        Logger.log("the dexfile data save to =" + filename);
    }

}
