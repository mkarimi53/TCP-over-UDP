import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;


public class TCPParser {

    int seq;
    int ack;
    public DatagramPacket datagramPacket;

    TCPParser(byte buf[], int length, InetAddress address, int destPort, int seq, int ack) {
        this.seq = seq;
        this.ack = ack;
        byte[] seqByte = convertToByte(seq);
        byte[] ackByte = convertToByte(ack);
        byte[] dpBuff = new byte[seqByte.length + ackByte.length + length];
        System.arraycopy(seqByte, 0, dpBuff, 0, seqByte.length);
        System.arraycopy(ackByte, 0, dpBuff, seqByte.length, ackByte.length);
        System.arraycopy(buf, 0, dpBuff, seqByte.length + ackByte.length, buf.length);

        datagramPacket = new DatagramPacket(dpBuff, length, address, destPort);
    }

    TCPParser(DatagramPacket dp){
        byte[] data = dp.getData();
        
    }

    public byte[] convertToByte(int x){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(x);
        return b.array();
    }

}