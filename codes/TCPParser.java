import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class TCPParser {

    int seq;
    int ack;
    byte[] data;
    public DatagramPacket datagramPacket;

    TCPParser(byte buf[], int length, InetAddress address, int destPort, int seq, int ack) {
        this.seq = seq;
        this.ack = ack;
        this.data = buf;

        byte[] seqByte = convertToByte(seq);
        byte[] ackByte = convertToByte(ack);
        byte[] dpBuff = new byte[seqByte.length + ackByte.length + length];

        System.arraycopy(seqByte, 0, dpBuff, 0, seqByte.length);
        System.arraycopy(ackByte, 0, dpBuff, seqByte.length, ackByte.length);
        System.arraycopy(buf, 0, dpBuff, seqByte.length + ackByte.length, buf.length);

        datagramPacket = new DatagramPacket(dpBuff, dpBuff.length, address, destPort);
    }

    TCPParser(DatagramPacket dp){
        byte[] dataBuf = dp.getData();
        byte[] seqByte = new byte[4];
        byte[] ackByte = new byte[4];
        byte[] dataByte = new byte[dataBuf.length - 8];

        System.arraycopy(dataBuf, 0, seqByte, 0, 4);
        System.arraycopy(dataBuf, 4, ackByte, 0, 4);
        System.arraycopy(dataBuf, 8, dataByte, 0, dataBuf.length - 8);

        this.seq = convertFromByte(seqByte);
        this.ack = convertFromByte(ackByte);
        this.data= dataByte;
    }

    public byte[] convertToByte(int x){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(x);
        return b.array();
    }

    public int convertFromByte(byte[] b){
        ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN);
        bb.position(0);
        return bb.getInt();
    }

    public int getSeq(){
        return seq;
    }

    public int getAck(){
        return ack;
    }

    public byte[] getData(){
        return data;
    }
}