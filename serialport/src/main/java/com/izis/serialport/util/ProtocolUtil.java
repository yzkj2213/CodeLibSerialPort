package com.izis.serialport.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtocolUtil {
    /**
     * 有响应的指令集合
     */
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

    /**
     * 发完需要延迟再发下一条的指令
     */
    public static final List<String> delayList = new ArrayList<String>(){{
        add("~RGC");
        add("~FLL");
    }};

    /**
     * 指令延迟的倍率
     */
    public static int delayTimes = 3;

    /**
     * 每条指令的最小基础间隔
     */
    public static int minDelay = 80;
}
