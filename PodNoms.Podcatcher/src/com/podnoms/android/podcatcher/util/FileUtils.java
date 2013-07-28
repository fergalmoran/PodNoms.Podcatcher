package com.podnoms.android.podcatcher.util;

import java.io.*;
import java.util.ArrayList;

public class FileUtils {
    public static String readFile(InputStream is) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            is.read(buffer);
            bo.write(buffer);
            bo.close();
            is.close();
        } catch (IOException e) {
            LogHandler.reportError("FileUtils: readFile", e);
        }

        return bo.toString();
    }

    public static ArrayList<String> parseSqlFile(BufferedReader br) {
        String strLine;
        StringBuilder buf = new StringBuilder();
        ArrayList<String> ret = new ArrayList<String>();
        try {
            while ((strLine = br.readLine()) != null) {
                if (!strLine.startsWith("--"))
                    buf.append(strLine).append(" ");

                if (strLine.endsWith(";")) {
                    ret.add(buf.toString().replace(
                            "--", ""));
                    buf.setLength(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void copyFile(File src, File dst) throws IOException {
        copyFile(src, dst, false);
    }
    public static void copyFile(File src, File dst, Boolean deleteDest) throws IOException {
        if (dst.exists() && deleteDest)
            dst.delete();

        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    public static String getFileContent(String filePath) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(filePath));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void writeFile(String fileName, byte[] baf) {
        try {
            File file = new File(fileName);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(baf);
            out.flush();
            out.close();
        } catch (IOException e) {
            LogHandler.reportError("Error writing byte array to file: " + fileName, e);
        }
    }

    public static void clearDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            System.out.println(directory + " does not exist");
            return;
        }

        String[] info = dir.list();
        for (int i = 0; i < info.length; i++) {
            File n = new File(directory + dir.separator + info[i]);
            if (!n.isFile())
                continue;
            System.out.println("removing " + n.getPath());
            if (!n.delete())
                System.err.println("Couldn't remove " + n.getPath());
        }
    }
}

