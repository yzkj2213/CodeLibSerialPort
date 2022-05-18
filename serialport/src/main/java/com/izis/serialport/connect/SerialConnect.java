package com.izis.serialport.connect;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.izis.serialport.listener.SerialConnectListener;
import com.izis.serialport.listener.SerialReceiveDataListener;
import com.izis.serialport.listener.SerialSendDataListener;
import com.izis.serialport.util.FileUtil;
import com.izis.serialport.util.Log;
import com.izis.serialport.util.ProtocolUtil;

import java.io.File;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 连接状态
 */
enum ConnectState {
    /**
     * 未连接
     */
    DisConnect,
    /**
     * 正在连接
     */
    ConnectIng,
    /**
     * 已连接
     */
    Connected,
}

/**
 * 串口连接
 */
public abstract class SerialConnect {
    final Context context;
    SerialConnectListener connectListener;
    SerialReceiveDataListener receiveDataListener;
    SerialSendDataListener sendDataListener;
    ConnectState connectState = ConnectState.DisConnect;//连接状态
    private int connectNum = 0;//连接次数
    private int connectNumMax = 3;//最大重连次数
    private String lastCommend = "";//最后一条指令
    private long lastSendTime = 0;//最后一条指令的发送时间
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SerialConnect(Context context) {
        this.context = context;
    }

    public void setConnectNumMax(int connectNumMax) {
        this.connectNumMax = connectNumMax;
    }

    /**
     * 设置指令最小延迟间隔
     */
    public void setMinDelay(int time) {
        ProtocolUtil.minDelay = time;
    }

    /**
     * 设置需要多倍延迟指令的延迟倍率
     */
    public void setDelayTimes(int times) {
        ProtocolUtil.delayTimes = times;
    }

    /**
     * 添加需要验证响应的指令
     */
    public void addResponseCommend(String key, String value) {
        ProtocolUtil.responseMap.put(key, value);
    }

    /**
     * 设置添加需要多倍延迟的指令
     */
    public void addDelayCommend(String commend) {
        ProtocolUtil.delayList.add(commend);
    }

    /**
     * 打开连接
     */
    public synchronized void open() {
        if (connectState != ConnectState.DisConnect) return;
        connectState = ConnectState.ConnectIng;
        connectNum++;

        openConnect();
    }

    /**
     * 关闭连接， 默认清除缓存指令
     */
    public void close() {
        close(true);
    }

    public void close(boolean cleanCacheCommend) {
        connectState = ConnectState.DisConnect;
        //主动断开时清除缓存指令
        if (cleanCacheCommend)
            clearCommend();

        disConnect();
    }

    //打开连接
    abstract void openConnect();

    //断开连接
    abstract void disConnect();

    public boolean isConnected() {
        return connectState == ConnectState.Connected;
    }

