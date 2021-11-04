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
     * 连接失败，如果设置了自动重连，则在自动重连次数用完后，依然没连上时触发
     *
     * @param connectNum 连接次数，连接成功时该次数会重置为0
     */
    default void onConnectFail(int connectNum) {
    }

    /**
     * 异常断开，主动断开时不会触发
     */
    default void onConnectError() {
    }
}
