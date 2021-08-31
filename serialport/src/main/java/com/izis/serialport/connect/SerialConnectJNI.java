package com.izis.serialport.connect;

import android.text.TextUtils;

import com.izis.serialport.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class SerialConnectJNI extends SerialConnect {
    private boolean isOpen = false;
    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;

    @Override
    public void open() {
        if (isOpen) return;
        connectNum++;
        SerialPortFinder mSerialPortFinder = new SerialPortFinder();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        if (entryValues == null) {
            Log.w("没有找到相关设备");
            if (connectListener != null)
                connectListener.onConnectFail(connectNum);
            return;
        }
        Log.i("查找到设备：" + Arrays.toString(entryValues));

        String device = "";
        for (String item : entryValues) {
            if (item.contains("/dev/ttyUSB0")) {
                device = item;
                break;
            }
        }
        if (device.isEmpty()) {
            Log.w("没有找到相关设备");
            if (connectListener != null)
                connectListener.onConnectFail(connectNum);
            return;
        }

        try {
            // 打开/dev/ttyUSB0路径设备的串口  ttyUSB0
            mSerialPort = new SerialPort(new File(device), 115200, 0);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
            if (connectListener != null)
                connectListener.onConnectSuccess();
            Log.i("连接设备成功");

            connectNum = 0;
            isOpen = true;
            new Thread() {
                @Override
                public void run() {
                    requestData();
                }
            }.start();
        } catch (Exception e) {
            Log.e("打开串口失败");
            if (connectListener != null)
                connectListener.onConnectFail(connectNum);
        }
    }

    @Override
    public void close() {
        isOpen = false;
        if (mSerialPort != null)
            mSerialPort.close();
    }

    @Override
    public boolean writeAndFlushNoDelay(String data) {
        if (TextUtils.isEmpty(data)) {
            Log.w("写入指令失败：" + data);
            return false;
        }
        try {
            mOutputStream.write(data.getBytes());
            mOutputStream.flush();
            Log.i("写入指令：" + data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("写入指令失败：" + data);
            return false;
        }
    }

    private void requestData() {
        while (isOpen) {
            try {
                sleep();

                byte[] buffer = new byte[1024];
                if (mInputStream == null) return;
                int size = mInputStream.read(buffer);
                if (size > 0) {
                    byte[] readBytes = new byte[size];
                    System.arraycopy(buffer, 0, readBytes, 0, size);
                    String curReadData = new String(readBytes);

                    checkData(curReadData);
                }
            } catch (IOException e) {
                e.printStackTrace();
                close();
                if (connectListener != null)
                    connectListener.onErrorConnect(connectNum);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
