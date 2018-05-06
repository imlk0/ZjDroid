package com.android.reverse.request;


import com.android.reverse.collecter.MemDump;
import com.android.reverse.collecter.ModuleContext;
import com.android.reverse.util.Logger;

public class DumpMemCommandHandler implements CommandHandler {

	private String dumpFileName;
	private long start;
	private int length;
	
	public DumpMemCommandHandler(long start, int length){
		this.start = start;
		this.length = length;
		this.dumpFileName = String.valueOf(start);
	}

	@Override
	public void doAction() {
		String memfilePath = ModuleContext.getInstance().getAppContext().getFilesDir()+"/"+dumpFileName;
        MemDump.dumpMem(memfilePath, start, length);
        Logger.log("the mem data save to ="+ memfilePath);
	}

}
