
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
        //this.seq=rand.nextInt(100)+100;
        this.seq=100;
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
        System.out.println(ParseDpReceive.getSeq());
        TCPParser AckDpReceive=new TCPParser
        (sendBuff, sendBuff.length, DpReceive.getAddress(), DpReceive.getPort(), this.seq, ParseDpReceive.getSeq()+1);
        edSocket.send(AckDpReceive.datagramPacket);
        byte recvAckBuf[] = new byte[1480]; //Not sure
        DatagramPacket DpAckReceive = new DatagramPacket(recvAckBuf, recvAckBuf.length);
        edSocket.receive(DpAckReceive);
        TCPParser x=new TCPParser(DpAckReceive);
        System.out.println(x.getAck());
        return new TCPSocketImpl(DpAckReceive.getAddress().getHostAddress(),DpAckReceive.getPort());
       //to Doooo check and port and address similarities

        /*
        TCPSocket tcpSocket = new TCPSocketImpl(DpReceive.getAddress().getHostAddress(), DpReceive.getPort());

        byte sendBuff[] = new byte[1480];
        DatagramPacket DpSend = new DatagramPacket(sendBuff, sendBuff.length, DpReceive.getAddress(), DpReceive.getPort());
*/
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
