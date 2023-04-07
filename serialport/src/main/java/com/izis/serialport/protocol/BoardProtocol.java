package com.izis.serialport.protocol;

import java.util.List;

/**
 * 电子棋盘协议
 */
public class BoardProtocol {
    /**
     * 下发的指令
     */
    public static class Down {
        /**
         * 发送指令指定当前对局为几路棋盘
         *
         * @param boardSize 棋盘路数
         * @return 底层指令
         */
        public static String boardSize(int boardSize) {
            if (boardSize != 9 && boardSize != 13 && boardSize != 19) {
                throw new RuntimeException("boardSize must be one of 9，13，19");
            }
            if (boardSize < 10) {
                return "~BOD0" + boardSize + "#";
            } else {
                return "~BOD" + boardSize + "#";
            }
        }

        /**
         * 请求全盘信息
         */
        public static String requestAllChess() {
            return "~STA#";
        }

        /**
         * 黑方白方的指示灯
         *
         * @param bw 1黑 2白
         */
        public static String lamp(int bw) {
            return "~LED" + bw + "1#";
        }

        /**
         * 关闭所有指示灯
         */
        public static String closeAllLamp() {
            return "~RGC#";
        }

        /**
         * 底层发滴滴警告声音
         */
        public static String warning() {
            return "~AWO#";
        }

        /**
         * 底层是否主动发全盘变化
         *
         * @param send true表示棋盘发送变化时主动发送数据，反之false
         */
        public static String autoSendAllChess(boolean send) {
            return "~CTS" + (send ? 1 : 0) + "#";
        }

        /**
         * 读秒提示音
         */
        public static String secondWarning() {
            return "~AWS#";
        }

        /**
         * 基础时间用完提示音
         */
        public static String baseTimeWarning() {
            return "~AWT#";
        }

        /**
         * 点亮单个灯
         *
         * @param position 1-361
         * @param color    1黑  2白  黑方默认绿灯  白方默认白灯
         */
        public static String lampPosition(int position, int color) {
            if (!inRange(position, 1, 361)) {
                throw new RuntimeException("position must be in 1..361");
            }
            String p = intToString(position);
            switch (color) {
                case 1:
                    return "~SHP" + p + ",r000g255b000,2#";
                case 2:
                    return "~SHP" + p + ",r120g120b210,1#";
                default:
                    throw new RuntimeException("color must be 1 or 2");
            }

        }

        /**
         * 点亮棋盘某个位置指示灯
         *
         * @param position 1-361
         * @param colorR   红色值
         * @param colorG   绿色值
         * @param colorB   蓝色值
         */
        public static String lampPosition(int position, int colorR, int colorG, int colorB) {
            if (!inRange(position, 1, 361)) {
                throw new RuntimeException("position must be in 1..361");
            }
            if (!inRange(colorR, 1, 255) ||
                    !inRange(colorG, 1, 255) ||
                    !inRange(colorB, 1, 255)) {
                throw new RuntimeException("color must be in 1..255");
            }
            String p = intToString(position);
            String r = intToString(colorR);
            String g = intToString(colorG);
            String b = intToString(colorB);
            return "~SHP" + p + ",r" + r + "g" + g + "b" + b + ",1#";
        }

        /**
         * 同时亮多个指示灯
         *
         * @param boardSize     棋盘路数
         * @param indexAndColor int数组，【位置索引（1-361），该位置的值（color: 有1-9个预定义颜色）】
         * @param lightType     亮灯的亮度。1表示低亮，2表示中亮，3表示高亮。只有在亮灯个数小于50的情况下有效
         */
        public static String lampMultiple(int boardSize, List<int[]> indexAndColor, int lightType) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= 361; i++) {
                int row = (i - 1) / 19 + 1;
                int col = (i - 1) % 19 + 1;
                if (boardSize == 13) {
                    if (((row == 3 || row == 17) && col >= 3 && col <= 17) ||
                            ((col == 3 || col == 17) && row >= 3 && row <= 17)) {
                        sb.append("1");//边界灯
                    } else {
                        sb.append("0");
                    }
                } else if (boardSize == 9) {
                    if (((row == 5 || row == 15) && col >= 5 && col <= 15) ||
                            ((col == 5 || col == 15) && row >= 5 && row <= 15)) {
                        sb.append("1");//边界灯
                    } else {
                        sb.append("0");
                    }
                } else {
                    sb.append("0");
                }
            }

            if (indexAndColor != null) {
                for (int[] item : indexAndColor) {
                    int index = item[0];
                    int color = item[1];
                    if (boardSize == 13) {
                        int row = (index - 1) / 13 + 1;
                        int col = (index - 1) % 13 + 1;
                        //转换成19路对应位置
                        index = ((row + 3) - 1) * 19 + (col + 3) - 1;
                    } else if (boardSize == 9) {
                        int row = (index - 1) / 9 + 1;
                        int col = (index - 1) % 9 + 1;
                        //转换成19路对应位置
                        index = ((row + 5) - 1) * 19 + (col + 5) - 1;
                    } else {
                        index = index - 1;
                    }
                    sb.replace(index, index + 1, String.valueOf(color));
                }
            }
            return "~SAR" + sb + "#";
        }

        /**
         * 棋盘显示一个对号（一般做题时使用）
         */
        public static String showRight() {
            return "~RLT#";
        }

        /**
         * 棋盘显示一个叉号（一般做题时使用）
         */
        public static String showError() {
            return "~RLW#";
        }

        /**
         * 棋盘显示一个OK（一般做题时使用）
         */
        public static String showOK() {
            return "~RLO#";
        }

        private static boolean inRange(int source, int min, int max) {
            return source >= min && source <= max;
        }

        private static String intToString(int position) {
            String p;
            if (position < 10)
                p = "00" + position;
            else if (position < 100)
                p = "0" + position;
            else
                p = String.valueOf(position);
            return p;
        }
    }

    /**
     * 底层主动反馈的指令
     */
    public static class Up {
        /**
         * 黑方拍钟
         */
        public static final String clickBlack = "~BKY#";

        /**
         * 白方拍钟
         */
        public static final String clickWhite = "~WKY#";

        /**
         * 黑方拍钟  双击
         */
        public static final String doubleClickBlack = "~BTK#";

        /**
         * 白方拍钟  双击
         */
        public static final String doubleClickWhite = "~WTK#";
    }
}
