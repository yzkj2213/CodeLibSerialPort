// ISerialServiceListener.aidl
package com.example.lxf.laucher2;

// Declare any non-default types here with import statements

interface ISerialServiceListener {
    void onReceiveNormalData(String data);
    void onReceiveErrorData(String data);
    void onSendData(String data, boolean success);
    void onConnectSuccess();
    void onConnectFail(int connectNum);
    void onConnectError();

    //Service连接专属事件，连接关闭后解绑服务
    void onClose();
}