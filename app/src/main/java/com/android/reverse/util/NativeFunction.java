package com.android.reverse.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import org.jf.dexlib2.dexbacked.MemoryDexFileItemPointer;
import org.jf.dexlib2.dexbacked.MemoryReader;

import com.android.reverse.collecter.ModuleContext;
import com.android.reverse.smali.DexFileHeadersPointer;


public class NativeFunction implements MemoryReader {
	
	private final static String DVMNATIVE_LIB = "dvmnative";

	static{

		SoFileLoader.loadLibrary(DVMNATIVE_LIB);
	}
	
	public static native ByteBuffer dumpDexFileByClass(Class classInDex,int version);
	public static native ByteBuffer dumpDexFileByCookie(long cookie,int version);
	public static native ByteBuffer dumpMemory(long start,int length);
	private static native DexFileHeadersPointer getHeaderItemPtr(long cookie,int version);
    public static native String getInlineOperation();
    public static native HashMap getSyslinkSnapshot();
	
	public byte[] readBytes(int arg0, int arg1) {
		ByteBuffer data = dumpMemory(arg0, arg1);
		data.order(ByteOrder.LITTLE_ENDIAN);
		byte[] buffer = new byte[data.capacity()];
		data.get(buffer, 0, data.capacity());
		return buffer;
	}
	
	public static MemoryDexFileItemPointer queryDexFileItemPointer(long cookie){
		int version = ModuleContext.getInstance().getApiLevel();
		//TODO change int to long
		DexFileHeadersPointer iteminfo = getHeaderItemPtr(cookie,version);
		MemoryDexFileItemPointer pointer = new MemoryDexFileItemPointer();
		pointer.setBaseAddr((int) iteminfo.getBaseAddr());
		pointer.setpClassDefs((int) iteminfo.getpClassDefs());
		pointer.setpFieldIds((int) iteminfo.getpFieldIds());
		pointer.setpMethodIds((int) iteminfo.getpMethodIds());
		pointer.setpProtoIds((int) iteminfo.getpProtoIds());
		pointer.setpStringIds((int) iteminfo.getpStringIds());
		pointer.setpTypeIds((int) iteminfo.getpTypeIds());
        pointer.setClassCount((int) iteminfo.getClassCount());
		return pointer;

	}

}
