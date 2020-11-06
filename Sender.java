
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
    private static int currentSeqNum;
    private static int nextSeqNum;
    //private static int totalPackets = 0;
    private static int totalPacketsSent = 0;
    private static long start = 0;
    private static long end = 0;
    private static int max = 4096;
    private static int userNum;
    private static int testNum;
    private static int packetLoss;
    private static int startIndex;
    private static int endIndex;
    private static byte[] data;
    private static ByteBuffer buf;

    public static void main(String args[]) throws IOException {
        DatagramSocket ds = new DatagramSocket();
        InetAddress ip = InetAddress.getLocalHost();

        DatagramPacket pkt;
        File file = new File("test.txt");
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        int bytesRead = 0;

        byte[] totalBytes = Files.readAllBytes(Paths.get("test.txt")); //convert entire file to bytes

        //totalPackets = totalBytes.length / max; //total # of packets in file
        //ArrayList<Packet> packetList = new ArrayList<>(); //new list of all packets
        //DatagramPacket ack = new DatagramPacket(); //create new Datagram packet for ACK coming in -- need to fill in parameters
        byte[] ackBytes = new byte[200]; //arbitrary number for ACK bytes

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
            start = System.nanoTime(); //start the timer
            int pseudoNum = new Random(System.currentTimeMillis()).nextInt(); //pseudonumber generated using random seed set to current system time

            currentSeqNum = 0;
            nextSeqNum = 0;

            while (currentSeqNum < windowSize - 1) {  //should not exceed window size
                data = new byte[max];
                startIndex = max * currentSeqNum;
                endIndex = startIndex + max;
                data = Arrays.copyOfRange(totalBytes, startIndex, endIndex); //get bytes for the current packet from totalBytes

                buf = ByteBuffer.wrap(data);

                buf.clear();
                bytesRead = bis.read(data);

                if (bytesRead == -1) {
                    break;
                }

                //if random number generated is less than user input, then simulate packet loss
                if (pseudoNum < userNum) {
                    ++packetLoss; //keep count of total packet losses

                } else {
                    buf.rewind();
                    pkt = new DatagramPacket(data, max, ip, 8888);
                    ds.send(pkt);

                    ++currentSeqNum;
                    nextSeqNum = currentSeqNum + 1;
                    ++totalPacketsSent;
                }

            }

            //if ds.receive(ack)
            //need code for: if ACK is received within a specific timeframe, it is successful
            //else if it takes too long/ACK never comes through,
            //resend from the packet requiring ACK, up to the previous sequence number (nextSeqNum-1)
            for (int i = (nextSeqNum - 1) - windowSize; i < nextSeqNum - 1; ++i) { //i'm attempting to make i = packet trying to get ACK'ed
                startIndex = max * i;
                endIndex = startIndex + max;
                data = Arrays.copyOfRange(totalBytes, startIndex, endIndex);

                buf = ByteBuffer.wrap(data);

                buf.clear();
                bytesRead = bis.read(data);
                buf.rewind();
                pkt = new DatagramPacket(data, max, ip, 8888);
                ds.send(pkt);

                ++currentSeqNum;
                nextSeqNum = currentSeqNum + 1;
            }
        }

        bis.close();
        fis.close();
        end = System.nanoTime(); //end the timer

        System.out.println("Elapsed time: " + (end - start));
        System.out.println("Packets sent: " + currentSeqNum);
        System.out.println("Lost packets: " + packetLoss);
    }

}