    /**
     * 延迟80ms写入数据
     *
     * @param commend 数据
     * @return 写入是否成功
     */
    public boolean writeAndFlush(String commend) {
        if (TextUtils.isEmpty(commend) || !isConnected()) {
            onSendData(commend, false);
            return false;
        }

        int delayTime = ProtocolUtil.delayList.contains(key(lastCommend))
                ? ProtocolUtil.minDelay * ProtocolUtil.delayTimes
                : ProtocolUtil.minDelay;
        long l = System.currentTimeMillis();
        if (l - lastSendTime < delayTime) {
            try {
                Thread.sleep(delayTime - (l - lastSendTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lastSendTime = System.currentTimeMillis();
        lastCommend = commend;

        if (TextUtils.isEmpty(commend) || !isConnected()) {
            onSendData(commend, false);
            return false;
        }
        boolean result = writeBytes(commend.getBytes());
        onSendData(commend, result);

        return result;
    }

    /**
     * 写文件，
     */
    public void writeFile(File file) {
        executorService.execute(() -> {
            Log.d("更新文件，验证串口连接是否正常");
            if (isConnected()) {
                Log.d("更新文件, 串口连接正常，准备写入");
                byte[] data = FileUtil.getBytes(file);
                Log.d("更新文件，文件长度：" + (data == null ? 0 : data.length));
                if (data != null) {
                    int max = data.length / 1024 + 1;
                    for (int i = 0; i < max; i++) {
                        int length = Math.min(data.length - i * 1024, 1024);
                        byte[] msg = new byte[length];
                        System.arraycopy(data, i * 1024, msg, 0, length);
                        Log.d("更新文件，进度：" + (i + 1) * 100.0 / max + "%");
                        if (isConnected()) {
                            writeBytes(msg);
                        }
                    }
                    Log.d("更新文件，写入完毕");
                }
            } else {
                Log.e("棋盘未连接");
                onConnectFailNoReConnect();
            }
        });
    }

    /**
     * 立即写入数据
     *
     * @param bytes 数据
     * @return 写入是否成功
     */
    public abstract boolean writeBytes(byte[] bytes);

    void sleep() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //------------------指令的重发
    private final LinkedList<String> commendList = new LinkedList<>();
    private Timer timerSendCommend;
    private int sendNumMax = 3;//最大重发次数
    private int sendNum = 0;

    public void setSendNumMax(int sendNumMax) {
        this.sendNumMax = sendNumMax;
    }

    /**
     * 清除缓存的指令列表
     */
    public synchronized void clearCommend() {
        commendList.clear();
    }

    /**
     * 以应答的方式发送指令
     *
     * @param commend 指令
     */
    public synchronized void addCommend(String commend) {
        commendList.add(commend);
        if (commendList.size() == 1) {
            send();
        }
    }

    private String getCommend() {
        if (!commendList.isEmpty()) {
            return commendList.getFirst();
        }
        return "";
    }

    private void send() {
        String commend = getCommend();
        if (commend == null || commend.isEmpty()) return;

        if (sendNum > sendNumMax) {
            loopNext();
            return;
        }
        boolean result = writeAndFlush(commend);
        if (result)
            sendNum++;//写入成功才累加发送次数，如果棋盘断开，则一直尝试，直到棋盘连上或者清除缓存指令
        if (!hasResponse(commend)) {
            loopNext();
            return;
        }

        if (timerSendCommend != null) timerSendCommend.cancel();
        timerSendCommend = new Timer();
        timerSendCommend.schedule(new TimerTask() {
            @Override
            public void run() {
                send();
            }
        }, 500);
    }

    private synchronized void loopNext() {
        if (timerSendCommend != null) timerSendCommend.cancel();

        sendNum = 0;

        if (!commendList.isEmpty()) {
            commendList.removeFirst();
            send();
        }
    }

    private String key(String commend) {
        if (commend.length() >= 4) {
            return commend.substring(0, 4);
        } else {
            return "";
        }
    }

    private boolean hasResponse(String commend) {
        return ProtocolUtil.responseMap.containsKey(key(commend));
    }

    private boolean isResponse(String commend, String result) {
        return hasResponse(commend) && result.contains(ProtocolUtil.responseMap.get(key(commend)));
    }

    //------------------处理接收到的数据
    private String readTemp = ""; // 存储接受到的非完整数据

    public void checkData(String curReadData) {
        Log.d("已缓存数据:" + readTemp);
        Log.d("本次读取数据:" + curReadData);

        readTemp = String.format("%s%s", readTemp, curReadData); // 得到最完整的读取池数据

        Log.d("初步处理数据:" + readTemp);

        String totalCommands;
        //缓存中完整数据后的半截数据
        String tempRest;
        if (readTemp.lastIndexOf("#") > 0) {//包含了#号（不包含则说明不包含一个完整的指令）
            if (readTemp.lastIndexOf("#") < readTemp.length() - 1) {//包含了部分下一条指令
                tempRest = readTemp.substring(readTemp.lastIndexOf("#") + 1); // 得到最后的#号之后的半截数据
            } else {
                tempRest = "";
            }

            totalCommands = readTemp.substring(0, readTemp.lastIndexOf("#") + 1); // 完整的指令集合
        } else { // 不包含结束符号，说明指令尚不完整，继续等待下一帧。
            return; // 继续下一次循环
        }

        Log.d("所有指令，可能包含多个:" + totalCommands);

        // ==================包含#号
        // 解析数据，分解成一条条指令后交给前台调用者处理===================

        String p = "~?[A-Z]{3}[^~#]*#";
        Pattern r = Pattern.compile(p);
        Matcher m = r.matcher(totalCommands);
        while (m.find()) {
            String group = m.group();


            Pattern pattern = Pattern.compile("[^A-Za-z0-9~#.,]");
            Matcher matcher = pattern.matcher(group);
            if (matcher.find()) {
                onReceiveErrorData(group);
            } else {
                onReceiveNormalData(group);
            }


            totalCommands = totalCommands.replace(group, "");

            if (isResponse(lastCommend, group)) {
                loopNext();
            }
        }

        if (totalCommands.length() != 0) {
            onReceiveErrorData(totalCommands);
        }

        readTemp = tempRest;
    }

    //-----------------------setter,getter--------------------
    public void setConnectListener(SerialConnectListener connectListener) {
        this.connectListener = connectListener;
    }

    public void setReceiveDataListener(SerialReceiveDataListener receiveDataListener) {
        this.receiveDataListener = receiveDataListener;
    }

    public void setSendDataListener(SerialSendDataListener sendDataListener) {
        this.sendDataListener = sendDataListener;
    }

    private final Handler main = new Handler(Looper.getMainLooper());

    void onConnectSuccess(String deviceName) {
        Log.i("连接设备 " + deviceName + " 成功");
        connectState = ConnectState.Connected;
        connectNum = 0;
        if (connectListener != null)
            main.post(() -> connectListener.onConnectSuccess());
    }

    void onConnectFail() {
        connectState = ConnectState.DisConnect;
        if (connectNum <= connectNumMax) {
            //默认重连 connectNumMax 次
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    open();
                }
            }, 1200);
        } else {
            onConnectFailNoReConnect();
        }
    }

    void onConnectFailNoReConnect() {
        connectState = ConnectState.DisConnect;
        //重连失败后回调通知
        if (connectListener != null)
            main.post(() -> connectListener.onConnectFail(connectNum));
    }


    void onConnectError(String deviceName) {
        Log.e(deviceName + " 连接中断");
        connectState = ConnectState.DisConnect;
        //异常中断直接重连
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                open();
            }
        }, 1500);

        if (connectListener != null)
            main.post(() -> connectListener.onConnectError());
    }

    void onReceiveNormalData(String data) {
        Log.i("得到一条正常指令:" + data);
        if (receiveDataListener != null)
            main.post(() -> receiveDataListener.onReceiveNormalData(data));
    }

    void onReceiveErrorData(String data) {
        Log.w("得到一条异常指令:" + data);
        if (receiveDataListener != null)
            main.post(() -> receiveDataListener.onReceiveErrorData(data));
    }

    void onSendData(String data, boolean result) {
        if (result) {
            Log.i("写入指令：" + data);
        } else {
            Log.w("写入指令失败：" + data);
        }
        if (sendDataListener != null)
            main.post(() -> sendDataListener.onSendData(data, result));
    }
}
