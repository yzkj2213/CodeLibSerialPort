package com.izis.serialport.connect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import com.example.lxf.laucher2.ISerialService;
import com.example.lxf.laucher2.ISerialServiceListener;
import com.izis.serialport.util.Log;

import java.io.File;

public class SerialConnectService extends SerialConnect {
    public SerialConnectService(Context context) {
        super(context);
    }

    @Override
    void openConnect() {
        Intent intent = new Intent();
        intent.setAction("com.lxf.aidl.SerialServer");
        intent.setPackage("com.example.lxf.laucher2");
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    void disConnect() {
        Log.i("主动断开串口服务<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        try {
            serialService.unRegisterListener(listener);
            context.unbindService(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized boolean writeAndFlush(String commend) {
        try {
            serialService.writeAndFlush(commend);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
        return false;
    }

    //发送指令失败，通知前台, 可能是服务没连接，也可能是服务异常死亡
    private void noticeFront() {
        Log.e("发送指令失败，通知前台，会回调onConnectFail方法, 可能是服务没连接，也可能是服务异常死亡");
        onConnectFailNoReConnect();
    }

    @Override
    public void writeFile(File file) {
        try {
            serialService.writeFile(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public synchronized void addCommend(String commend) {
        try {
            serialService.addCommend(commend);
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public synchronized void clearCommend() {
        try {
            serialService.clearCommend();
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public void setConnectNumMax(int connectNumMax) {
        try {
            serialService.setConnectMaxNum(connectNumMax);
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public void setSendNumMax(int sendNumMax) {
        try {
            serialService.setSendMaxNum(sendNumMax);
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }


    @Override
    public void setMinDelay(int time) {
        try {
            serialService.setMinDelay(time);
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public void setDelayTimes(int times) {
        try {
            serialService.setDelayTimes(times);
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public void addDelayCommend(String commend) {
        try {
            serialService.addDelayCommend(commend);
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public void addResponseCommend(String key, String value) {
        try {
            serialService.addResponseCommend(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            noticeFront();
        }
    }

    @Override
    public boolean writeBytes(byte[] bytes) {
        try {
            return serialService.writeBytes(bytes);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ISerialService serialService;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("串口服务连接成功>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            serialService = ISerialService.Stub.asInterface(service);
            try {
                serialService.registerListener(listener);
                onConnectSuccess("serial_service");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("串口服务连接异常断开<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            try {
                serialService.unRegisterListener(listener);
                onConnectFailNoReConnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private final ISerialServiceListener listener = new ISerialServiceListener.Stub() {
        @Override
        public void onReceiveNormalData(String data) throws RemoteException {
            SerialConnectService.this.onReceiveNormalData(data);
        }

        @Override
        public void onReceiveErrorData(String data) throws RemoteException {
            SerialConnectService.this.onReceiveErrorData(data);
        }

        @Override
        public void onSendData(String data, boolean success) throws RemoteException {
            SerialConnectService.this.onSendData(data, success);
        }

        @Override
        public void onConnectSuccess() throws RemoteException {
            SerialConnectService.this.onConnectSuccess("serial_service");
        }

        @Override
        public void onConnectFail(int connectNum) throws RemoteException {
            SerialConnectService.this.onConnectFail();
        }

        @Override
        public void onConnectError() throws RemoteException {
            SerialConnectService.this.onConnectError("serial_service");
        }
    };
}

