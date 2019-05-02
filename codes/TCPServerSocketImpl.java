
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;


public class TCPServerSocketImpl extends TCPServerSocket {

    int port;
    InetAddress ip;
    EnhancedDatagramSocket edSocket;
    int seq; 
    Random rand=new Random();
    

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.seq=rand.nextInt(100)+100;
        this.port = port;
        this.ip = InetAddress.getLocalHost(); 
        edSocket = new EnhancedDatagramSocket(port);
    }

    @Override
    public TCPSocket accept() throws Exception {
        byte recvBuf[] = new byte[1480]; //Not sure
        DatagramPacket DpReceive = new DatagramPacket(recvBuf, recvBuf.length);
        edSocket.receive(DpReceive);
        TCPParser ParseDpReceive=new TCPParser(DpReceive);
        byte sendBuff[] = new byte[0];
        TCPParser AckDpReceive=new TCPParser
        (sendBuff, sendBuff.length, DpReceive.getAddress(), DpReceive.getPort(), this.seq, ParseDpReceive.getSeq()+1);
        edSocket.send(AckDpReceive.datagramPacket);
        byte recvAckBuf[] = new byte[1480]; //Not sure
        DatagramPacket DpAckReceive = new DatagramPacket(recvAckBuf, recvAckBuf.length);
        edSocket.receive(DpAckReceive);
        TCPParser x=new TCPParser(DpAckReceive);
        while(x.getAck()!=this.seq+1){
            recvAckBuf = new byte[1480]; 
            DpAckReceive = new DatagramPacket(recvAckBuf, recvAckBuf.length);
            edSocket.receive(DpAckReceive);    
            x=new TCPParser(DpAckReceive);
        }
        this.seq=x.getAck();
        return new TCPSocketImpl(DpAckReceive.getAddress().getHostAddress(),DpAckReceive.getPort());
    }

    @Override
    public void close() throws Exception {
        this.edSocket.close();
        //throw new RuntimeException("Not implemented!");
    }
}
