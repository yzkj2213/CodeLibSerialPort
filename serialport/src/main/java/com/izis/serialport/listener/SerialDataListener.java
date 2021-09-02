package com.izis.serialport.listener;

/**
 * 棋盘数据监听
 */
public interface SerialDataListener {
    /**
     * 接收到串口传输的数据
     * @param data 数据，已做处理，符合 ~?[A-Z]{3}[^~#]*# 格式
     */
    void onNormalData(String data);

    /**
     * 接收到串口传输的异常数据
     * @param data 数据，不符合 ~?[A-Z]{3}[^~#]*# 格式，或者有非数字字母的字符
     */
    void onErrorData(String data);
}
