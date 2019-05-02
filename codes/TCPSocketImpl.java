import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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
        this.ip = InetAddress.getByName(ip);
        this.edSocket = new EnhancedDatagramSocket(0);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        File file=new File("../file.txt");
        FileInputStream fileIS=null;
        byte fileContent[]=null;
        try{
            fileIS=new FileInputStream(file);
            fileContent= new byte[(int)file.length()];
            fileIS.read(fileContent);
        }
        catch(FileNotFoundException ex){
            System.out.println("File not existed: "+ex);
        }
        catch(IOException ex){
            System.out.println("Exception while reading file: "+ex);
        }
        finally{
            try{
                if(fileIS!=null){
                    fileIS.close();
                }
            }
            catch(IOException ex){
                System.out.println("Exception while closing file: "+ ex);
            }
        }
        for(int i=0 ;i<Math.ceil(fileContent.length/1480f);i++){
            
        }

//   throw new RuntimeException("Not implemented!");

    }
    
    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }
    @Override
    public void connect() throws Exception{
        byte bufSendPacketSYN[]=new byte[0];
        DatagramPacket sendPacketSYN = new TCPParser(bufSendPacketSYN, 0, this.ip, this.port,this.seq , 0).datagramPacket;
        edSocket.send(sendPacketSYN);
        byte bufRecievepacketSYNAC[]=new byte[1148];
        DatagramPacket recievepacketSYNAC=new DatagramPacket(bufRecievepacketSYNAC,bufRecievepacketSYNAC.length);
        edSocket.receive(recievepacketSYNAC);
        TCPParser SYNACparse=new TCPParser(recievepacketSYNAC);
        while(SYNACparse.getAck()!=this.seq+1){
            bufRecievepacketSYNAC=new byte[1148];
            recievepacketSYNAC=new DatagramPacket(bufRecievepacketSYNAC,bufRecievepacketSYNAC.length);
            edSocket.receive(recievepacketSYNAC);
            SYNACparse=new TCPParser(recievepacketSYNAC);
                
        }
       
        int ack=SYNACparse.getSeq();
        this.seq=SYNACparse.getAck();
        byte bufSendPacketAC[]=new byte[0];
        DatagramPacket sendPacketAC = new TCPParser(bufSendPacketAC, 0, this.ip, this.port,this.seq ,ack+1).datagramPacket;
        edSocket.send(sendPacketAC);
    }


    @Override
    public void close() throws Exception {
        //throw new RuntimeException("Not implemented!");
        this.edSocket.close();
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
