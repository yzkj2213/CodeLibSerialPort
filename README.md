# 隐智科技电子棋盘

- [产品示意图](#产品示意图)
- [集成](#集成)
- [开发工具包](#开发工具包)
  - [使用方式](#使用方式)
  - [连接监听](#连接监听)
  - [接收数据监听](#接收数据监听)
  - [发送数据监听](#发送数据监听)
  - [自定义有响应和需要延迟的指令](#自定义有响应和需要延迟的指令)
- [底层协议（常用部分）](底层协议（常用部分）)
  - [下发指令](#下发指令)
  - [上发指令](#上发指令)


## 产品示意图

![产品示意图](/app/img/board.png)

## 集成

- [开发工具包](#开发工具包)。
- 电子屏幕桌面经过处理，只能展示出appId以`cn.izis`开头的应用



## 开发工具包

> 提供五种方式连接不同版本的隐智电子棋盘。

- 使用PL2303官方提供的jar包**<3 和 3 Plus>**：SerialConnectPl2303
- 使用google开源的android-serial-port（需要root权限）：SerialConnectJNI
- 使用android提供的API：SerialConnectAPI
- 使用物理串口直连**<3 Plus 5G>**：SerialConnectDirect
- 使用桌面服务的方式**<所有版本>**：SerialConnectService
### 使用方式
```groovy

    allprojects {
        repositories {
            ...
            maven {
                url "https://jitpack.io"
            }
        }
    }

    implementation 'com.github.lunxinfeng:CodeLibSerialPort:1.0.0'
```

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
    //已应答的方式写入指令，连接正常但是未收到响应时最多发送3次，若未连接棋盘会持续到棋盘连接成功为止
    serialConnect.addCommend();

    //...

    //清除缓存的未发指令
    serialConnect.clearCommend();
    //关闭连接
    serialConnect.close();
```
异常断开时默认会重连3次。
### 连接监听
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
### 接收数据监听
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
### 发送数据监听
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
### 自定义有响应和需要延迟的指令
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

# 底层协议（常用部分）

> 底层命令以`~`开头，以`#`结尾。**`BoardProtocol`**类罗列了当前最新版棋盘<3 Plus 5G>支持的常用命令。

指令大概分两类：

- 下发指令：由上层主动下发，操作棋盘，或者获取当前棋盘的一些状态信息，大部分指令会有反馈
- 上发指令：由物理动作触发的底层主动发送数据到上层，比如拍钟

## 下发指令

以请求全盘为例：

```java
    /**
     * 请求全盘信息
     */
    public static String requestAllChess() {
        return "~STA#";
    }
```

返回数据：

```java
~SDA0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000#
```

- ~SDA：数据头，表示该数据是全盘信息
- 中间数字：实时的棋盘数据
  - 0表示该位置为空白，1表示该位置有黑子，2表示该位置有白子
  - 数字长度为设置的棋盘总子数，19路为361，9路为81，依次类推
  - 顺序以黑方左手边为第一个位置，向右依次递增，每行都是从左边开始
- #：数据尾，表示该条数据到此结束

## 上发指令

以拍黑方棋钟为例：

```java
/**
 * 黑方拍钟
 */
public static final String clickBlack = "~BKY#";

```

拍击黑方棋钟时，底层会主动发送指令`~BKY#`。