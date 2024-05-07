package com.izis.serialport.device_id;

import android.os.Build;

import com.android.uniwin.UniwinAPI;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class OSInfo {

    /**
     * 是否是电子棋盘
     */
    public static Boolean isBoard() {
        return isSZ() || isSZ_A133() || isDW() || isYS();
    }

    /**
     * 获取电子棋盘的设备ID
     */
    public static String getDeviceId() {
        if (isSZ_A133()) {
            return getCpuSerial();
        } else {
            return getMacAddress();
        }
    }

    private static String getCpuSerial() {
        return UniwinAPI.getProperty("ro.serialno");
    }

    private static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (int i = 0; i < all.size(); i++) {
                NetworkInterface nif = all.get(i);
                if (!nif.getName().equals("wlan0")) continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString().replace(":", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00".replace(":", "");
    }

    private static Boolean isSZ_A133() {
        return Build.VERSION.SDK_INT == 29 && Build.MODEL.equals("UW-M133");
    }

    private static Boolean isSZ() {
        return Build.VERSION.RELEASE.equals("7.5.0") || Build.VERSION.RELEASE.equals("7.6.0") || isSZ_YZ();
    }

    private static Boolean isSZ_YZ() {
        return Build.VERSION.RELEASE.equals("7.5.0_1");
    }

    private static Boolean isDW() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 && Build.BRAND.contains("rockchip");
    }

    private static Boolean isYS() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.O && Build.BRAND.contains("Android");
    }
}
