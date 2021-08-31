package com.izis.serialport.connect;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.izis.serialport.util.Log;

import java.io.IOException;
import java.util.HashMap;

public class SerialConnectAPI extends SerialConnect {
    private final Context context;
    private UsbManager manager;
    private UsbDevice device;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpointIn;
    private UsbEndpoint usbEndpointOut;
    private UsbDeviceConnection usbDeviceConnection;
    private boolean isOpen = false;
    private static final String ACTION_DEVICE_PERMISSION = "ACTION_DEVICE_PERMISSION";
    private UsbPermissionReceiver usbPermissionReceiver;
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.e(device.getDeviceName() + "断开");
                    close();
                    if (connectListener != null)
                        connectListener.onErrorConnect(connectNum);
                }
            }
        }
    };

    private static class UsbPermissionReceiver extends BroadcastReceiver {
        private final SerialConnectAPI connect;

        public UsbPermissionReceiver(SerialConnectAPI connect) {
            this.connect = connect;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ACTION_DEVICE_PERMISSION)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device == null || connect.device == null) return;
                    if (connect.device.getDeviceName().equals(device.getDeviceName())) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            Log.i("授权成功");
                            connect.connect();
                        } else {
                            Log.w("请授权");
                        }
                    }
                }
            }
        }
    }

    public SerialConnectAPI(Context context) {
        this.context = context;
    }

    @Override
    public void open() {
        if (isOpen) return;
        connectNum++;
        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Log.d("deviceList：" + deviceList.size());
        for (UsbDevice next : deviceList.values()) {
            if (next != null) {
                Log.d(next.toString());
                Log.d("---------------------");
                if (next.getVendorId() == 1659 && next.getProductId() == 8963) {
                    device = next;
                    break;
                }
            }
        }

        if (device != null) {
            int interfaceCount = device.getInterfaceCount();
            Log.d("UsbInterface数目：" + interfaceCount);
            if (interfaceCount > 0) {
                usbInterface = device.getInterface(0);
                if (usbInterface != null) {
                    for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                        UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
                        if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                usbEndpointIn = usbEndpoint;
                            }
                            if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                usbEndpointOut = usbEndpoint;
                            }
                        }
                    }
                }
            }
        }

        if (usbEndpointIn != null && usbEndpointOut != null) {
            Intent intent = new Intent(ACTION_DEVICE_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            IntentFilter intentFilter = new IntentFilter(ACTION_DEVICE_PERMISSION);
            usbPermissionReceiver = new UsbPermissionReceiver(this);
            context.registerReceiver(usbPermissionReceiver, intentFilter);
            manager.requestPermission(device, pendingIntent);
        } else {
            if (connectListener != null)
                connectListener.onConnectFail(connectNum);
        }
    }

    private void connect() {
        if (manager == null) return;
        usbDeviceConnection = manager.openDevice(device);
        if (usbDeviceConnection != null) {
            boolean claimInterface = usbDeviceConnection.claimInterface(usbInterface, true);
            if (!claimInterface) {
                if (connectListener != null)
                    connectListener.onConnectFail(connectNum);
                return;
            }
            try {
                config();
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        requestData();
                    }
                }.start();
                isOpen = true;
                connectNum = 0;
                Log.i("打开" + device.getDeviceName() + "成功");
                IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
                context.registerReceiver(usbReceiver, filter);
                if (connectListener != null)
                    connectListener.onConnectSuccess();
            } catch (Exception e) {
                e.printStackTrace();
                if (connectListener != null)
                    connectListener.onConnectFail(connectNum);
            }
        } else {
            if (connectListener != null)
                connectListener.onConnectFail(connectNum);
        }
    }

    @Override
    public void close() {
        isOpen = false;
        if (usbPermissionReceiver != null && context != null) {
            context.unregisterReceiver(usbPermissionReceiver);
            context.unregisterReceiver(usbReceiver);
        }
        if (usbDeviceConnection != null) {
            usbDeviceConnection.releaseInterface(usbInterface);
            usbDeviceConnection.close();
        }

    }

    @Override
    public boolean writeAndFlushNoDelay(String data) {
        if (usbDeviceConnection != null && usbEndpointOut != null) {
            byte[] bytes = data.getBytes();
            int i = usbDeviceConnection.bulkTransfer(usbEndpointOut, bytes, bytes.length, 80);
            Log.i("写入指令：" + data);
            if (i < 0) {
                Log.w("写入指令失败：" + data);
            }
            return i > 0;
        }
        Log.w("写入指令失败：" + data);
        return false;
    }

    private void requestData() {
        while (isOpen) {
            try {
//                sleep();

                byte[] buffer = new byte[512];
                if (usbDeviceConnection == null || usbEndpointIn == null) return;
                int size = usbDeviceConnection.bulkTransfer(usbEndpointIn, buffer, buffer.length, 80);

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

    private void config() throws IOException {
        /*
          private final PL2303MultiLib.BaudRate mBaudRate = PL2303MultiLib.BaudRate.B115200;
          private final PL2303MultiLib.DataBits mDataBits = PL2303MultiLib.DataBits.D8;
          private final PL2303MultiLib.Parity mParity = PL2303MultiLib.Parity.NONE;
          private final PL2303MultiLib.StopBits mStopBits = PL2303MultiLib.StopBits.S1;
          private final PL2303MultiLib.FlowControl mFlowControl = PL2303MultiLib.FlowControl.XONXOFF;  // 是否有效？

          针对这5个设置，从pl2303源码中获得的设置波特率的方法
         */
        byte[] data = new byte[7];
        int ret = usbDeviceConnection.controlTransfer(161, 33, 0, 0, data, 7, 80);
        if (ret < 0) {
            throw new IOException("Error setting baud rate.");
        }

        int baudRate = 115200;
        data[0] = (byte) (baudRate & 255);
        data[1] = (byte) (baudRate >> 8 & 255);
        data[2] = (byte) (baudRate >> 16 & 255);
        data[3] = (byte) (baudRate >> 24 & 255);
        data[4] = 0;
        data[5] = 0;
        data[6] = 8;
        ret = usbDeviceConnection.controlTransfer(33, 32, 0, 0, data, 7, 80);
        if (ret < 0) {
            throw new IOException("Error setting baud rate.");
        }
        ret = usbDeviceConnection.controlTransfer(33, 35, 0, 0, (byte[]) null, 0, 80);
        if (ret < 0) {
            throw new IOException("Error setting baud rate.");
        }


        ret = usbDeviceConnection.controlTransfer(64, 1, 0, 193, (byte[]) null, 0, 80);
        if (ret < 0) {
            throw new IOException("Error setting baud rate.");
        }


        ret = usbDeviceConnection.controlTransfer(64, 1, 1, 0, (byte[]) null, 0, 80);
        if (ret < 0) {
            throw new IOException("Error setting baud rate.");
        }


        ret = usbDeviceConnection.controlTransfer(64, 1, 2, 68, (byte[]) null, 0, 80);
        if (ret < 0) {
            throw new IOException("Error setting baud rate.");
        }

    }
}
