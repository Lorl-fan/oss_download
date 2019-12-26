package com.xian.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 文件读写工具类
 */
public class FileUtil {

    // 写文件
    public static void writeFile(String fileName, byte[] content){
        File file = new File(fileName);
        File fileparent = file.getParentFile();
        if (!fileparent.exists()) {
            fileparent.mkdirs();
        }
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fileName);
            os.write(content);
            os.flush();

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (null != os)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        os = null;
    }

    // 读文件
    public static byte[] readFile(String fileName) {
        FileInputStream fis = null;
        byte[] buffer = null;
        try {
            fis = new FileInputStream(fileName);
            buffer = new byte[fis.available()];
            fis.read(buffer);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fis)
                    fis.close();
            } catch (IOException e) {
            }
        }
        fis = null;
        return buffer;
    }

}
