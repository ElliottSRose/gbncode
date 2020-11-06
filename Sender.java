
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Sender {

    private static int windowSize = 4;
    private static int prevSeqNum = 0;
    private static int nextSeqNum = 0;
    private static int totalPackets = 0;
    private static int seqNumAck = 0;
    private static long start = 0;
    private static long end = 0;
    private static int max = 4096;
    private static int userNum;
    private static int testNum;
    private static int packetLoss;

    public static void main(String args[]) throws IOException {

        byte[] totalBytes = Files.readAllBytes(Paths.get("../test.txt")); //convert entire file to bytes
        totalPackets = totalBytes.length / max; //total # of packets in file
        //ArrayList<Packet> packetList = new ArrayList<>(); //new list of all packets
        
        //user inputs number 0-99
        Scanner reader = new Scanner(System.in);

        do {
            System.out.println("Please enter a number from 0-99:");
            userNum = Integer.parseInt(reader.nextLine());

            if (userNum < 0 || userNum > 99) {
                System.out.println("Invalid. Please enter a number from 0-99: \n");
                testNum = -1;   //wrong input = loop again
            } else if (userNum >= 0 && userNum <= 99) {
                break;
            }
        } while (userNum >= 0 && userNum <= 99 || testNum == -1);

        while (true) {
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getLocalHost();

            DatagramPacket pkt;
            File file = new File("test.txt");
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            int bytesRead = 0;

            while (nextSeqNum < prevSeqNum + windowSize) { //cannot exceed window size
                start = System.nanoTime(); //start the timer

                byte[] data = new byte[max];
                int startIndex = max * prevSeqNum;
                int endIndex = max * prevSeqNum + max;
                data = Arrays.copyOfRange(totalBytes, startIndex, endIndex); //update bytes for the current packet

                ByteBuffer buf = ByteBuffer.wrap(data);

                buf.clear();
                bytesRead = bis.read(data);

                if (bytesRead == -1) {
                    break;
                }

                int randomNum = new Random(System.currentTimeMillis()).nextInt(); //pseudonumber generated using random seed set to current system time
                if (randomNum < userNum) { //if random number generated is less than user input, then simulate packet loss
                    ++packetLoss; //keeps count of total packet losses
                } else {
                    buf.rewind();
                    pkt = new DatagramPacket(data, max, ip, 8888);
                    ds.send(pkt);
                    ++prevSeqNum;
                }

                byte[] ackBytes = new byte[200]; //arbitrary number for ACK bytes
                DatagramPacket ack = new DatagramPacket(); //create new Datagram packet for ACK -- need to fill in parameters

                
                //if it takes too long, try to resend.
                try {
                    ds.setSoTimeout(60); //arbitrary timeout number
                    ds.receive(ack);
                    
                    //need code for: if ACK is received within the timeframe, ACK is received successfully.
                    //++seqNumAck; 
                    
                } catch (SocketTimeoutException e) {
                    ++packetLoss;

                    for (int i = seqNumAck; i < prevSeqNum; ++i) { //resend from the seqNumAck up to the previous sequence number
                        startIndex = max * i;
                        endIndex = max * i + max;
                        data = Arrays.copyOfRange(totalBytes, startIndex, endIndex); 

                        buf = ByteBuffer.wrap(data);

                        buf.clear();
                        bytesRead = bis.read(data);
                    }

                }
            }

            end = System.nanoTime(); //end the timer

            bis.close();
            fis.close();
      
        } 
        
        System.out.println("Elapsed time: " + (end - start));
        System.out.println("Packets sent: " + prevSeqNum);
        System.out.println("Lost packets: " + packetLoss);
    }

}
