// ISerialService.aidl
package com.example.lxf.laucher2;

import com.example.lxf.laucher2.ISerialServiceListener;
// Declare any non-default types here with import statements

interface ISerialService {
    void registerListener(ISerialServiceListener listener);
    void unRegisterListener(ISerialServiceListener listener);
    void writeAndFlush(String commend);
    void addCommend(String commend);
    void writeFile(String path);
    void clearCommend();

    void setConnectMaxNum(int num);
    void setSendMaxNum(int num);
    void setMinDelay(int time);
    void setDelayTimes(int times);
    void addResponseCommend(String key, String value);
    void addDelayCommend(String value);
}