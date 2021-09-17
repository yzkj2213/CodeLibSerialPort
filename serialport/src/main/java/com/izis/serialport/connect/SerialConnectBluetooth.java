package com.izis.serialport.connect;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.izis.serialport.R;
import com.izis.serialport.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

class SerialConnectBluetooth extends SerialConnect {

    private final FragmentActivity activity;
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final List<BluetoothDevice> searchDevices = new ArrayList<>();
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("查找到蓝牙设备：" + device);
                if (!searchDevices.contains(device)){
                    searchDevices.add(device);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    public SerialConnectBluetooth(Context context) {
        super(context);
        if (context instanceof FragmentActivity) {
            this.activity = (FragmentActivity) context;
        } else {
            throw new RuntimeException("蓝牙连接方式需要传入的上下文为Activity");
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
            activity.unregisterReceiver(receiver);
        } catch (Exception e) {
            //有可能没有注册
        }
        cancelDiscovery();
    }

    @Override
    boolean writeAndFlushNoDelay(byte[] bytes) {
        return false;
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
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            Log.i("蓝牙已打开，发现已配对设备" + pairedDevices.size() + "台：" + pairedDevices);
            // 已配对设备
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        } else {
            //请求位置权限并查找周围蓝牙设备
            requestGPS();
        }
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
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(receiver, filter);
        boolean startDiscovery = bluetoothAdapter.startDiscovery();
        if (startDiscovery) {
            Log.i("蓝牙已打开，未发现已配对设备，开始查找周围设备...");
            showSearchUI();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    //10s后结束查找
                    cancelDiscovery();
                }
            }, 10 * 1000);
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

    private void cancelDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            Log.i("停止查找蓝牙设备");
            closeSearchUI();
        }
    }

    private BottomSheetDialog dialog;
    private final BluetoothUIAdapter adapter = new BluetoothUIAdapter(searchDevices, R.layout.bluetooth_search_ui_item);

    private void showSearchUI() {
        if (dialog == null)
            dialog = new BottomSheetDialog(activity);
        dialog.setCancelable(false);
        View view = activity.getLayoutInflater().inflate(R.layout.bluetooth_search_ui, null);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(adapter);
        dialog.setContentView(view);
        dialog.show();
    }

    private void closeSearchUI() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }

    static class BluetoothUIAdapter extends RecyclerView.Adapter<BluetoothUIAdapter.MyViewHolder> {
        private final List<BluetoothDevice> data;
        private final int layoutRes;

        public BluetoothUIAdapter(List<BluetoothDevice> data, int layoutRes) {
            this.data = data;
            this.layoutRes = layoutRes;
        }

        @NonNull
        @Override
        public BluetoothUIAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull BluetoothUIAdapter.MyViewHolder holder, int position) {
            holder.tvName.setText(data.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
            }
        }
    }
}
