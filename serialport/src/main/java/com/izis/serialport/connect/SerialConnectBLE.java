package com.izis.serialport.connect;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
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
import com.izis.serialport.util.FileUtil;
import com.izis.serialport.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class SerialConnectBLE extends SerialConnect {
    private static final String UUID_SERVER = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String UUID_READ = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final String UUID_WRITE = "0000ffe2-0000-1000-8000-00805f9b34fb";
    private static final String UUID_CCCD = "00002902-0000-1000-8000-00805f9b34fb";
    private final FragmentActivity activity;
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final List<Device> searchDevices = new ArrayList<>();
    private final ProgressDialog progressDialog;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristicWrite;
    private BluetoothGattDescriptor descriptor;
    private int mtu = 512 - 3;
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i("搜索到蓝牙设备：" + result.getDevice().getName() + result);
            Device device = new Device();
            device.bluetoothDevice = result.getDevice();
            if (!searchDevices.contains(device) && result.getDevice() != null && result.getDevice().getName() != null) {
                searchDevices.add(device);
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("蓝牙搜索失败：" + errorCode);
            onConnectFailNoReConnect();
        }
    };
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("------------------onConnectionStateChange: " + status + " " + newState);
            gatt.discoverServices();//5C:C3:36:00:37:64
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    closeConnectUI();
                    onConnectSuccess(gatt.getDevice().getName());
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    closeConnectUI();
                    close();
                    onConnectFailNoReConnect();
                    break;
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("request mtu 成功，约定后的MTU值为：" + mtu);
                SerialConnectBLE.this.mtu = mtu - 3;
            } else {
                Log.e("request mtu 失败");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //服务发现成功后，我们就可以进行数据相关的操作了，比如写入数据、开启notify等等
            Log.d("------------------onServicesDiscovered: " + gatt.getServices().size() + "个service");

            gatt.requestMtu(512);

            BluetoothGattService service = gatt.getService(UUID.fromString(UUID_SERVER));
//            BluetoothGattCharacteristic characteristicRead = service.getCharacteristic(UUID.fromString(UUID_READ));
            characteristicWrite = service.getCharacteristic(UUID.fromString(UUID_WRITE));

//            if (characteristicRead != null) {
//                for (BluetoothGattDescriptor descriptor : characteristicRead.getDescriptors()) {
//                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    gatt.writeDescriptor(descriptor);
//                }
//                gatt.setCharacteristicNotification(characteristicRead, true);
//            }

            if (characteristicWrite != null) {
                descriptor = characteristicWrite.getDescriptor(UUID.fromString(UUID_CCCD));
            }

            if (characteristicWrite != null)
                gatt.setCharacteristicNotification(characteristicWrite, true);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String content = new String(characteristic.getValue());
            checkData(content);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (index >= 0) {
                index += mtu;
                sendData(bytes, index);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (index == 0)
                sendData(bytes, index);
        }
    };

    public SerialConnectBLE(Context context) {
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
        stopScan();
        closeSearchUI();
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
    }

    int index = -1;
    byte[] bytes;

    @Override
    public boolean writeAndFlush(String commend) {
        if (descriptor != null) {
            descriptor.setValue("data".getBytes());
        }
        return super.writeAndFlush(commend);
    }

    @Override
    public void writeFile(File file) {
        if (descriptor != null) {
            descriptor.setValue("file".getBytes());
        }
        Log.d("更新文件，验证串口连接是否正常");
        if (isConnected()) {
            Log.d("更新文件, 串口连接正常，准备写入");
            byte[] data = FileUtil.getBytes(file);
            Log.d("更新文件，文件长度：" + (data == null ? 0 : data.length));
            if (data != null) {
                writeBytes(data);
            }
        } else {
            Log.e("棋盘未连接");
        }
    }

    @Override
    public boolean writeBytes(byte[] bytes) {
        if (characteristicWrite == null || bluetoothGatt == null || bytes == null || bytes.length == 0)
            return false;
        if (index < 0) {
            this.bytes = bytes;
            index = 0;
            if (descriptor != null) {
                bluetoothGatt.writeDescriptor(descriptor);
            } else {
                sendData(bytes, index);
            }
            return true;
        } else {
            return false;
        }
    }

    private void sendData(byte[] bytes, int offset) {
        if (characteristicWrite == null || bluetoothGatt == null || bytes == null || bytes.length == 0)
            return;

        int end = Math.min(offset + mtu, bytes.length);
        if (end >= bytes.length)
            index = -1;
        characteristicWrite.setValue(Arrays.copyOfRange(bytes, offset, end));
        bluetoothGatt.writeCharacteristic(characteristicWrite);

        Log.d(">>>>>>>>>写入进度>>>>>>>>>: " + end * 100.0 / bytes.length + "%");
    }

//    @Override
//    public boolean isConnected() {
//        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
//        return bluetoothManager != null && bluetoothGatt.getDevice() != null
//                && bluetoothManager.getConnectionState(bluetoothGatt.getDevice(), 1) == BluetoothProfile.STATE_CONNECTED;
//    }

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
//        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
//
//        Log.i("蓝牙已打开，发现已配对设备" + bondedDevices.size() + "台：");
//        for (BluetoothDevice bondedDevice : bondedDevices) {
//            Device device = new Device();
//            device.bluetoothDevice = bondedDevice;
//            device.type = 1;
//            if (!searchDevices.contains(device))
//                searchDevices.add(device);
//        }


        //请求位置权限并查找周围蓝牙设备
        requestGPS();
    }

    private boolean checkIfLocationOpened() {
        final LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * 请求位置权限
     */
    private void requestGPS() {
        boolean isGpsEnabled = checkIfLocationOpened();
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
        Log.i("蓝牙已打开，开始查找周围设备...");
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        showSearchUI();
        activity.runOnUiThread(() -> {
            btnReSearch.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        });

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                //10s后结束查找
                stopScan();
            }
        }, 10 * 1000);
    }

    public void checkGPSPermission() {
        PermissionsFragment.getInstance(activity)
                .setRequestPermissionsListener(new Callback() {
                    @Override
                    public void onSuccess() {
//                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//                        activity.registerReceiver(searchReceiver, filter);
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

    void stopScan() {
        Log.i("停止查找蓝牙设备");
        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        activity.runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnReSearch.setVisibility(View.VISIBLE);
        });
    }

    private BottomSheetDialog dialog;
    private final BluetoothUIAdapter adapter = new BluetoothUIAdapter(searchDevices, R.layout.bluetooth_search_ui_item);
    private Button btnReSearch;
    private ProgressBar progressBar;

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
                stopScan();
                connectState = ConnectState.DisConnect;
            });
            View view = activity.getLayoutInflater().inflate(R.layout.bluetooth_search_ui, null);
            progressBar = view.findViewById(R.id.progressBar);
            btnReSearch = view.findViewById(R.id.btnReSearch);
            btnReSearch.setOnClickListener(v -> scanBluetooth());
            RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            adapter.setOnItemClickListener(device -> {
                stopScan();
                dialog.dismiss();

                showConnectUI();

                bluetoothGatt = device.connectGatt(context, false, gattCallback);
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
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
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
