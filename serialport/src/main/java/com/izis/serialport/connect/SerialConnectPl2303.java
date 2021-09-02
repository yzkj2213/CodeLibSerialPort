package com.izis.serialport.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.izis.serialport.listener.SerialConnectListener;
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
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Context mContext;
    private PLMultiLibReceiver mPlMultiLibReceiver;
    private boolean isOpen = false;


    static class PLMultiLibReceiver extends BroadcastReceiver {
        private final PL2303MultiLib mSerialMulti;
        private final SerialConnectListener mConnectListener;
        private final int deviceIndex;
        private final SerialConnect serialConnect;

        public PLMultiLibReceiver(PL2303MultiLib mSerialMulti, SerialConnectListener mConnectListener, int deviceIndex, SerialConnect serialConnect) {
            this.mSerialMulti = mSerialMulti;
            this.mConnectListener = mConnectListener;
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
                            if (mConnectListener != null)
                                mConnectListener.onConnectError(serialConnect.connectNum);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public SerialConnectPl2303(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void open() {
        if (isOpen) return;
        connectNum++;
        if (mSerialMulti == null) {
            mSerialMulti = new PL2303MultiLib((UsbManager) mContext.getSystemService(Context.USB_SERVICE), mContext, ACTION_USB_PERMISSION);
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (pl2303LinkExist()) return;

                mSerialMulti.PL2303Enumerate();

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        boolean res = mSerialMulti.PL2303OpenDevByUARTSetting(deviceIndex, mBaudRate, mDataBits, mStopBits, mParity, mFlowControl);
                        if (!res) {
                            Log.e("打开连接失败");
                            onConnectFail();
                        } else {
                            Log.i("打开" + mSerialMulti.PL2303getDevicePathByIndex(deviceIndex) + "成功!");

                            IntentFilter filter = new IntentFilter();
                            filter.addAction(mSerialMulti.PLUART_MESSAGE);
                            mPlMultiLibReceiver = new PLMultiLibReceiver(mSerialMulti, connectListener, deviceIndex, SerialConnectPl2303.this);
                            mContext.registerReceiver(mPlMultiLibReceiver, filter);

                            onConnectSuccess();

                            isOpen = true;
                            connectNum = 0;
                            requestData();
                        }
                    }
                }, 300);
            }
        }, 300);
    }

    @Override
    public void close() {
        isOpen = false;
        if (mSerialMulti != null) {
            try {
                if (mPlMultiLibReceiver != null)
                    mContext.unregisterReceiver(mPlMultiLibReceiver);
            } catch (Exception e) {
                //防止多次调用close报  Receiver not registered 异常
            }

            mSerialMulti.PL2303Release();
            mSerialMulti = null;
        }
        executorService.shutdown();
    }

    @Override
    boolean writeAndFlushNoDelay(String commend) {
        if (!pl2303LinkExist() || TextUtils.isEmpty(commend)) {
            Log.w("写入指令失败：" + commend);
            onSendData(commend, false);
            return false;
        }

        if (mSerialMulti != null) {
            int i = mSerialMulti.PL2303Write(deviceIndex, commend.getBytes());
            if (i < 0) {
                Log.w("写入指令失败：" + commend);
                onSendData(commend, false);
            }else{
                Log.i("写入指令：" + commend);
                onSendData(commend, true);
            }
            return i > 0;
        }
        Log.w("写入指令失败：" + commend);
        onSendData(commend, false);
        return false;
    }

    private void requestData() {
        executorService.submit(new Runnable() {
            private final byte[] readByte = new byte[512]; // 4096

            @Override
            public void run() {
                while (isOpen) {
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
