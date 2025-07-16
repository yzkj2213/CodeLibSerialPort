package com.izis.serialport.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.izis.serialport.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tw.com.prolific.pl2303multilib.PL2303MultiLib;

public class SerialConnectPl2303 extends SerialConnect {
    private static final String ACTION_USB_PERMISSION = "com.izis.serialport.connect.SerialConnectPl2303.USB_PERMISSION";
    private static final int deviceIndex = 0;
    private final PL2303MultiLib.BaudRate mBaudRate = PL2303MultiLib.BaudRate.B115200;
    private final PL2303MultiLib.DataBits mDataBits = PL2303MultiLib.DataBits.D8;
    private final PL2303MultiLib.Parity mParity = PL2303MultiLib.Parity.NONE;
    private final PL2303MultiLib.StopBits mStopBits = PL2303MultiLib.StopBits.S1;
    private final PL2303MultiLib.FlowControl mFlowControl = PL2303MultiLib.FlowControl.XONXOFF;  // 是否有效？
    private PL2303MultiLib mSerialMulti;
    private ExecutorService executorService;
    private PLMultiLibReceiver mPlMultiLibReceiver;

    static class PLMultiLibReceiver extends BroadcastReceiver {
        private final PL2303MultiLib mSerialMulti;
        private final int deviceIndex;
        private final SerialConnect serialConnect;

        public PLMultiLibReceiver(PL2303MultiLib mSerialMulti, int deviceIndex, SerialConnect serialConnect) {
            this.mSerialMulti = mSerialMulti;
            this.deviceIndex = deviceIndex;
            this.serialConnect = serialConnect;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(mSerialMulti.PLUART_MESSAGE)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String str = (String) extras.get(mSerialMulti.PLUART_DETACHED);
                    try {
                        int index = Integer.parseInt(str);
                        if (deviceIndex == index) {
                            serialConnect.close();
                            serialConnect.onConnectError(mSerialMulti.PL2303getDevicePathByIndex(deviceIndex));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public SerialConnectPl2303(Context context) {
        super(context);
    }

    @Override
    void openConnect() {
        if (mSerialMulti == null) {
            mSerialMulti = new PL2303MultiLib((UsbManager) context.getSystemService(Context.USB_SERVICE), context, ACTION_USB_PERMISSION);
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (pl2303LinkExist()) {
                    Log.e("打开连接失败");
                    onConnectFail();
                    return;
                }

                if (mSerialMulti == null) return;
                mSerialMulti.PL2303Enumerate();

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (mSerialMulti == null) return;
                        boolean res = mSerialMulti.PL2303OpenDevByUARTSetting(deviceIndex, mBaudRate, mDataBits, mStopBits, mParity, mFlowControl);
                        if (!res) {
                            Log.e("打开连接失败");
                            onConnectFail();
                        } else {
                            IntentFilter filter = new IntentFilter();
                            filter.addAction(mSerialMulti.PLUART_MESSAGE);
                            mPlMultiLibReceiver = new PLMultiLibReceiver(mSerialMulti, deviceIndex, SerialConnectPl2303.this);
                            context.registerReceiver(mPlMultiLibReceiver, filter);

                            onConnectSuccess(mSerialMulti.PL2303getDevicePathByIndex(deviceIndex));

                            requestData();
                        }
                    }
                }, 300);
            }
        }, 300);
    }

    @Override
    void disConnect() {
        if (mSerialMulti != null) {
            try {
                if (mPlMultiLibReceiver != null)
                    context.unregisterReceiver(mPlMultiLibReceiver);
            } catch (Exception e) {
                //防止多次调用close报  Receiver not registered 异常
            }

            mSerialMulti.PL2303Release();
            mSerialMulti = null;
        }
        if (executorService != null)
            executorService.shutdown();
    }

    @Override
    public boolean writeBytes(byte[] bytes) {
        boolean result;
        if (!pl2303LinkExist()) {
            result = false;
        } else {
            if (mSerialMulti != null) {
                int i = mSerialMulti.PL2303Write(deviceIndex, bytes);
                result = i > 0;
            } else {
                result = false;
            }
        }

        if (!result) {
            disConnect();
            onConnectError("PL2303");
        }

        return result;
    }

    private void requestData() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            private final byte[] readByte = new byte[512]; // 4096

            @Override
            public void run() {
                while (isConnected()) {
                    sleep();

                    if (mSerialMulti != null) {
                        int readLen = 0;//一次读取到的数据长度
                        try {
                            readLen = mSerialMulti.PL2303Read(deviceIndex, readByte); // 从底层读取数据
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (readLen > 0) {
                            StringBuilder readData = new StringBuilder();
                            for (int j = 0; j < readLen; j++) {
                                readData.append((char) (readByte[j] & 0x000000FF));
                            }

                            String curReadData = readData.toString(); // 将获取的棋盘数据转换成字符串

                            checkData(curReadData);
                        }
                    }
                }
            }
        });
    }

    private boolean pl2303LinkExist() {
        return mSerialMulti != null && mSerialMulti.PL2303IsDeviceConnectedByIndex(deviceIndex);
    }
}
