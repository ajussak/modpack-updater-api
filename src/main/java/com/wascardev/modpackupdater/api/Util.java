package com.wascardev.modpackupdater.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    static OS getPlatform() {
        String str = System.getProperty("os.name").toLowerCase();
        if (str.contains("win")) {
            return OS.WINDOWS;
        }
        if (str.contains("mac")) {
            return OS.MACOS;
        }
        if (str.contains("solaris")) {
            return OS.SOLARIS;
        }
        if (str.contains("sunos")) {
            return OS.SOLARIS;
        }
        if (str.contains("linux")) {
            return OS.LINUX;
        }
        if (str.contains("unix")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }

    static File getClientDirectory(String paramString) {
        String str1 = System.getProperty("user.home", ".");
        File localFile;
        switch (getPlatform()) {
            case LINUX:
            case SOLARIS:
                localFile = new File(str1, '.' + paramString + '/');
                break;
            case WINDOWS:
                String str2 = System.getenv("APPDATA");
                String str3 = str2 != null ? str2 : str1;

                localFile = new File(str3, '.' + paramString + '/');
                break;
            case MACOS:
                localFile = new File(str1, "Library/Application Support/" + paramString);
                break;
            default:
                localFile = new File(str1, paramString + '/');
        }
        if ((!localFile.exists()) && (!localFile.mkdirs())) {
            return null;
        }
        return localFile;
    }

    enum OS {
        LINUX, SOLARIS, WINDOWS, MACOS, UNKNOWN
    }

    public static boolean verifyChecksum(File file, String testChecksum) throws NoSuchAlgorithmException, IOException {
        if(testChecksum.equals("none"))
            return true;

        String fileHash = sha1(file);

        return fileHash.equals(testChecksum);
    }

    public static String sha1(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        FileInputStream fis = new FileInputStream(file);

        byte[] data = new byte[1024];
        int read = 0;
        while ((read = fis.read(data)) != -1) {
            sha1.update(data, 0, read);
        }
        byte[] hashBytes = sha1.digest();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

}
