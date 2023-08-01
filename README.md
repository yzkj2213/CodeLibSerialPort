# 隐智科技电子棋盘

- [产品示意图](#产品示意图)
- [集成](#集成)
- [开发工具包](#开发工具包)
  - [使用方式](#使用方式)
  - [连接监听](#连接监听)
  - [接收数据监听](#接收数据监听)
  - [发送数据监听](#发送数据监听)
  - [自定义有响应和需要延迟的指令](#自定义有响应和需要延迟的指令)
- [底层协议](#底层协议)
  - [核心指令](#核心指令)
    - [获取版本号](#获取版本号)
    - [请求全盘信息](#请求全盘信息)
    - [底层是否主动发送全盘变化](#底层是否主动发送全盘变化)
    - [切换棋盘路数](#切换棋盘路数)
    - [棋盘亮多个灯](#棋盘亮多个灯)
    - [棋盘亮单个灯](#棋盘亮单个灯)
    - [切换行棋指示灯](#切换行棋指示灯)
    - [关闭棋盘灯](#关闭棋盘灯)
    - [棋盘报警提示音](#棋盘报警提示音)
    - [拍钟](#拍钟)
  - [一些辅助指令](#一些辅助指令)


## 产品示意图

![产品示意图](/img/board.png)

## 集成

- [开发工具包](#开发工具包)。
- 电子屏幕桌面经过处理，只能展示出appId以`cn.izis`开头的应用


## 开发工具包

> 提供五种方式连接不同版本的隐智电子棋盘。

- ~~使用PL2303官方提供的jar包**<3 和 3 Plus>**：SerialConnectPl2303~~
- ~~使用google开源的android-serial-port（需要root权限）：SerialConnectJNI~~
- ~~使用android提供的API：SerialConnectAPI~~
- 使用物理串口直连**<3 Plus 5G>**：SerialConnectDirect  **推荐**
- 使用桌面服务的方式**<所有版本>**：SerialConnectService

> 也可以基于其他语言或服务自己实现，核心就是找到并打开Android开发板的`/dev/ttyS1`串口。

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

    implementation 'com.github.yzkj2213:CodeLibSerialPort:1.1.0'
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

# 底层协议

## 协议格式
> 一般是上位机主动向单片机发送指令，发送 ASCII 码（文本模式），串口波特率为115200。每条指令都是以~开头，都是以#符结尾，格式为`~[A-Z]{3}[^~#]*#`。每个指令发
送完毕后需要间隔 80ms 才能发送下一条指令。

- `~`：指令头
- `[A-Z]{3}`：固定3个大写字母，用以区分不同的指令。
- `[^~#]*`：指令携带的数据参数。
- `#`：指令尾

指令大概分两类：
- 下发指令：由上层主动下发，操作棋盘，或者获取当前棋盘的一些状态信息，大部分指令会有反馈
- 上发指令：由物理动作触发的底层主动发送数据到上层，比如拍钟

## 核心指令
### 获取版本号
> 发送：~GVE#
> 返回：~VER100#

表示当前底层版本号为100。

### 请求全盘信息
> 发送：~STA#
> 返回：~SDA0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000#

- ~SDA：数据头，表示该数据是全盘信息
- 中间数字：实时的棋盘数据
  - 0表示该位置为空白，1表示该位置有黑子，2表示该位置有白子
  - 数字长度为设置的棋盘总子数，19路为361，9路为81，依次类推
  - 顺序以黑方左手边为第一个位置，向右依次递增，每行都是从左边开始
- #：数据尾，表示该条数据到此结束

### 底层是否主动发送全盘变化
> 发送：~CTS1# / ~CTS0#
> 返回：~UDS1# / ~UDS0#

- 1表示底层在棋盘数据发生变化（比如棋子在棋盘上滑动，新落子等等）时实时发送，默认为1。
- 0表示底层不主动发送棋盘数据，上层可以在需要获取数据时主动发送`~STA#`来获取全盘数据。

### 切换棋盘路数
> 发送：~BOD19# / ~BOD13# / ~BOD09#
> 返回：~GBS19# / ~GBS13# / ~GBS09#

- 数据数字代表棋盘路数，默认为19路，且仅支持9、13、19路。
- 切换路数后，9路和13路会亮边界灯，返回的全盘信息数据长度会相应变化。

### 棋盘亮多个灯
> 发送：~SAR0010002000001100000....0000#
> 返回：~ALS#

- 数据长串长度限制为361，按棋盘点位置顺序拼接，每个点数字可以为0-9，0表示不亮灯，1-9预制了9种不同颜色的灯。

### 棋盘亮单个灯
> 发送：~SHP001,r100g100b100,1#
> 返回：~HCS#

- `001`表示棋盘的第一个位置点，必须为3位数。
- `r100g100b100`表示rgb色值分别为100，色值取值范围为000-255。
- `1`预留，暂未处理。

### 切换行棋指示灯
> 发送：~LED11# / ~LED21# / ~LED10# / ~LED20#
> 返回：~LOS#

- 数据第一个数字：1表示亮黑方灯，2表示亮白方灯，亮一个时会自动关闭另一个。
- 数据第二个数字：1表示亮灯，0表示关灯。

### 关闭棋盘灯
> 发送：~RGC# / ~RGF#
> 返回：~ALC# / ~SIC#

- `RGC`表示关闭所有灯，不包括行棋指示灯。
- `RGF`表示关闭9路和13路的灯，不包括边界和行棋指示灯。

### 棋盘报警提示音
> 发送：~AWO#
> 无返回

- 棋盘发出滴滴的声音进行报警提示，一般可以用在棋盘和真实盘面不符时。

### 拍钟
> 黑方拍钟返回：~BKY#
> 白方拍钟返回：~WKY#

拍击棋钟时，底层会主动发送指令。

## 一些辅助指令

- `~RLT#`: 亮一个对号。
- `~RLW#`: 亮一个错号。
- `~RLO#`: 亮一个OK。