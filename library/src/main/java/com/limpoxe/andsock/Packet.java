package com.limpoxe.andsock;

public class Packet {
    private static final String TAG = "Packet";

    public static final int TYPE_REQ = 0;
    public static final int TYPE_ACK = 1;

    //必须 包总长度
    final int length;
    //必须 包类型，是请求包还是ACK包
    public final int type;
    //必须 包id 由发包侧随机生成，在发包侧确保唯一性，作为回包依据
    public final int id;
    //非必须 数据
    public final byte[] data;

    public Packet(int type, int id, byte[] data) {
        this.length = 4 + 1 + 4 + (data==null?0:data.length);
        if (Byte.MIN_VALUE <= type && type <= Byte.MAX_VALUE) {
            this.type = type;
        } else {
            throw new IllegalArgumentException("type outOfRange: " + type);
        }
        this.id = id;
        this.data = data;

        if (type != Packet.TYPE_REQ && type != Packet.TYPE_ACK) {
            throw new Error("unknown type: " + type);
        }
    }

    public String toString() {
        return "[" + length + "][" + (type == Packet.TYPE_REQ? "Req":"Ack") + "][" + id + "]<RawData Length " + (data==null?0:data.length) +">";
                //+ (BuildConfig.DEBUG?("," + hex(pack(this))):"");
    }

    private static String hex(byte[] pack) {
        StringBuilder stringBuilder = new StringBuilder();
        if (pack != null) {
            stringBuilder.append("[");
            for (int i = 0; i < pack.length; i++) {
                stringBuilder.append("0x");
                String hex = Integer.toHexString(pack[i] & 0xFF).toUpperCase();
                stringBuilder.append(hex.length()==1?("0" + hex):hex);
                if (i != pack.length - 1) {
                    stringBuilder.append(" ");
                }
            }
            stringBuilder.append("]");
        }
        return stringBuilder.toString();
    }

    public static byte[] pack(Packet packet) {
        //4个字节 包长度
        byte[] pak = new byte[packet.length];
        byte[] len = ByteOrder.intToByte4B(packet.length);
        pak[0] = len[0];
        pak[1] = len[1];
        pak[2] = len[2];
        pak[3] = len[3];

        //1个字节 包类型
        pak[4] = (byte)packet.type;

        //4个字节 包序号
        byte[] idByte = ByteOrder.intToByte4B(packet.id);
        pak[5] = idByte[0];
        pak[6] = idByte[1];
        pak[7] = idByte[2];
        pak[8] = idByte[3];

        //数据
        if (packet.data != null) {
            for (int i = 0; i < packet.data.length; i++) {
                pak[i + 9] = packet.data[i];
            }
        }

        return pak;
    }

    public static Packet unpack(byte[] pak) {
        if (pak.length < 9) {
            LogUtil.log(TAG, "unpack fail, null");
            return null;
        }

        //4个字节 包长度
        byte[] len = new byte[4];
        len[0] = pak[0];
        len[1] = pak[1];
        len[2] = pak[2];
        len[3] = pak[3];
        int packetLen = ByteOrder.byte4ToIntB(len);
        if (packetLen != pak.length) {
            LogUtil.log(TAG, "unpack fail " + packetLen + " " + pak.length);
            return null;
        }

        //1个字节 包类型
        int type = (int)pak[4];
        if (type != Packet.TYPE_REQ && type != Packet.TYPE_ACK) {
            LogUtil.log(TAG, "unpack fail, type=" + type);
            return null;
        }

        //4个字节 包序号
        byte[] idByte = new byte[4];
        idByte[0] = pak[5];
        idByte[1] = pak[6];
        idByte[2] = pak[7];
        idByte[3] = pak[8];
        int id = ByteOrder.byte4ToIntB(idByte);

        //数据
        byte[] data = new byte[pak.length - 9];
        for (int i = 0; i < data.length; i++) {
            data[i] = pak[i + 9];
        }

        return new Packet(type, id, data);
    }

}
