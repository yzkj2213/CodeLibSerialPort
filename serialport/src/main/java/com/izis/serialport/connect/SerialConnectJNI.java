package com.izis.serialport.connect;

import android.text.TextUtils;
import com.izis.serialport.util.Log;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class SerialConnectJNI extends SerialConnect {
    private boolean isOpen = false;
    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;

    @Override
    public void open() {
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

        List<String> devices = new ArrayList<>(Arrays.asList(entryValues));
        if (!devices.contains("/dev/ttyUSB0")) {
            Log.w("没有找到相关设备");
            if (connectListener != null)
                connectListener.onConnectFail(connectNum);
            return;
        }

        try {
            // 打开/dev/ttyUSB0路径设备的串口  ttyUSB0
            mSerialPort = new SerialPort(new File("/dev/ttyUSB0"), 115200, 0);
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
    public void writeAndFlush(String data) {
        if (TextUtils.isEmpty(data)) return;
        try {
            sleep();

            mOutputStream.write(data.getBytes());
            mOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
