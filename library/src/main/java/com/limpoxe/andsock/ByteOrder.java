package com.limpoxe.andsock;

public class ByteOrder {

    public static int byte4ToIntB(byte[] b){
        int i = (b[0] & 0xFF) << 24;
        i |= (b[1] & 0xFF) << 16;
        i |= (b[2] & 0xFF) << 8;
        i |= b[3] & 0xFF;
        return i;
    }

    public static byte[] intToByte4B(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) ((i >> 24) & 0xFF);
        b[1] = (byte) ((i >> 16) & 0xFF);
        b[2] = (byte) ((i >> 8) & 0xFF);
        b[3] = (byte) (i & 0xFF);
        return b;
    }

    public static int byte4ToIntL(byte[] b){
        int i = b[0] & 0xFF;
        i |= (b[1] & 0xFF) << 8;
        i |= (b[2] & 0xFF) << 16;
        i |= (b[3] & 0xFF) << 24;
        return i;
    }

    public static byte[] intToByte4L(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0xFF);
        b[1] = (byte) ((i >> 8) & 0xFF);
        b[2] = (byte) ((i >> 16) & 0xFF);
        b[3] = (byte) ((i >> 24) & 0xFF);
        return b;
    }

    public static short byte2ToShortB(byte[] b) {
        short i = (short)((b[0] & 0xff) << 8);
        i |= (b[1] & 0xff);
        return i;
    }

    public static byte[] shortToByte2B(short i) {
        byte[] b = new byte[2];
        b[0] = (byte) ((i >> 8) & 0xFF);
        b[1] = (byte) (i & 0xFF);
        return b;
    }

    public static byte[] ipv4ToByte4(String ipv4) {
        String[] ipArr = ipv4.split("\\.");
        byte[] ret = new byte[4];
        ret[0] = (byte) (Integer.parseInt(ipArr[0]) & 0xFF);
        ret[1] = (byte) (Integer.parseInt(ipArr[1]) & 0xFF);
        ret[2] = (byte) (Integer.parseInt(ipArr[2]) & 0xFF);
        ret[3] = (byte) (Integer.parseInt(ipArr[3]) & 0xFF);
        return ret;
    }

    public static String byte4ToIpv4(byte[] b) {
       return new StringBuilder()
                .append(b[0] & 0xFF).append('.')
                .append(b[1] & 0xFF).append('.')
                .append(b[2] & 0xFF).append('.')
                .append(b[3] & 0xFF).toString();
    }
}
