package com.izis.serialport.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;

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
    private final Context context;
    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.e(device.getDeviceName() + "断开");
                    close();
                    onConnectError();
                }
            }
        }
    };
    public SerialConnectDirect(Context context) {
        this.context = context;
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

            IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
            if (context != null) context.registerReceiver(usbReceiver, filter);

            onConnectSuccess();
            Log.i("连接设备成功");

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
        try {
            if (context != null) {
                context.unregisterReceiver(usbReceiver);
            }
        } catch (Exception e) {
            //防止多次调用close报  Receiver not registered 异常
        }

        if (mSerialPort != null)
            executorService.execute(() -> mSerialPort.close());
    }

    @Override
    boolean writeAndFlushNoDelay(String commend) {
        if (TextUtils.isEmpty(commend)) {
            Log.w("写入指令失败：" + commend);
            onSendData(commend, false);
            return false;
        }
        try {
            mOutputStream.write(commend.getBytes());
            mOutputStream.flush();
            Log.i("写入指令：" + commend);
            onSendData(commend, true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("写入指令失败：" + commend);
            onSendData(commend, false);
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
