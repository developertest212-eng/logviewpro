package com.logviewpro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EncodingDetector {

    public static Charset detectEncoding(File file) {
        if (file == null || !file.exists() || file.length() == 0) {
            return StandardCharsets.UTF_8;
        }

        byte[] buffer = new byte[Math.min(8192, (int) file.length())];
        int bytesRead;
        try (InputStream is = new FileInputStream(file)) {
            bytesRead = is.read(buffer);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }

        if (bytesRead < 2) {
            return StandardCharsets.UTF_8;
        }

        // 1. Check BOM
        if (bytesRead >= 3 && buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB && buffer[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (buffer[0] == (byte) 0xFE && buffer[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE) {
            return StandardCharsets.UTF_16LE;
        }

        // 2. Validate UTF-8 byte sequences
        if (isValidUtf8(buffer, bytesRead)) {
            return StandardCharsets.UTF_8;
        }

        // 3. Fallback to Windows-1252 / ISO-8859-1
        try {
            return Charset.forName("windows-1252");
        } catch (Exception e) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    private static boolean isValidUtf8(byte[] bytes, int length) {
        int i = 0;
        while (i < length) {
            int b = bytes[i] & 0xFF;
            if (b <= 0x7F) {
                i++;
            } else if ((b >= 0xC2) && (b <= 0xDF)) {
                if (i + 1 >= length) break;
                if ((bytes[i + 1] & 0xC0) != 0x80) return false;
                i += 2;
            } else if ((b >= 0xE0) && (b <= 0xEF)) {
                if (i + 2 >= length) break;
                if ((bytes[i + 1] & 0xC0) != 0x80 || (bytes[i + 2] & 0xC0) != 0x80) return false;
                i += 3;
            } else if ((b >= 0xF0) && (b <= 0xF4)) {
                if (i + 3 >= length) break;
                if ((bytes[i + 1] & 0xC0) != 0x80 || (bytes[i + 2] & 0xC0) != 0x80 || (bytes[i + 3] & 0xC0) != 0x80) return false;
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }
}
