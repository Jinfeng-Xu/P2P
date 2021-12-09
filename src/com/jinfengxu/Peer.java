package com.jinfengxu;

import com.jinfengxu.utils.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Peer extends JFrame {
    private String src = System.getProperty("user.dir") + File.separatorChar + "src" + File.separatorChar;
    private JTextArea ReceivedMessage;
    private JTextArea GetRes;
    private JTextArea Connect;
    private JTextArea DHRT;
    private String ip;
    private String getRes;
    private String connectAddress;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader buffer_reader;
    private boolean isclosed;
    private ChordDHT dhrt;
    private ObjectInputStream oin;
    private int number;
    private InputStream is;
    private String resourceName;
    private ServerSocket serverSocket;
    private String resGuid;
    private int resIdentifier;
    private int resNumber;

    public static void main(String[] args) {
        Peer peer = new Peer();
    }

    public Peer() {
        // get the ip
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress().toString();
        } catch (UnknownHostException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // UI
        JTextField IPAddress = new JTextField();
        IPAddress.setText(ip);
        IPAddress.setEditable(false);
        JTextField Port = new JTextField();
        Port.setText("8000");
        Port.setEditable(false);
        DHRT = new JTextArea(20, 65);
        DHRT.setEditable(false);
        GetRes = new JTextArea(2, 30);
        Connect = new JTextArea(2,30);
        ReceivedMessage = new JTextArea(30, 35);
        ReceivedMessage.setLineWrap(true);
        ReceivedMessage.setWrapStyleWord(true);
        ReceivedMessage.setEditable(false);
        JButton sendButton = new JButton("GetRes");
        JButton connectButton = new JButton("Connect");
        sendButton.setPreferredSize(new Dimension(100, 50));
        ActionListener send = new send();
        sendButton.addActionListener(send);
        connectButton.setPreferredSize(new Dimension(100, 50));
        ActionListener connect = new connect();
        connectButton.addActionListener(connect);
        JPanel leftpanel = new JPanel();
        JPanel northpanel = new JPanel();
        JPanel centerpanel = new JPanel();
        JPanel southpanel = new JPanel();
        northpanel.setLayout(new GridLayout(2, 2));
        northpanel.add(new JLabel("IP Address"));
        northpanel.add(IPAddress);
        northpanel.add(new JLabel("Server's Port:"));
        northpanel.add(Port);
        centerpanel.add(DHRT);
        southpanel.setLayout(new GridLayout(2,2,5,5));
        southpanel.add(GetRes);
        southpanel.add(sendButton);
        southpanel.add(Connect);
        southpanel.add(connectButton);
        leftpanel.setLayout(new BorderLayout());
        leftpanel.add(northpanel, BorderLayout.NORTH);
        leftpanel.add(centerpanel, BorderLayout.CENTER);
        leftpanel.add(southpanel, BorderLayout.SOUTH);
        JPanel rightpanel = new JPanel();
        rightpanel.add(ReceivedMessage);
        JPanel bottonpanel = new JPanel();
        bottonpanel.setLayout(new FlowLayout());
        bottonpanel.add(leftpanel);
        bottonpanel.add(rightpanel);

        //JFrame
        add(bottonpanel);
        pack();
        WindowListener listener = new Terminator();
        addWindowListener(listener);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);

        // init the first DHRT.
        // The original DHRT is the same as the UHRT when it is connected.
        try {
            // get the dhrt by server
            socket = new Socket(InetAddress.getLocalHost(), 8000);
            is = socket.getInputStream();
            oin = new ObjectInputStream(socket.getInputStream());
            Object readObject = oin.readObject();
            dhrt = (ChordDHT) readObject;

            // write DHRT in txt
            FileUtil.writeDHRT(src + "DHRT.txt", dhrt);
            Node node = dhrt.getLastNode();
            String str = "";
            for (Object o : node.getPeerList()) {
                str += o.toString();
            }
            number = Integer.valueOf(str);
            System.out.println(7000 + number);

            // Enable a serverSocket for accessing resources by other peers.
            serverSocket = new ServerSocket(7000 + number);
            setTitle("Peer" + str);
            resourceName = src + node.getResourseName() + ".txt";

            // Simulate a resource. The resource name and information are random
            FileUtil.initResourse(resourceName);

            // Show the DHRT in the UI.
            writeByFile();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Start a thread and wait for other clients to connect to request resources
        WaitThread wait = new WaitThread();
        Thread waitThread = new Thread(wait);
        waitThread.start();

        // socket to receiving server information
        try {
            InputStreamReader reader = new InputStreamReader(is);
            buffer_reader = new BufferedReader(reader);
            writer = new PrintWriter(socket.getOutputStream());
            // The thread terminates when the window is closed
            isclosed = false;
            int countFlag = 0;
            String str = null;
            while (!isclosed) {
                try {
                    str = buffer_reader.readLine();
                    if (str != null) {
                        // get the Identifier by server in order to update DHRT
                        if(countFlag == 1){
                            countFlag = 0;
                            resIdentifier = Integer.parseInt(str.substring(12, str.length()));
                        }
                        // get the Guid by server in order to update DHRT
                        else if(countFlag == 2){
                            countFlag = 1;
                            resGuid = str.substring(6, str.length());
                        }
                        else {
                            // Receives system notification when resources are requested by other peers
                            // and prepares corresponding resource information
                            if(str.substring(0, 4).equals("Peer")){
                                resourceName = src + str.substring(str.length()-6, str.length()) + ".txt";
                                System.out.println(resourceName);
                            }
                            // reset the flag
                            else if (str.substring(0, 8).equals("Resource"))
                                countFlag = 2;
                            // print server's info to UI
                            ReceivedMessage.append("Server: " + str + "\n");
                            System.out.println("Server say:" + str);
                        }
                    }
                } catch (Exception e) {
                    ReceivedMessage.append("Wrong");
                    System.out.println("Wrong");
                    e.printStackTrace();
                    break;
                }
            }
            // Close the pipeline
            writer.close();
            buffer_reader.close();
            socket.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            ReceivedMessage.append("Wrong");
            System.out.println("Wrong");
            e.printStackTrace();
        }
    }

    // Sends target resource name to the server
    class send implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO Auto-generated method stub
            getRes = GetRes.getText();
            System.out.println(getRes);
            // Determine whether the requested resource is already owned
            if(judgeRes(getRes)){
                ReceivedMessage.append("Already owns the resource\n");
            }
            else{
                sendToServer(getRes);
            }
            GetRes.setText("");
        }
    }

    // Determine whether the requested resource is already owned
    private boolean judgeRes(String getRes) {
        boolean flag = false;
        Node firstNode = dhrt.getLastNode().getSuccessor();
        if(firstNode.getResourseName().equals(getRes)){
            flag = judgePeer(firstNode);
        }
        else{
            while(firstNode!=dhrt.getLastNode()){
                firstNode = firstNode.getSuccessor();
                if(firstNode.getResourseName().equals(getRes)){
                    flag = judgePeer(firstNode);
                }
            }
        }
        return flag;
    }

    // Determine whether the requested resource is already owned
    private boolean judgePeer(Node firstNode) {
        for (Object o : firstNode.getPeerList()) {
            if(o.toString().equals(String.valueOf(number))){
                return true;
            }
        }
        return false;
    }

    // a method to print String to server
    private void sendToServer(String msg){
        writer.println(msg);
        writer.flush();
    }

    // Sends the target peer resource request
    class connect implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO Auto-generated method stub
            // target peer's address
            connectAddress = Connect.getText();
            System.out.println(connectAddress);
            Socket tmpSocket = null;
            DataInputStream dis = null;
            FileOutputStream fos = null;
            String fileName = null;
            Boolean flag = true;
            try {
                // Determine if it is your own resource
                if(Integer.parseInt(connectAddress) == 7000 + number) {
                    ReceivedMessage.append("Don't ask for your own port"+"\n");
                }
                // a socket to target peer
                else{
                    tmpSocket = new Socket(InetAddress.getLocalHost(), Integer.parseInt(connectAddress));
                    while(flag){
                        // receive the aim resource by method FileUtil.receiveFile
                        fileName = FileUtil.receiveFile(src, tmpSocket, dis, fos);
                        // Resource received, change tag
                        if(fileName!=null) flag = false;
                        String msg = getTitle() + " successfully received resource " + fileName.substring(0, fileName.length()-4) + " from " + connectAddress;
                        // Sends successful information to the server
                        sendToServer(msg);
                        // show this information on UI
                        ReceivedMessage.append(msg+"\n");
                        // update DHRT
                        updateDHRT(fileName);
                    }
                }
            } catch (Exception e) {
                ReceivedMessage.append("Wrong connect!\n");
                System.out.println("Wrong connect!");
//                e.printStackTrace();
            } finally {
                try {
                    // close the socket
                    if(tmpSocket!=null)
                        tmpSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // clear the JTextArea
            Connect.setText("");
        }
    }

    // update DHRT
    private void updateDHRT(String fileName) {
        // Check whether the Guid already exists
        if(dhrt.isIn(resGuid)){
            // Update peerList if it already exists
            List peerList = dhrt.getNode(resGuid).getPeerList();
            peerList.add(number);
            Collections.sort(peerList);
            dhrt.getNode(resGuid).setPeerList(peerList);
        }
        else{
            // Add a new node to store the resource's information and GUID
            List peerList = new ArrayList();
            peerList.add(Integer.parseInt(connectAddress) - 7000);
            peerList.add(number);
            Collections.sort(peerList);
            Node dhrtNode = new Node(6, fileName.substring(0, fileName.length()-4), resGuid, peerList, resIdentifier);
            dhrt.joinToRing(dhrt.getLastNode(), dhrtNode, 6);

        }
        try {
            // Update the local DHRT.txt file
            FileUtil.writeDHRT(src + "DHRT.txt", dhrt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Clear the JTextArea
        DHRT.setText("");
        // Reset the DHRT on UI
        writeByFile();
    }

    // When resources are requested by other peers,
    // a new thread is enabled to send resources
    class SocketHandler implements Runnable {
        private Socket socket;

        public SocketHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // send resource by Method FileUtil.sendFile
                FileUtil.sendFile(resourceName, socket);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Terminates the thread when the window closes
    class Terminator extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            isclosed = false;
        }
    }

    // Display the DHRT on the UI
    public void writeByFile(){
        File fileUHPT = new File(src + "DHRT.txt");
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(fileUHPT));
            String str;
            while ((str = in.readLine()) != null)
            {
                DHRT.append(str+'\n');
            }
            in.close();
        }
        catch (IOException e)
        {
            e.getStackTrace();
        }
    }

    // A thread, waiting for other peers to request resources
    class WaitThread implements Runnable{
        public void run(){
            while(!isclosed){
                Socket resSocket = null;
                try {
                    resSocket = serverSocket.accept();
                    // Start resource sending thread
                    SocketHandler handler = new SocketHandler(resSocket);
                    Thread thread = new Thread(handler);
                    thread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
