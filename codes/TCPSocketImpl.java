import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    InetAddress ip;
    int port;
    EnhancedDatagramSocket edSocket;
    int seq; 
    Random rand=new Random();
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.seq=rand.nextInt(100)+100;
        this.port = port;
        this.ip = InetAddress.getLocalHost(); 
        edSocket = new EnhancedDatagramSocket(0);
        byte bufSendPacketSYN[]=new byte[0];
        DatagramPacket sendPacketSYN = new TCPParser(bufSendPacketSYN, 0, this.ip, port,this.seq , 0).datagramPacket;
        edSocket.send(sendPacketSYN);
        byte bufRecievepacketSYNAC[]=new byte[1148];
        DatagramPacket recievepacketSYNAC=new DatagramPacket(bufRecievepacketSYNAC,bufRecievepacketSYNAC.length);
        edSocket.receive(recievepacketSYNAC);
        TCPParser SYNACparse=new TCPParser(recievepacketSYNAC);
        //TODOOO handle receive timer and check ac number 
        // ac=SYNACparse.getAC();
        // seq=SYNACparse.getSEQ();
        // byte bufSendPacketAC[]=new byte[0];
        // DatagramPacket sendPacketAC = new TCPParser(bufSendPacketAC, 0, this.ip, port,seq ,ac+1).datagramPacket;
        // edSocket.send(sendPacketAC);
    }

    @Override
    public void send(String pathToFile) throws Exception {

//   throw new RuntimeException("Not implemented!");

    }

    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}
