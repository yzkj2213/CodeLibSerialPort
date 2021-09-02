package com.izis.serialport.connect;

import com.izis.serialport.listener.SerialConnectListener;
import com.izis.serialport.listener.SerialDataListener;
import com.izis.serialport.util.Log;
import com.izis.serialport.util.ProtocolUtil;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 串口连接
 */
public abstract class SerialConnect {
    SerialConnectListener connectListener;
    SerialDataListener dataListener;
    int connectNum = 0;//连接次数
    private long lastSendTime;

    public void setConnectListener(SerialConnectListener connectListener) {
        this.connectListener = connectListener;
    }

    public void setDataListener(SerialDataListener dataListener) {
        this.dataListener = dataListener;
    }

    /**
     * 打开连接
     */
    public abstract void open();

    /**
     * 关闭连接
     */
    public abstract void close();

    /**
     * 延迟80ms写入数据
     *
     * @param commend 数据
     * @return 写入是否成功
     */
    public boolean writeAndFlush(String commend) {
        int delayTime = ProtocolUtil.delayList.contains(key(commend))
                ? ProtocolUtil.minDelay * ProtocolUtil.delayTimes
                : ProtocolUtil.minDelay;
        long l = System.currentTimeMillis();
        if (l - lastSendTime < delayTime) {
            try {
                Thread.sleep(l - lastSendTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lastSendTime = l;
        return writeAndFlushNoDelay(commend);
    }

    /**
     * 立即写入数据
     *
     * @param commend 数据
     * @return 写入是否成功
     */
    abstract boolean writeAndFlushNoDelay(String commend);

    void sleep() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //------------------指令的重发
    private final LinkedList<String> commendList = new LinkedList<>();
    private String currCommend = "";
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
            currCommend = nextCommend();
            send(currCommend);
        }
    }

    private String nextCommend() {
        if (!commendList.isEmpty()) {
            return commendList.getFirst();
        }
        return "";
    }

    private void send(String commend) {
        if (commend == null || commend.isEmpty()) return;

        if (sendNum > sendNumMax) {
            loopNext();
            return;
        }
        sendNum++;
        writeAndFlush(commend);
        if (!hasResponse(commend)) {
            loopNext();
            return;
        }

        if (timerSendCommend != null) timerSendCommend.cancel();
        timerSendCommend = new Timer();
        timerSendCommend.schedule(new TimerTask() {
            @Override
            public void run() {
                send(commend);
            }
        }, 200);
    }

    private void clearCurrCommend() {
        currCommend = "";
        sendNum = 0;
    }

    private synchronized void loopNext() {
        if (timerSendCommend != null) timerSendCommend.cancel();
        if (currCommend == null || currCommend.isEmpty()) return;
        clearCurrCommend();
        if (!commendList.isEmpty()) {
            commendList.removeFirst();
            currCommend = nextCommend();
            send(currCommend);
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

//        int firstStartCharIndex = readTemp.indexOf("~");
//
//        if (firstStartCharIndex > 0) {// 总池子不是以“~”开头，有故障。将开头数据摒弃。（-1，0，>0）
//            readTemp = readTemp.substring(firstStartCharIndex);
//        }

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


            Pattern pattern = Pattern.compile("[^A-Za-z0-9~#]");
            Matcher matcher = pattern.matcher(group);
            if (matcher.find()) {
                Log.w("得到一条异常指令:" + group);
                if (dataListener != null)
                    dataListener.onErrorData(group);
            } else {
                Log.i("得到一条正常指令:" + group);
                if (dataListener != null)
                    dataListener.onNormalData(group);
            }


            totalCommands = totalCommands.replace(group, "");

            if (isResponse(currCommend, group)) {
                loopNext();
            }
        }

        if (totalCommands.length() != 0) {
            Log.w("得到一条异常指令:" + totalCommands);
            if (dataListener != null)
                dataListener.onErrorData(totalCommands);
        }

        readTemp = tempRest;
    }
}
