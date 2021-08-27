package com.izis.serialport.listener;

/**
 * 棋盘数据监听
 */
public interface SerialDataListener {
    /**
     * 接收到串口传输的数据
     * @param data 数据，已做处理，符合~XXX#格式
     */
    void onReadData(String data);
}
