package com.jinfengxu.utils;

import com.jinfengxu.ChordDHT;
import com.jinfengxu.Node;

import java.io.*;
import java.net.Socket;
import java.util.Random;

/**
 * File utility class
 */
public class FileUtil {

    // Check whether a file exists based on the file path
    public static boolean isExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    // Generates a random string of the specified length
    public static String randomFileName(int length) {
        String val = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int choice = random.nextInt(2) % 2 == 0 ? 65 : 97;
            val += (char) (choice + random.nextInt(26));
        }
        return val;
    }

    // Generates a random string of random length to fill the file
    public static String randomSentence() {
        int length = (int) (Math.random() * 18 + 1);
        String val = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int choice = random.nextInt(2) % 2 == 0 ? 65 : 97;
            val += (char) (choice + random.nextInt(26));
        }
        return val;
    }

    // Write the UHPT object information that needs to be displayed in the UI to a file, using overwrite write
    public static void writeUHPT(String fileSrc, ChordDHT uhpt) throws IOException {
        File file = new File(fileSrc);
        if(!file.exists()) file.createNewFile();
        FileWriter fw = new FileWriter(file,false); //Over Write
        fw.write("UHPT\tPeers\tGUID\t\t\t\tRouting Metric\n");
        Node firstNode = uhpt.getLastNode().getSuccessor();
        if(uhpt.getLastNode()!=null){
            fw.write("\t"+firstNode.getPeerNumber()+"\t"+firstNode.getGuid()+"\t"+firstNode.getRoutingMetric()+"\n");
            Node node = firstNode.getSuccessor();
            while(node != firstNode){
                fw.write("\t"+node.getPeerNumber()+"\t"+node.getGuid()+"\t"+node.getRoutingMetric()+"\n");
                node = node.getSuccessor();
            }
        }
        fw.close();
    }

    // Write the UHRT object information that needs to be displayed in the UI to a file, using overwrite write
    public static void writeUHRT(String fileSrc, ChordDHT uhrt) throws IOException {
        File file = new File(fileSrc);
        if(!file.exists()) file.createNewFile();
        FileWriter fw = new FileWriter(file,false); //Over Write
        fw.write("UHRT\tResource\tGUID\t\t\t\tPeers\n");
        Node firstNode = uhrt.getLastNode().getSuccessor();
        if(uhrt.getLastNode()!=null){
            String tmp = "";
            for (Object o : firstNode.getPeerList()) {
                tmp += "P" + o.toString()+" ";
            }
            fw.write("\t"+firstNode.getResourseName()+"\t"+firstNode.getGuid()+"\t"+tmp+"\n");
            Node node = firstNode.getSuccessor();
            while(node != firstNode){
                tmp = "";
                for (Object o : node.getPeerList()) {
                    tmp += "P" + o.toString()+" ";
                }
                fw.write("\t"+node.getResourseName()+"\t"+node.getGuid()+"\t"+tmp+"\n");
                node = node.getSuccessor();
            }
        }
        fw.close();
    }

    // Write the DHRT object information that needs to be displayed in the UI to a file, using overwrite write
    public static void writeDHRT(String fileSrc, ChordDHT dhrt) throws IOException {
        File file = new File(fileSrc);
        if(!file.exists()) file.createNewFile();
        FileWriter fw = new FileWriter(file,false); //Over Write
        fw.write("DHRT\tResource\tGUID\t\t\t\tPeers\n");
        Node firstNode = dhrt.getLastNode().getSuccessor();
        if(dhrt.getLastNode()!=null){
            String tmp = "";
            for (Object o : firstNode.getPeerList()) {
                tmp += "P" + o.toString()+" ";
            }
            fw.write("\t"+firstNode.getResourseName()+"\t"+firstNode.getGuid()+"\t"+tmp+"\n");
            Node node = firstNode.getSuccessor();
            while(node != firstNode){
                tmp = "";
                for (Object o : node.getPeerList()) {
                    tmp += "P" + o.toString()+" ";
                }
                fw.write("\t"+node.getResourseName()+"\t"+node.getGuid()+"\t"+tmp+"\n");
                node = node.getSuccessor();
            }
        }
        fw.close();
    }

    // init resource by random sentence
    public static void initFile(String fileSrc) throws IOException {
        File file = new File(fileSrc);
        FileWriter fw = new FileWriter(file,false); //Over Write
        int line = (int) (Math.random()*20 + 5);
        for (int i = 0; i < line; i++) {
            fw.write(randomSentence()+"\n");
        }
        fw.close();
    }

    // send resource File to other peer
    public static void sendFile(String fileSrc, Socket socket) throws IOException {
        File file = new File(fileSrc);
        FileInputStream fis = null;
        DataOutputStream dos = null;
        try {
            fis = new FileInputStream(file);
            dos = new DataOutputStream(socket.getOutputStream());
            //File name, size, and other properties
            dos.writeUTF(file.getName());
            dos.flush();
            dos.writeLong(file.length());
            dos.flush();
            // Starting
            System.out.println("======== Starting ========");
            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                dos.write(bytes, 0, length);
                dos.flush();
            }
            System.out.println("======== Successful ========");
        }catch(IOException e){
            e.printStackTrace();
            System.out.println("Sending FIle Error");
        }finally{
            // close fis and dos
            fis.close();
            dos.close();
        }
    }

    // receive resource file from other peer
    public static String receiveFile(String dir, Socket socket, DataInputStream dis, FileOutputStream fos) throws IOException {
        String fileName = null;
        try {
            dis = new DataInputStream(socket.getInputStream());
            // File name, size
            fileName = dis.readUTF();
            long fileLength = dis.readLong();
            File directory = new File(dir);
            File file = new File(dir + File.separatorChar + fileName);
            fos = new FileOutputStream(file);
            System.out.println("======== Starting ========");
            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                fos.write(bytes, 0, length);
                fos.flush();
            }
            System.out.println("======== Successful ========");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // close the fos and dis
            if (fos != null)
                fos.close();
            if (dis != null)
                dis.close();
        }
//        System.out.println(fileName);
        return fileName;
    }

    // Initialization automatically simulates a random resource
    public static void initResourse(String resourceName) throws IOException {
        File file = new File(resourceName);
        file.createNewFile();
        FileUtil.initFile(resourceName);
    }

}