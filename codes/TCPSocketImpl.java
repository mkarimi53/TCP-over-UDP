import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import javax.naming.TimeLimitExceededException;

import com.sun.source.tree.ExportsTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TCPSocketImpl extends TCPSocket {
    InetAddress ip;
    int port;
    EnhancedDatagramSocket edSocket;
    int seq; 
    int timeout;
    byte[] fileContent;
    ArrayList<byte[]> receiveBuffer=new ArrayList<byte[]>();
    int windowSize;
    int SSThreshold;
    int dupACKcount;
    int base;
    int MSS;
    int payload;
    int lastWindowEnd;
    Random rand=new Random();
    enum TCPNewRenoState{ SLOWSTART,CONGESTOINAVOIDANCE,FASTRECOVERY}

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.seq=rand.nextInt(100)+100;
        this.port = port;
        this.ip = InetAddress.getByName(ip);
        this.edSocket = new EnhancedDatagramSocket(0);
        this.windowSize=1;
        this.SSThreshold=8;
        this.dupACKcount=0;
        this.timeout=1000;
        this.base=0;
        this.MSS=948;
        this.lastWindowEnd=0;
        this.payload=1148;
    }

    public void readFile(String pathToFile){
        File file=new File(pathToFile);
        FileInputStream fileIS=null;
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
    }
    public void GBNsend(){
        System.out.println("GBNSend");
        for (int i=lastWindowEnd;i<base+windowSize;i++){
            System.out.println(base);
            System.out.println(lastWindowEnd);

            int end=(i+1)*MSS;
            if(end>fileContent.length)end=fileContent.length+1;
            DatagramPacket sendPacketSYN = new TCPParser(Arrays.copyOfRange(fileContent,(i)*MSS,end), MSS, this.ip, this.port,this.seq , 0).datagramPacket;
            System.out.println(new String(sendPacketSYN.getData()));
            System.out.println(this.ip);
            System.out.println(this.port);

        
            try{
                edSocket.send(sendPacketSYN);
            }catch(Exception ex){
                System.out.println(ex.toString());
            }
            
        }
        lastWindowEnd=base+windowSize;
    }
    public void retransmit(int ack){
        DatagramPacket retransmitPacket = new TCPParser(Arrays.copyOfRange(fileContent,ack*MSS,(ack+1)*MSS),
        MSS, this.ip, this.port,this.seq , 0).datagramPacket;
        try{
            edSocket.send(retransmitPacket);
        }catch(Exception ex){
            System.out.println(ex.toString());
        }
        
    }
    public void tcpNewReno(){
        TCPNewRenoState state=TCPNewRenoState.SLOWSTART;
        while(true){
            switch(state){
                case SLOWSTART:
                    state=slowStart();
                    break;
                // case CONGESTOINAVOIDANCE:
                //     state=congestionAvoidance();
                //     break;
                // case FASTRECOVERY:
                //     state=fastRecovery();
                //     break;
                default:
                    state=slowStart();
                    System.out.println("wtf");

            }
            state=slowStart();
            if(state!=TCPNewRenoState.SLOWSTART)
                break;
        }

    }
    
    public TCPNewRenoState slowStart(){
        GBNsend();
        byte ACKpacket[]=new byte[200];
        DatagramPacket recieveACK=new DatagramPacket(ACKpacket,ACKpacket.length);
        try{
            edSocket.setSoTimeout(timeout);
            edSocket.receive(recieveACK);
        }
        catch(SocketException ex){
            SSThreshold=windowSize/2;
            windowSize=1;
            dupACKcount=0;
            return TCPNewRenoState.SLOWSTART ;
        }
        catch(IOException ex){
            System.out.println("slow start i/o exception :"+ ex.toString());
            return TCPNewRenoState.SLOWSTART;
        }
        TCPParser parseRecieveACK=new TCPParser(recieveACK);
        int ack=parseRecieveACK.getAck();
        if(ack ==(this.seq)){
            base++;
            windowSize++;
            dupACKcount=0;
            return (windowSize<SSThreshold)?TCPNewRenoState.SLOWSTART:TCPNewRenoState.CONGESTOINAVOIDANCE;
        }else{
            dupACKcount++;
            if(dupACKcount==3){
                SSThreshold=windowSize/2;
                windowSize=SSThreshold+3;
                retransmit(ack);
                return TCPNewRenoState.FASTRECOVERY;
            }
            else{
                return TCPNewRenoState.SLOWSTART;
            }
        }
        
    }
    // public TCPNewRenoState congestionAvoidance(){

    // }
    // public TCPNewRenoState fastRecovery(){

    // }

    @Override
    public void send(String pathToFile) throws Exception {
        this.seq=0;
        readFile(pathToFile);
        tcpNewReno();
        // for(int i=0 ;i<Math.ceil(fileContent.length/1480f);i++){
            
        // }

//   throw new RuntimeException("Not implemented!");

    }
    
    @Override
    public void receive(String pathToFile) throws Exception {
        int expectedSeq=0;
        ArrayList<Integer> seqList=new ArrayList<Integer>();
        

        while(true){
            byte[] packet=new byte[payload];
            DatagramPacket packetDatagram=new DatagramPacket(packet,payload);
            System.out.println("receive");
            edSocket.receive(packetDatagram);
            System.out.println(new String(packetDatagram.getData()));
            
            TCPParser packetParse= new TCPParser(packetDatagram);
            int recievedSeq=packetParse.getSeq();
        
            if(recievedSeq==expectedSeq){                
                seqList.add(expectedSeq);
                Collections.sort(seqList);
                for(int i=expectedSeq+1;i<seqList.size();i++){
                    if(seqList.get(i)==expectedSeq+1)expectedSeq=seqList.get(i);
                    else break;
                }
                expectedSeq++;
                DatagramPacket sendACKPacket = new TCPParser(new byte[0], 0, this.ip, this.port,expectedSeq+1, expectedSeq).datagramPacket;
                edSocket.send(sendACKPacket);
                receiveBuffer.add(packet);
                    
            }else{
                DatagramPacket sendACKPacket = new TCPParser(new byte[0], 0, this.ip, this.port,expectedSeq+1, expectedSeq).datagramPacket;
                edSocket.send(sendACKPacket);
                seqList.add(recievedSeq);
                receiveBuffer.add(packet);
            }
        }
    }

    @Override
    public void connect() throws Exception{
        byte bufSendPacketSYN[]=new byte[0];
        DatagramPacket sendPacketSYN = new TCPParser(bufSendPacketSYN, 0, this.ip, this.port,this.seq , 0).datagramPacket;
        edSocket.send(sendPacketSYN);
        byte bufRecievepacketSYNAC[]=new byte[payload];
        DatagramPacket recievepacketSYNAC=new DatagramPacket(bufRecievepacketSYNAC,bufRecievepacketSYNAC.length);
        edSocket.receive(recievepacketSYNAC);
        TCPParser SYNACparse=new TCPParser(recievepacketSYNAC);
        while(SYNACparse.getAck()!=this.seq+1){
            bufRecievepacketSYNAC=new byte[payload];
            recievepacketSYNAC=new DatagramPacket(bufRecievepacketSYNAC,bufRecievepacketSYNAC.length);
            edSocket.receive(recievepacketSYNAC);
            SYNACparse=new TCPParser(recievepacketSYNAC);
                
        }
       
        int ack=SYNACparse.getSeq();
        this.seq=SYNACparse.getAck();
        byte bufSendPacketAC[]=new byte[0];
        ///checkport
        
        DatagramPacket sendPacketAC = new TCPParser(bufSendPacketAC, 0, this.ip
        , this.port,this.seq ,ack+1).datagramPacket;
        edSocket.send(sendPacketAC);

        this.ip = recievepacketSYNAC.getAddress();
        this.port = recievepacketSYNAC.getPort();
        System.out.println(this.ip);    
        System.out.println(this.port);
        this.seq=0;
    }

    public void sendSYN_ACK(DatagramPacket dp){
        System.out.println("sendSYN_ACK");
        try{
            edSocket.send(dp);
        }
        catch(IOException e){
            System.out.println("IOException in sendSYN_ACK");
        }
    }


    @Override
    public void close() throws Exception {
        //throw new RuntimeException("Not implemented!");
        this.edSocket.close();
    }

    @Override
    public long getSSThreshold() {
        return SSThreshold*MSS;
    }

    @Override
    public long getWindowSize() {
        return windowSize*MSS;
    }
}
