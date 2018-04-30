package com.android.reverse.request;


import com.android.reverse.collecter.DexFileInfoCollecter;
import com.android.reverse.collecter.ModuleContext;
import com.android.reverse.util.Logger;

public class DumpDexFileCommandHandler implements CommandHandler {

    private String mCookie;

    public DumpDexFileCommandHandler(String mCookie) {
        this.mCookie = mCookie;
    }

    @Override
    public void doAction() {
        // TODO Auto-generated method stub
        String filename = ModuleContext.getInstance().getAppContext().getFilesDir() + "/dexdump" + mCookie + ".odex";
        DexFileInfoCollecter.getInstance().dumpDexFile(filename, mCookie);
        Logger.log("the dexfile data save to =" + filename);
    }


}
