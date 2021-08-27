package com.izis.serialport.connect;

import com.izis.serialport.listener.SerialConnectListener;
import com.izis.serialport.listener.SerialDataListener;
import com.izis.serialport.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 串口连接
 */
public abstract class SerialConnect {
    SerialConnectListener connectListener;
    SerialDataListener dataListener;
    int connectNum = 0;//连接次数

    public void setConnectListener(SerialConnectListener connectListener) {
        this.connectListener = connectListener;
    }

    public void setDataListener(SerialDataListener dataListener) {
        this.dataListener = dataListener;
    }

    /**
     * 打开连接
     */
    public abstract void open();

    /**
     * 关闭连接
     */
    public abstract void close();

    /**
     * 写入数据
     *
     * @param data 数据
     */
    public abstract void writeAndFlush(String data);

    private String readTemp = ""; // 存储接受到的非完整数据

    public void checkData(String curReadData) {
        Log.d("已缓存数据:" + readTemp);
        Log.d("本次读取数据:" + curReadData);

        readTemp = String.format("%s%s", readTemp, curReadData); // 得到最完整的读取池数据

        int firstStartCharIndex = readTemp.indexOf("~");

        if (firstStartCharIndex > 0) {// 总池子不是以“~”开头，有故障。将开头数据摒弃。（-1，0，>0）
            readTemp = readTemp.substring(firstStartCharIndex);
        }

        Log.d("初步处理数据:" + readTemp);

        String totalCommands = "";
        //开头必定为~号
        //缓存中完整数据后的半截数据
        String tempRest = "";
        if (readTemp.lastIndexOf("#") > 0) {//包含了#号（不包含则说明不包含一个完整的指令）
            if (readTemp.lastIndexOf("#") < readTemp.length() - 1) {//包含了部分下一条指令
                tempRest = readTemp.substring(readTemp.lastIndexOf("#") + 1); // 得到最后的#号之后的半截数据
            } else {
                tempRest = "";
            }

            totalCommands = readTemp.substring(0, readTemp.lastIndexOf("#") + 1); // 完整的指令集合
        } else { // 不包含结束符号，说明指令尚不完整，继续等待下一帧。
            return; // 继续下一次循环
        }

        Log.d("所有正常指令，可能包含多个:" + totalCommands);

        // ==================包含#号
        // 解析数据，分解成一条条指令后交给前台调用者处理===================

        String p = "~?[^~#]*#";
        Pattern r = Pattern.compile(p);
        Matcher m = r.matcher(totalCommands);
        while (m.find()) {
            String group = m.group();
            Log.i("得到一条完整的指令:" + group);
            if (dataListener != null)
                dataListener.onReadData(group);// 刚好一条完整指令，则直接通知前台去处理即可
        }

        readTemp = tempRest;
    }
}
