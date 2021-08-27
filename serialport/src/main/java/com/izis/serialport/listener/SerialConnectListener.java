package com.izis.serialport.listener;

/**
 * 串口连接监听
 */
public interface SerialConnectListener {
    /**
     * 连接成功
     */
    void onConnectSuccess();

    /**
     * 连接失败
     *
     */
    void onConnectFail(int connectNum);

    /**
     * 异常断开
     *
     * 有可能会触发多次
     */
    void onErrorConnect(int connectNum);
}
