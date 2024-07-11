package com.izis.serialport.device_id;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.android.uniwin.UniwinAPI;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class OSInfo {

    /**
     * 是否是电子棋盘
     *
     * @deprecated 使用 {@link #isBoard(Context context)} 代替。
     * 标记过时原因：
     * 1. 某些极端的情况下会存在误判，比如用户也使用了同样一款定制的开发板。
     * 2. 当有新型号的棋盘设备时，需要更改api才能判断
     *
     */
    @Deprecated
    public static Boolean isBoard() {
        return isSZ() || isSZ_A133() || isSZ_M527() || isDW() || isYS();
    }

    /**
     * 是否是电子棋盘，通过检测当前设备是否有安装 系统桌面 app判断
     */
    public static Boolean isBoard(Context context) {
        boolean hasLauncher = false;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        for (int i = 0; i < resolveInfos.size(); i++) {
            ResolveInfo item = resolveInfos.get(i);
            if (item.activityInfo.packageName.equals("com.example.lxf.laucher2")) {
                hasLauncher = true;
                break;
            }
        }
        return hasLauncher;
    }


    /**
     * 获取电子棋盘的设备ID
     */
    public static String getDeviceId() {
        if (isSZ_M527()) {
            return getCpuSerialM527();
        } else if (isSZ_A133()) {
            return getCpuSerialM133();
        } else {
            return getMacAddress();
        }
    }

    private static String getCpuSerialM133() {
        return UniwinAPI.getProperty("ro.serialno");
    }

    private static String getCpuSerialM527() {
        return android.uniwin.UniwinAPI.getProperty("ro.serialno");
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

    private static Boolean isSZ_M527() {
        return Build.VERSION.SDK_INT == 33 && Build.MODEL.equals("UW-M527");
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
