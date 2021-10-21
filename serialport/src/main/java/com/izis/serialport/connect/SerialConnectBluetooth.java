package com.izis.serialport.connect;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.izis.serialport.R;
import com.izis.serialport.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ConnectThread implements Runnable {
    private final SerialConnectBluetooth serialConnectBluetooth;
    private final BluetoothSocket bluetoothSocket;
    private final BluetoothDevice bluetoothDevice;
    private InputStream inputStream;
    private OutputStream outputStream;

    public ConnectThread(SerialConnectBluetooth serialConnectBluetooth, BluetoothDevice device) {
        this.serialConnectBluetooth = serialConnectBluetooth;
        this.bluetoothDevice = device;
        BluetoothSocket tmp = null;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
//            tmp = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
//            tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.randomUUID());
//            tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("socket创建失败");
        }
        bluetoothSocket = tmp;
    }

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void run() {
        try {
            Log.i("开始连接蓝牙设备：" + bluetoothDevice.getName());
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();

            serialConnectBluetooth.onConnectSuccess(bluetoothDevice.getName());
            main.post(serialConnectBluetooth::closeConnectUI);

            requestData();
        } catch (IOException connectException) {
            connectException.printStackTrace();
            Log.w("连接设备 " + bluetoothDevice.getName() + " 失败");
            serialConnectBluetooth.onConnectFailNoReConnect();
            main.post(serialConnectBluetooth::closeConnectUI);
            // Unable to connect; close the socket and return.
            try {
                bluetoothSocket.close();
            } catch (IOException closeException) {
                Log.e("无法关闭socket");
            }
        }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e("无法关闭socket");
        }
    }

    boolean writeAndFlush(byte[] bytes) {
        try {
            outputStream.write(bytes);
            outputStream.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void requestData() {
        while (serialConnectBluetooth.isConnected()) {
            try {
                byte[] buffer = new byte[1024];
                if (inputStream == null) return;
                int size = inputStream.read(buffer);
                System.out.println(size);
                if (size > 0) {
                    byte[] readBytes = new byte[size];
                    System.arraycopy(buffer, 0, readBytes, 0, size);
                    String curReadData = new String(readBytes);

                    serialConnectBluetooth.checkData(curReadData);
                }
            } catch (Exception e) {
                e.printStackTrace();
                serialConnectBluetooth.onConnectError(bluetoothDevice.getName());
            }
        }
    }
}

class Device {
    BluetoothDevice bluetoothDevice;
    // 0 新设备  1 已配对
    int type;

    @Override
    public boolean equals(Object o) {
        if (o instanceof BluetoothDevice) {
            return bluetoothDevice.getAddress().equals(((BluetoothDevice) o).getAddress());
        } else if (o instanceof Device) {
            return bluetoothDevice.getAddress().equals(((Device) o).bluetoothDevice.getAddress());
        }
        return false;
    }
}

public class SerialConnectBluetooth extends SerialConnect {

    private final FragmentActivity activity;
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final List<Device> searchDevices = new ArrayList<>();
    private ConnectThread connectThread;
    private final ProgressDialog progressDialog;
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver searchReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Device device = new Device();
                device.bluetoothDevice = bluetoothDevice;
                Log.d("查找到蓝牙设备：" + bluetoothDevice.getName() + "\t" + bluetoothDevice.getAddress());
                if (!searchDevices.contains(device) && bluetoothDevice.getName() != null) {
                    Log.i("新增蓝牙设备：" + bluetoothDevice);
                    searchDevices.add(device);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };
//    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            switch (intent.getAction()) {
//                case BluetoothAdapter.ACTION_STATE_CHANGED:
//                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
//
//                    switch (blueState) {
//                        case BluetoothAdapter.STATE_TURNING_ON:
//                            break;
//                        case BluetoothAdapter.STATE_ON:
//                            break;
//                        case BluetoothAdapter.STATE_TURNING_OFF:
//                            break;
//                        case BluetoothAdapter.STATE_OFF:
//                            break;
//                    }
//                    break;
//            }
//        }
//    };

    public SerialConnectBluetooth(Context context) {
        super(context);
        if (context instanceof FragmentActivity) {
            this.activity = (FragmentActivity) context;
            this.progressDialog = new ProgressDialog(activity);
            this.progressDialog.setCancelable(false);
            this.progressDialog.setMessage("正在连接...");
        } else {
            throw new RuntimeException("蓝牙连接方式需要传入的上下文为FragmentActivity");
        }
    }

    @Override
    void openConnect() {
        if (bluetoothAdapter == null) {
            Log.e("该设备不支持蓝牙");
            onConnectFailNoReConnect();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.i("蓝牙未打开，请求打开蓝牙");
            requestBlueTooth();
        } else {
            getDevice();
        }
    }

    @Override
    void disConnect() {
        try {
            activity.unregisterReceiver(searchReceiver);
        } catch (Exception e) {
            //有可能没有注册
        }
        cancelDiscovery();
        closeSearchUI();
        if (connectThread != null)
            connectThread.cancel();
    }

    @Override
    boolean writeAndFlushNoDelay(byte[] bytes) {
        return connectThread != null && connectThread.writeAndFlush(bytes);
    }

    private void requestBlueTooth() {
        PermissionsFragment.getInstance(activity)
                .setStartActivityForResultListener(new Callback() {
                    @Override
                    public void onSuccess() {
                        getDevice();
                    }

                    @Override
                    public void onFail() {
                        Log.w("打开蓝牙请求被拒绝");
                        onConnectFailNoReConnect();
                    }
                })
                ._startActivityForResult(
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                );
    }

    /**
     * 获取设备
     */
    private void getDevice() {
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        Log.i("蓝牙已打开，发现已配对设备" + bondedDevices.size() + "台：");
        for (BluetoothDevice bondedDevice : bondedDevices) {
            Device device = new Device();
            device.bluetoothDevice = bondedDevice;
            device.type = 1;
            if (!searchDevices.contains(device))
                searchDevices.add(device);
        }


        //请求位置权限并查找周围蓝牙设备
        requestGPS();
    }

    /**
     * 请求位置权限
     */
    private void requestGPS() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGpsEnabled) {
            Log.i("GPS未打开，请求打开GPS");
            PermissionsFragment.getInstance(activity)
                    .setStartActivityForResultListener(new Callback() {
                        @Override
                        public void onSuccess() {
                            checkGPSPermission();
                        }

                        @Override
                        public void onFail() {
                            //部分手机打开GPS后返回，得到的result_code 为cancel，原因不明，因此在判断一次
                            LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                            if (isGpsEnabled) {
                                checkGPSPermission();
                            } else {
                                Log.w("打开GPS请求被拒绝");
                                onConnectFailNoReConnect();
                            }
                        }
                    })
                    ._startActivityForResult(
                            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    );
        } else {
            checkGPSPermission();
        }
    }

    /**
     * 查找蓝牙设备
     */
    private void scanBluetooth() {
        boolean startDiscovery = bluetoothAdapter.startDiscovery();
        if (startDiscovery) {
            Log.i("蓝牙已打开，未发现已配对设备，开始查找周围设备...");
            showSearchUI();
            activity.runOnUiThread(() -> {
                btnReSearch.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            });

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    //10s后结束查找
                    cancelDiscovery();
                }
            }, 20 * 1000);
        } else {
            Log.w("蓝牙已打开，未发现已配对设备，查找进程未正常启动");
            onConnectFailNoReConnect();
        }
    }

    public void checkGPSPermission() {
        PermissionsFragment.getInstance(activity)
                .setRequestPermissionsListener(new Callback() {
                    @Override
                    public void onSuccess() {
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        activity.registerReceiver(searchReceiver, filter);
                        scanBluetooth();
                    }

                    @Override
                    public void onFail() {
                        Log.w("打开位置权限请求被拒绝");
                        onConnectFailNoReConnect();
                    }
                })
                ._requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }
                );
    }

    void cancelDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            Log.i("停止查找蓝牙设备");
            activity.runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnReSearch.setVisibility(View.VISIBLE);
            });
        }
    }

    private BottomSheetDialog dialog;
    private final BluetoothUIAdapter adapter = new BluetoothUIAdapter(searchDevices, R.layout.bluetooth_search_ui_item);
    private Button btnReSearch;
    private ProgressBar progressBar;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private void showConnectUI() {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    void closeConnectUI() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void showSearchUI() {
        if (dialog == null) {
            dialog = new BottomSheetDialog(activity);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnDismissListener(dialog -> {
                cancelDiscovery();
                connectState = ConnectState.DisConnect;
            });
            View view = activity.getLayoutInflater().inflate(R.layout.bluetooth_search_ui, null);
            progressBar = view.findViewById(R.id.progressBar);
            btnReSearch = view.findViewById(R.id.btnReSearch);
            btnReSearch.setOnClickListener(v -> scanBluetooth());
            RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            adapter.setOnItemClickListener(device -> {
                cancelDiscovery();
                dialog.dismiss();

                showConnectUI();

                connectThread = new ConnectThread(SerialConnectBluetooth.this, device);
                executorService.execute(connectThread);
            });
            recyclerView.setAdapter(adapter);
            dialog.setContentView(view);

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) (view.getParent()));
            behavior.setDraggable(false);
        }
        dialog.show();
    }

    private void closeSearchUI() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }

    static class BluetoothUIAdapter extends RecyclerView.Adapter<BluetoothUIAdapter.MyViewHolder> {
        private final List<Device> data;
        private final int layoutRes;
        private OnItemClickListener onItemClickListener;

        public BluetoothUIAdapter(List<Device> data, int layoutRes) {
            this.data = data;
            this.layoutRes = layoutRes;
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        @NonNull
        @Override
        public BluetoothUIAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BluetoothUIAdapter.MyViewHolder holder, int position) {
            holder.tvName.setText(data.get(position).bluetoothDevice.getName());
            holder.tvType.setText(data.get(position).type == 1 ? "已配对" : "");
            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null)
                    onItemClickListener.onItemClick(data.get(position).bluetoothDevice);
            });
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvType;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvType = itemView.findViewById(R.id.tvType);
            }
        }
    }

    interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }
}
