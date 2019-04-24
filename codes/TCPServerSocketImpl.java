
import java.net.DatagramPacket;
import java.net.InetAddress;

import Plot.Data;

public class TCPServerSocketImpl extends TCPServerSocket {

    int port;
    InetAddress ip;
    EnhancedDatagramSocket edSocket;

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;
        this.ip = InetAddress.getLocalHost(); 
        edSocket = new EnhancedDatagramSocket(port);
    }

    @Override
    public TCPSocket accept() throws Exception {
        byte recvBuf[] = new byte[1480]; //Not sure
        DatagramPacket DpReceive = new DatagramPacket(recvBuf, recvBuf.length, ip, port);
        edSocket.receive(DpReceive);

        byte sendBuff[] = new byte[1480];
        DatagramPacket DpSend = new DatagramPacket(sendBuff, sendBuff.length, DpReceive.getAddress(), DpReceive.getPort());

    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
