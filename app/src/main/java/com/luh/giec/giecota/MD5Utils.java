package com.luh.giec.giecota;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Administrator on 2017/5/31.
 */

public class MD5Utils {
    public static String getFileMD5(File file) throws NoSuchAlgorithmException, IOException {
        if (!file.isFile()) {
            System.out.println("it is not file");
            return null;
        }
        MessageDigest digest;
        FileInputStream in;
        byte buffer[] = new byte[1024];
        int len;
        //请求MD5算法
        digest = MessageDigest.getInstance("MD5");
        in = new FileInputStream(file);
        while ((len = in.read(buffer, 0, 1024)) != -1) {
            digest.update(buffer, 0, len);
        }
        in.close();
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16).toUpperCase();
    }

    public static String getAssetsFileMD5(String name, Context context) throws
            NoSuchAlgorithmException, IOException {
        if (name.equals("")) {
            System.out.println("name is null");
            return null;
        }
        MessageDigest digest;
        FileInputStream in;
        byte buffer[] = new byte[1024];
        int len;
        //请求MD5算法
        digest = MessageDigest.getInstance("MD5");

        InputStream inputReader = context.getResources().getAssets().open(name);
        while ((len = inputReader.read(buffer, 0, 1024)) != -1) {
            digest.update(buffer, 0, len);
        }
        inputReader.close();
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16).toUpperCase();
    }
}
