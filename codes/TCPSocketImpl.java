import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.*;
import javax.naming.TimeLimitExceededException;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class TCPSocketImpl extends TCPSocket {
    InetAddress ip;
    int port;
    EnhancedDatagramSocket edSocket;
    int seq; 
    int timeout;
    boolean endPacket;
    byte[] fileContent;
    ArrayList<byte[]> receiveBuffer=new ArrayList<byte[]>();
    int windowSize;
    int SSThreshold;
    int dupACKcount;
    int base;
    int numberOfPackets;
    int MSS;
    int payload;
    int lastWindowEnd;
    Random rand=new Random();
    enum TCPNewRenoState{ SLOWSTART,CONGESTOINAVOIDANCE,FASTRECOVERY,ENDPACKET}

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.seq=rand.nextInt(100)+100;
        this.port = port;
        this.endPacket=false;
        this.ip = InetAddress.getByName(ip);
        this.edSocket = new EnhancedDatagramSocket(0);
        this.windowSize=1;
        this.SSThreshold=8;
        this.dupACKcount=0;
        this.timeout=1000;
        this.base=0;
        this.MSS=1280;
        this.lastWindowEnd=0;
        this.payload=1480;
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
        // if(this.seq>fileContent.length/this.MSS-10)return;
        System.out.println("lastWindowEnd "+lastWindowEnd+"this.seq"+this.seq+"base" +base+" windowSize "+windowSize);
        for (int i=this.seq;i< (base+windowSize );i++){
            if(i*MSS>fileContent.length-1){
                break;
            }
            int end=(i+1)*MSS;
            int _seq=i;
            if(_seq>numberOfPackets)_seq=numberOfPackets;
            //if(end>fileContent.length)end=fileContent.length-1;
            if(end>fileContent.length)end=fileContent.length-1;
            System.out.println("sentpacket"+i);
            DatagramPacket sendPacketSYN = new TCPParser(Arrays.copyOfRange(fileContent,(_seq)*MSS,end), MSS, this.ip, this.port,_seq , 0).datagramPacket;
         
            try{
                edSocket.send(sendPacketSYN);
            }catch(Exception ex){
                System.out.println(ex.toString());
            }
            
        }
        lastWindowEnd=base+windowSize;
    }
    public void retransmit(int ack){
        if(ack*MSS>fileContent.length-1){
            return;
        }
        int end=(ack+1)*MSS;
        if(end>fileContent.length)end=fileContent.length-1;
    
        System.out.println("retransmit "+ack);       
        DatagramPacket retransmitPacket = new TCPParser(Arrays.copyOfRange(fileContent,ack*MSS,end),
        MSS, this.ip, this.port,ack, ack).datagramPacket;
        try{
            edSocket.send(retransmitPacket);
        }catch(Exception ex){
            System.out.println(ex.toString());
        }
    }
    
    
    public TCPNewRenoState slowStart(){
        GBNsend();
        byte ACKpacket[]=new byte[200];
        DatagramPacket recieveACK=new DatagramPacket(ACKpacket,ACKpacket.length);
        try{
            edSocket.setSoTimeout(timeout);
            System.out.println("waiting for ack");
            edSocket.receive(recieveACK);
            // System.out.println(new String(recieveACK.getData()));
        }
        catch(SocketException ex){
            System.out.println("Timeout");
            SSThreshold=windowSize/2;
            windowSize=1;
            dupACKcount=0;
            System.out.println("slow start socket exception :"+ ex.toString());
            
            return TCPNewRenoState.SLOWSTART ;
        }
        catch(IOException ex){
            System.out.println("slow start i/o exception :"+ ex.toString());
            SSThreshold=windowSize/2;
            windowSize=1;
            dupACKcount=0;
            retransmit(this.seq);
            System.out.println("slow start socket exception :"+ ex.toString());
     
            return TCPNewRenoState.SLOWSTART;
        }
        TCPParser parseRecieveACK=new TCPParser(recieveACK);
        int ack=parseRecieveACK.getAck();
        if(ack>=numberOfPackets)return TCPNewRenoState.ENDPACKET;
        System.out.println("Sender: ack = " + ack + ", seq = " + this.seq);
        if(ack >=(this.seq)){
            System.out.println("Sender: ack = this.seq");
            base+=ack-seq+1;
            windowSize++;
            dupACKcount=0;
            this.seq = ack + 1;
            System.out.println("************************threshold reached***********************");
            return (windowSize < SSThreshold) ? TCPNewRenoState.SLOWSTART : TCPNewRenoState.CONGESTOINAVOIDANCE;
        }else{
            System.out.println("Sender: dupAck");
            
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

    public TCPNewRenoState congestionAvoidance(){
        
        float partialWindowSize = 0;

        byte ACKDataBuffer[] = new byte[200];
        DatagramPacket ACKDatagramPacket = new DatagramPacket(ACKDataBuffer, ACKDataBuffer.length);

        try{
            edSocket.setSoTimeout(timeout);
            edSocket.receive(ACKDatagramPacket);

            TCPParser parsedACKDatagramPacket = new TCPParser(ACKDatagramPacket);
            int ack = parsedACKDatagramPacket.getAck();
            if(ack>=numberOfPackets)return TCPNewRenoState.ENDPACKET;
            System.out.println("CongestionAvoidance: Ack received. ackNum = " + ack + ", seqNum = " + this.seq);

            if(ack >= this.seq){
                System.out.println("CongestionAvoidance: newAck");

                base+=ack-seq+1;

                partialWindowSize += (1/(float)windowSize);
                if(partialWindowSize > 1){
                    windowSize++;
                    partialWindowSize = 0;
                }

                dupACKcount = 0;
                this.seq = ack + 1;

                GBNsend();

                return TCPNewRenoState.CONGESTOINAVOIDANCE;
            } 
            else {
                System.out.println("CongestionAvoidance: dupAck");

                dupACKcount++;


                if(dupACKcount == 3){
                    SSThreshold = windowSize/2;
                    windowSize=SSThreshold+3;
                    retransmit(this.seq);
                    return TCPNewRenoState.FASTRECOVERY;
                }
                else {
                    return TCPNewRenoState.CONGESTOINAVOIDANCE;
                }
            }
        }
        catch(SocketException ex){
            System.out.println("CongestionAvoidance: Timeout");
            SSThreshold=windowSize/2;
            windowSize=1;
            dupACKcount=0;
            retransmit(this.seq);
            return TCPNewRenoState.SLOWSTART ;
        }
        catch(IOException ex){
            System.out.println("CongestionAvoidance: i/o exception occured. "+ ex.toString());
            SSThreshold=windowSize/2;
            windowSize=1;

            dupACKcount=0;

            retransmit(this.seq);
            System.out.println("slow start socket exception :"+ ex.toString());
     
            return TCPNewRenoState.SLOWSTART;
        }
    }

    public TCPNewRenoState fastRecovery(){
        System.out.println("-------------------------------------------fastrecovery");
        byte ACKDataBuffer[] = new byte[200];
     //   int mostRecentAck=this.seq-1;//check it
        DatagramPacket ACKDatagramPacket = new DatagramPacket(ACKDataBuffer, ACKDataBuffer.length);
        try{
            edSocket.setSoTimeout(timeout);
            System.out.println("waiting for recieve fast recovery");
            edSocket.receive(ACKDatagramPacket);
            TCPParser parsedACKDatagramPacket = new TCPParser(ACKDatagramPacket);
            int ack = parsedACKDatagramPacket.getAck();

            System.out.println("fast recovery: Ack received. ackNum = " + ack + ", seqNum = " + this.seq);
            if(ack>=numberOfPackets)return TCPNewRenoState.ENDPACKET;
            if(ack >= this.seq){
                System.out.println("return from fast recovery ack:"+this.seq);
                base=base+ack-seq+1;
                dupACKcount=0;
                this.windowSize=this.SSThreshold;
                this.seq = ack + 1;
                
                GBNsend();
                return TCPNewRenoState.CONGESTOINAVOIDANCE;
            } 
            else{
                this.windowSize++;
         //       GBNsend();
                return TCPNewRenoState.FASTRECOVERY;
            }
        }catch(SocketException ex){
            System.out.println("FastRecovery: Timeout");

            SSThreshold=windowSize/2;
            windowSize=1;
            dupACKcount=0;
            retransmit(this.seq);
            if(this.endPacket)return TCPNewRenoState.ENDPACKET;
            return TCPNewRenoState.SLOWSTART ;
        }catch(IOException ex){
            System.out.println("FastRecovery: i/o exception occured. "+ ex.toString());
            SSThreshold=windowSize/2;
            windowSize=1;
            dupACKcount=0;
            retransmit(this.seq);

            System.out.println("slow start socket exception :"+ ex.toString());
     
            return TCPNewRenoState.SLOWSTART ;

        }
    }
    public void sendEndMassage(){
        byte[] message=new String("").getBytes();
        DatagramPacket sendEndPacket = new TCPParser(message, message.length, this.ip, this.port,-1, -1).datagramPacket;
        try{
            edSocket.setDelayInMilliseconds(0);
            edSocket.setLossRate(0f);
            System.out.println("sending End packet");
            edSocket.send(sendEndPacket);
        }catch(SocketException ex){}
        catch(IOException ex){}
        
    }
    public void tcpNewReno(){
        boolean endSending=false;
        TCPNewRenoState state=TCPNewRenoState.SLOWSTART;
        while(!endSending){
            switch(state){
                case SLOWSTART:
                    state=slowStart();
                    break;
                case CONGESTOINAVOIDANCE:
                    state=congestionAvoidance();
                    break;
                case FASTRECOVERY:
                    state=fastRecovery();
                    break;
                default:
                    sendEndMassage();
                    endSending=true;
           }
        }
    }

    @Override
    public void send(String pathToFile) throws Exception {
        this.seq=0;

        readFile(pathToFile);
        numberOfPackets=(int)(Math.ceil(((float)fileContent.length)/MSS))-1;
        System.out.println("numberOfPackets"+numberOfPackets);
        System.out.println("file size"+fileContent.length/MSS);
        tcpNewReno();
    }
    
    @Override
    public void receive(String pathToFile) throws Exception {
        int expectedSeq=0;
        ArrayList<Boolean> seqList=new ArrayList<Boolean>();
        while(true){
            byte[] packet = new byte[payload];
            DatagramPacket packetDatagram = new DatagramPacket(packet,payload);
            System.out.println("waiting for recieve: " + expectedSeq);
            edSocket.receive(packetDatagram);
            
            TCPParser packetParse = new TCPParser(packetDatagram);

            if(packetParse.getAck()==-1){
                break;
            }
            int recievedSeq = packetParse.getSeq();
            if(recievedSeq>=seqList.size()){
                for(int i=0;i<(recievedSeq-seqList.size());i++){
                    seqList.add(false);
                }
                seqList.add(true);
            }
            if(recievedSeq == expectedSeq){    

                System.out.println("IF recieved" + recievedSeq + " this is expected " + expectedSeq);  
                seqList.set(expectedSeq,true);
                if(receiveBuffer.size()>expectedSeq){
                    System.out.println("write packet1 "+ expectedSeq);
                    System.out.println(new String(packetParse.getData()));
                    receiveBuffer.set(expectedSeq,packetParse.getData());
                    
                }
                else {
                    System.out.println("write packet2 "+ expectedSeq);
                    System.out.println(new String(packetParse.getData()));
                    receiveBuffer.add(packetParse.getData());
                };
                int temp=expectedSeq+1;    
                for(int i=temp;i<seqList.size();i++){
                    if(seqList.get(i))expectedSeq++;
                    else break;
                }
                DatagramPacket sendACKPacket = new TCPParser(new byte[0], 0, this.ip, this.port,expectedSeq, expectedSeq).datagramPacket;
                System.out.println("IF recieved" + recievedSeq + " this is expected " + expectedSeq);  
                
                edSocket.send(sendACKPacket);

                expectedSeq++;

                    
            }else {               
                System.out.println("else"+"recieved"+recievedSeq+" this is expected"+expectedSeq);
                DatagramPacket sendACKPacket = new TCPParser(new byte[0], 0, this.ip, this.port,expectedSeq, expectedSeq).datagramPacket;
                
                System.out.println("send dup ack");

                edSocket.send(sendACKPacket);
                if(recievedSeq>expectedSeq){
                    byte[] tempByte=new String("a").getBytes();
                    for(int i=0;i<=recievedSeq-receiveBuffer.size();i++){
                        receiveBuffer.add(tempByte);
                    }
                    if(recievedSeq<receiveBuffer.size()-1){
                        System.out.println("write packet3 "+ recievedSeq);
                        System.out.println(new String(packetParse.getData()));             
                        receiveBuffer.set(recievedSeq,packetParse.getData());
                    }
                    else{
                        receiveBuffer.add(packetParse.getData());    
                        System.out.println("write packet4 "+ recievedSeq);
                        System.out.println(new String(packetParse.getData()));
                    }
                        
                }
            }
        }
        File yourFile = new File(pathToFile);
        yourFile.createNewFile(); // if file a
        FileOutputStream op=new FileOutputStream(pathToFile);
        System.out.println(receiveBuffer.size());
        
        for(int i=0;i<receiveBuffer.size();i++){
            System.out.println(new String(receiveBuffer.get(i)));
            op.write(receiveBuffer.get(i));
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
        this.seq=0;
    }

    public void sendSYN_ACK(DatagramPacket dp){
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
