package com.izis.serialport.connect;

import android.content.Context;
import com.izis.serialport.util.Log;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class SerialConnectDirect extends SerialConnect {
    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public SerialConnectDirect(Context context) {
        super(context);
    }

    @Override
    void openConnect() {
        SerialPortFinder mSerialPortFinder = new SerialPortFinder();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        if (entryValues == null) {
            Log.w("没有找到相关设备");
            onConnectFail();
            return;
        }
        Log.i("查找到设备：" + Arrays.toString(entryValues));

        String device = "";
        for (String item : entryValues) {
            if (item.contains("/dev/ttyS1")) {
                device = item;
                break;
            }
        }
        if (device.isEmpty()) {
            Log.w("没有找到相关设备");
            onConnectFail();
            return;
        }

        try {
            // 打开串口
            mSerialPort = new SerialPort(new File(device), 115200, 0);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();

            onConnectSuccess(device);

            new Thread() {
                @Override
                public void run() {
                    requestData();
                }
            }.start();
        } catch (Exception e) {
            Log.e("打开串口失败");
            onConnectFail();
        }
    }

    @Override
    void disConnect() {
        if (mSerialPort != null)
            executorService.execute(() -> mSerialPort.close());
    }

    @Override
    boolean writeAndFlushNoDelay(byte[] bytes) {
        try {
            mOutputStream.write(bytes);
            mOutputStream.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void requestData() {
        while (isConnected()) {
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
}
