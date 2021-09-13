> 提供四种方式连接隐智电子棋盘。

- 使用PL2303官方提供的jar包：SerialConnectPl2303
- 使用google开源的android-serial-port（需要root权限）：SerialConnectJNI
- 使用android提供的API：SerialConnectAPI
- 使用物理串口直链：SerialConnectDirect
# 使用方式
```groovy

    allprojects {
        repositories {
            ...
            maven {
                url "https://jitpack.io"
            }
        }
    }

    implementation 'com.github.lunxinfeng:CodeLibSerialPort:0.1.6'
```

# 推荐的使用方式：
```java
    public static SerialConnect newInstance(Context context){
        //return new SerialConnectPl2303(context);
        //return new SerialConnectJNI(context);
        //return new SerialConnectAPI(context);
        return new SerialConnectDirect(context);
    }

    //...

    SerialConnect serialConnect = newInstance(context);
    serialConnect.setConnectListener();
    serialConnect.setReceiveDataListener();
    serialConnect.setSendDataListener();
    serialConnect.open();

    //...

    //sendNumMax为0时，两者等价
    //写入指令
    serialConnect.writeAndFLush();
    //已应答的方式写入指令，未收到响应最多发送3次
    serialConnect.addCommend();

    //...

    //如果需要关闭的时候清除缓存的未发指令，调用该方法；重连时不建议调用该方法，这也重连上会继续发送之前addCommend的未发指令
    serialConnect.clearCommend();
    //关闭连接
    serialConnect.close();
```
断开时默认会重连3次。
# 连接监听
```java
public interface SerialConnectListener {
    /**
     * 连接成功
     */
    void onConnectSuccess();

    /**
     * 连接失败，如果设置了自动重连，则在自动重连次数用完后，依然没连上时触发
     * @param connectNum  连接次数
     */
    void onConnectFail(int connectNum);

    /**
     * 异常断开，主动断开时不会触发
     */
    default onConnectError(){}
}
```
# 接收数据监听
```java
public interface SerialReceiveDataListener {
    /**
     * 接收到串口传输的正常数据
     * @param data 数据，已做处理，符合 ~?[A-Z]{3}[^~#]*# 格式
     */
    void onReceiveNormalData(String data);

    /**
     * 接收到串口传输的异常数据
     * @param data 数据，不符合 ~?[A-Z]{3}[^~#]*# 格式，或者有非数字、字母、小数点、英文逗号的字符
     */
    void onReceiveErrorData(String data);
}
```
# 发送数据监听
```java
public interface SerialSendDataListener {
    /**
     * 发送数据的监听
     * @param data 发送的数据
     * @param result 发送是否成功
     */
    void onSendData(String data, boolean result);
}
```
# 自定义有响应和需要延迟的指令
```java
    //添加有响应的指令
    ProtocolUtil.responseMap.put();

    //添加发完需要延迟再发下一条的指令
    ProtocolUtil.delayList.add();
    //指令延迟的倍率
    public static int delayTimes = 3;
    //每条指令的最小基础间隔
    public static int minDelay = 80;
```