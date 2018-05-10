package com.android.reverse.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Process;

import com.android.reverse.mod.ReverseXposedModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by imlk on 2018/5/5.
 */

public class SoFileLoader {

    private static final String CACHE_NAME = "xm_code_cache";
    private static final String SO_CACHE_SECONDARY_FOLDER_NAME = "xm_secondary-sos";


    public static void loadLibrary(String libName) {


        try {
            ZipFile zipFile = new ZipFile(ReverseXposedModule.MODULE_PATH);


            String soEntryPath;


            if (Build.VERSION.SDK_INT >= 21) {
                soEntryPath = "lib" + "/" + getSuitAbi() + "/lib" + libName + ".so";
            } else {
                soEntryPath = "lib" + "/" + Build.CPU_ABI + "/lib" + libName + ".so";
            }

            ZipEntry soEntry = zipFile.getEntry(soEntryPath);


            if (soEntry.isDirectory()) {
                Logger.log(soEntryPath + " in " + ReverseXposedModule.MODULE_PATH + " is not a file");
                return;
            } else {

                File outFileDir = new File(ReverseXposedModule.APPINFO_DATA_DIR + "/" + CACHE_NAME + "/" + SO_CACHE_SECONDARY_FOLDER_NAME);

                if (!outFileDir.exists()) {
                    outFileDir.mkdirs();
                }

                File outFile = new File(outFileDir, "lib" + libName + ".so");

                FileOutputStream fileOutputStream = new FileOutputStream(outFile);

                InputStream inputStream = zipFile.getInputStream(soEntry);

                byte[] temp = new byte[2048];

                int len;

                while ((len = inputStream.read(temp)) != -1) {
                    fileOutputStream.write(temp, 0, len);
                }

                inputStream.close();
                fileOutputStream.close();

                System.load(outFile.getAbsolutePath());

                outFile.delete();
                outFileDir.delete();
                outFileDir.getParentFile().delete();

            }


        } catch (IOException e) {
            Logger.log("Load So File Error!");
            e.printStackTrace();
        }

    }


    public static String getSuitAbi() {
        if (Process.is64Bit()) {
            return Build.SUPPORTED_64_BIT_ABIS[0];
        } else {
            return Build.SUPPORTED_32_BIT_ABIS[0];
        }
    }

}
