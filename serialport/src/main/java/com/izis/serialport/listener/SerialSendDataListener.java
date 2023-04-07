package com.izis.serialport.listener;


public interface SerialSendDataListener {
    /**
     * 发送数据的监听
     * @param data 发送的数据
     * @param result 发送是否成功
     */
    void onSendData(String data, boolean result);
}
