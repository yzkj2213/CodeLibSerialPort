package com.izis.serialport.util;

import java.util.HashMap;
import java.util.Map;

public class ProtocolUtil {
    public static final Map<String,String> responseMap = new HashMap<String, String>(){{
        put("~RGC","ALC");
        put("~STA","SDA");
        put("~TLO","TLS");
        put("~GSV","FJX");
        put("~GVE","VER");
        put("~SHP","HCS");
        put("~CTS","UDS");
        put("~LED","LOS");
        put("~HOT","HSM");
        //----亮多个灯
        put("~SAL","ALS");
        put("~SAM","ALS");
        put("~SAW","ALS");
        put("~SAU","ALS");
        put("~SAN","ALS");
    }};
}
