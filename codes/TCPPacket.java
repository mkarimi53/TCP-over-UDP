import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;


public class TCPPacket {

    int seq;
    int ack;
    int sourcePort;
    public DatagramPacket datagramPacket;

    TCPPacket(byte buf[], int length, InetAddress address, int destPort, int sourcePort, int seq, int ack) {
        this.seq = seq;
        this.ack = ack;
        this.sourcePort = sourcePort;
        byte[] seqByte = convertToByte(seq);
        byte[] ackByte = convertToByte(ack);
        byte[] sourcePortByte = convertToByte(sourcePort);
        byte[] dpBuff = new byte[seqByte.length + ackByte.length + sourcePortByte.length];
        System.arraycopy(seqByte, 0, dpBuff, 0, seqByte.length);
        System.arraycopy(ackByte, 0, dpBuff, seqByte.length, ackByte.length);
        System.arraycopy(sourcePortByte, 0, dpBuff, seqByte.length + ackByte.length, sourcePortByte.length);  //Not sure
        System.arraycopy(buf, 0, dpBuff, seqByte.length + ackByte.length + sourcePortByte.length, buf.length);

        datagramPacket = new DatagramPacket(dpBuff, length, address, destPort);
    }

    public byte[] convertToByte(int x){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(x);
        return b.array();
    }

}