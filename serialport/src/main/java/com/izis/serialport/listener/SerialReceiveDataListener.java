package com.izis.serialport.listener;

import androidx.annotation.NonNull;

/**
 * 棋盘数据监听
 */
public interface SerialReceiveDataListener {
    /**
     * 接收到串口传输的正常数据
     * @param data 数据，已做处理，符合 ~?[A-Z]{3}[^~#]*# 格式
     */
    void onReceiveNormalData(@NonNull String data);

    /**
     * 接收到串口传输的异常数据
     * @param data 数据，不符合 ~?[A-Z]{3}[^~#]*# 格式，或者有非数字、字母、小数点的字符
     */
    void onReceiveErrorData(@NonNull String data);
}
