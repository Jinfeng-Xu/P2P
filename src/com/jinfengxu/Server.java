package com.jinfengxu;

import com.jinfengxu.utils.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Server extends JFrame {
    private JTextArea ReceivedMessage;
    private JTextArea UHRT;
    private JTextArea UHPT;
    private String ip;
    private String UHPTSrc = System.getProperty("user.dir") + File.separatorChar + "src" + File.separatorChar + "UHPT.txt";
    private String UHRTSrc = System.getProperty("user.dir") + File.separatorChar + "src" + File.separatorChar + "UHRT.txt";
    private String fromclient, toclient;
    private JList list;
    private DefaultListModel clientItem;
    private ArrayList<Socket> socketlist;
    private int clientNo, index;
    private ChordDHT uhpt = new ChordDHT();
    private ChordDHT uhrt = new ChordDHT();

    public static void main(String[] args) throws Exception {
        Server server = new Server();
    }

    public Server() throws Exception {
        // get the ip
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress().toString();
        } catch (UnknownHostException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        //UI
        JTextField IPAddress = new JTextField();
        IPAddress.setText(ip);
        IPAddress.setEditable(false);
        JTextField Port = new JTextField();
        Port.setText("8000");
        Port.setEditable(false);
        list = new JList();
        list.setPreferredSize(new Dimension(200, 100));
        clientItem = new DefaultListModel();
        UHRT = new JTextArea(10, 65);
        UHPT = new JTextArea(10, 65);
        ReceivedMessage = new JTextArea(30, 35);
        ReceivedMessage.setLineWrap(true);
        ReceivedMessage.setWrapStyleWord(true);
        ReceivedMessage.setEditable(false);
        UHPT.setEditable(false);
        UHRT.setEditable(false);

        JPanel leftpanel = new JPanel();
        JPanel northpanel = new JPanel();
        JPanel centerpanel = new JPanel();
        JPanel southpanel = new JPanel();
        northpanel.setLayout(new GridLayout(2, 2));
        northpanel.add(new JLabel("IP Address"));
        northpanel.add(IPAddress);
        northpanel.add(new JLabel("Port:"));
        northpanel.add(Port);
        centerpanel.setLayout(new BorderLayout(0, 15));
        centerpanel.add(list, BorderLayout.NORTH);
        centerpanel.add(UHPT, BorderLayout.CENTER);
        centerpanel.add(UHRT, BorderLayout.SOUTH);

        leftpanel.setLayout(new BorderLayout(0, 20));
        leftpanel.add(northpanel, BorderLayout.NORTH);
        leftpanel.add(centerpanel, BorderLayout.CENTER);
        leftpanel.add(southpanel, BorderLayout.SOUTH);
        //Connected Client
        JPanel rightpanel = new JPanel();
        rightpanel.add(ReceivedMessage);
        //Bottom Part
        JPanel bottonpanel = new JPanel();
        bottonpanel.setLayout(new FlowLayout());
        bottonpanel.add(leftpanel);
        bottonpanel.add(rightpanel);
        //JFrame
        add(bottonpanel);
        pack();
        //setSize(400,500);
        setTitle("Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);
        clientNo = 0;
        index = 0;
        try {
            // Open ServerSocket Waiting for peer connection
            ServerSocket server = new ServerSocket(8000);
            // socketList for multiple connect
            socketlist = new ArrayList<Socket>();
            while (true) {
                Socket socket = server.accept();
                // add socket and index to clientItem
                clientItem.add(index, socket);
                // add socket to socketList
                socketlist.add(socket);
                // Show peer's info on UI
                list.setModel(clientItem);

                //Update and Save UHPT
                int routingMetric = (int) (Math.random() * 100 + 1);
                Node uhptNode = new Node(6, String.valueOf(socket.getPort()), index, routingMetric, (7000 + index));
                uhpt.joinToRing(uhpt.getLastNode(), uhptNode, 6);
                // write UHPT info in .txt file
                FileUtil.writeUHPT(UHPTSrc, uhpt);

                //Update and Save UHRT
                String ResourseName = FileUtil.randomFileName(6);
                List peerList = new ArrayList();
                peerList.add(index);
                Node uhrtNode = new Node(6, ResourseName, peerList);
                uhrt.joinToRing(uhrt.getLastNode(), uhrtNode, 6);
                // write UHRT info in .txt file
                FileUtil.writeUHRT(UHRTSrc, uhrt);

                // Display the information about connected peers on the UI
                ReceivedMessage.append("Peer" + clientNo + " connected from: " + socket.getInetAddress().getHostAddress() + " port:" + socket.getPort() + "\nTime:" + new Date() + "\n");
                UHPT.setText(null);
                UHRT.setText(null);

                // Display UHPT and UHRT information on UI
                writeByFile();

                // Each connection enables a separate thread for communication
                SocketHandler handler = new SocketHandler(socket, clientNo);
                Thread thread = new Thread(handler);
                thread.start();

                // Notifies other peers of the update of new resources
                sendMsg("A new resource: " + ResourseName,socket);
                clientNo++;
                index++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Separate threads for each peer and server
    class SocketHandler implements Runnable {
        private Socket socket;
        private int no;

        public SocketHandler(Socket socket, int clientno) {
            this.socket = socket;
            this.no = clientno;
        }

        public void run() {
            try {
                // Initialize the current UHRT to the peer as its DHRT
                InputStreamReader reader = new InputStreamReader(socket.getInputStream());
                BufferedReader buffer_reader = new BufferedReader(reader);
                String client = "<" + socket.getInetAddress().toString() + ":" + socket.getPort() + ">";
                OutputStream oos = socket.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(oos);
                oout.writeObject(uhrt);
                oout.flush();
                String str;

                // Receives information transmitted by peers
                while (true) {
                    try {
                        str = buffer_reader.readLine();
                        if(str!=null){
                            ReceivedMessage.append("Peer" + no + " say: " + str + "\n");
                            System.out.println(client + str);
                            // Information filtering
                            if(str.length() >= 4){
                                System.out.println(str.substring(0,4));
                                // Update the UHRT upon receiving information from the peer
                                // about the completion of resource acquisition
                                if(str.substring(0,4).equals("Peer")){
                                    String name =str.substring(str.length()-16,str.length()-10);
                                    updateUHRT(name, no);
                                    FileUtil.writeUHRT(UHRTSrc, uhrt);
                                    UHRT.setText("");
                                    UHPT.setText("");
                                    writeByFile();
                                    // Issue resource update notifications to all peers
                                    sendMsg("Peer" + no + " has obtained a new resource: " + name, null);
                                }
                                else{
                                    // UHPT Node
                                    // Check whether the resource requested by the peer exists
                                    Node aimNode = handleRes(str);
                                    String response = null;
                                    PrintWriter writer = new PrintWriter(socket.getOutputStream());
                                    // If Not exists
                                    if(aimNode == null){
                                        response = "Wrong resource name！";
                                        writer.println(response);
                                        writer.flush();
                                    }
                                    else{
                                        // Sends information to peer about its requested resource
                                        Socket aimSocket = socketlist.get(aimNode.getPeerNumber());
                                        sendResInfo("Peer" + no + " will get your resource: " + str, aimSocket);
                                        response = "Resource " + str + " in Peer" + aimNode.getPeerNumber() + " with Link port: " + aimNode.getLinkPort();
                                        System.out.println(response);
                                        writer.println(response);
                                        // UHRT node
                                        Node node = getUHRTGuid(str);
                                        String guid = node.getGuid();
                                        int identifier = node.getIdentifier();
                                        // Simultaneously transmit the GUID and Identifier for resource to the peer to facilitate its update of DHRT
                                        writer.println("GUID: " + guid);
                                        writer.println("identifier: " + identifier);
                                        System.out.println(guid);
                                        writer.flush();
                                    }
                                }
                            }
                            else{
                                // Error resource name
                                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                                writer.println("Wrong resource name！");
                                writer.flush();
                            }
                        }
                        else{
                            // Null character request error
                            PrintWriter writer = new PrintWriter(socket.getOutputStream());
                            writer.println("Please input the resource name");
                            writer.flush();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ReceivedMessage.append("Peer" + no + "wrong!" + "\n");
                        clientItem.remove(no);
                        list.setModel(clientItem);
                        break;
                    }
                }
                // close pipe
                buffer_reader.close();
                oout.flush();
                oos.close();
                oout.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Obtain the node from the UHRT by resource name
    private Node getUHRTGuid(String str) {
        Node aimNode = null;
        Node firstNode = uhrt.getLastNode().getSuccessor();
        if(firstNode.getResourseName().equals(str)){
            aimNode = firstNode;
        }
        else{
            while(firstNode != uhrt.getLastNode()){
                firstNode = firstNode.getSuccessor();
                if(firstNode.getResourseName().equals(str)){
                    aimNode = firstNode;
                    break;
                }
            }
        }
        return aimNode;
    }

    // Update UHRT after peer resource exchange
    private void updateUHRT(String name, int no) {
        String guid = null;
        System.out.println(name);
        Node firstNode = uhrt.getLastNode().getSuccessor();
        if(firstNode.getResourseName().equals(name)){
            guid = firstNode.getGuid();
        }
        else{
            while(firstNode != uhrt.getLastNode()){
                firstNode = firstNode.getSuccessor();
                if(firstNode.getResourseName().equals(name)){
                    guid = firstNode.getGuid();
                }
            }
        }
//        System.out.println(guid);
        List peerList = uhrt.getNode(guid).getPeerList();
        peerList.add(no);
        Collections.sort(peerList);
        uhrt.getNode(guid).setPeerList(peerList);
    }

    // Obtain the peer node with the smallest routing metric by resource name
    private Node handleRes(String str) {
        List peerList = null;
        Node firstNode = uhrt.getLastNode().getSuccessor();
        if(firstNode.getResourseName().equals(str))
            peerList = firstNode.getPeerList();
        else{
            while(firstNode!=uhrt.getLastNode()){
                firstNode = firstNode.getSuccessor();
                if(firstNode.getResourseName().equals(str)) {
                    peerList = firstNode.getPeerList();
                    break;
                }
            }
        }
        if(peerList == null) return null;
        return searchPeer(peerList);
    }


    // Find the peer node with the smallest routing metric through peerList
    private Node searchPeer(List peerList){
        int minMetric = 200;
        Node aimNode = null;
        List<Node> nodeList = new ArrayList<Node>();
        Node firstNode = null;
        for (Object o : peerList) {
            firstNode = uhpt.getLastNode().getSuccessor();
            if(String.valueOf(firstNode.getPeerNumber()).equals(o.toString())){
                nodeList.add(firstNode);
            }
            else{
                while(firstNode!=uhpt.getLastNode()){
                    firstNode = firstNode.getSuccessor();
                    if(String.valueOf(firstNode.getPeerNumber()).equals(o.toString())) {
                        nodeList.add(firstNode);
                        break;
                    }
                }
            }
        }
        for (Node node : nodeList) {
            if(node.getRoutingMetric() <= minMetric){
                aimNode = node;
                minMetric = node.getRoutingMetric();
            }
        }
        return aimNode;
    }

    // Send information to all peers except the specified peer.
    // Used to notify other peers of resource updates when there is a new peer connection.
    public void sendMsg(String msg, Socket aimSocket){
        for (Socket socket : socketlist) {
            if(socket != aimSocket) {
                try {
                    PrintWriter writer = new PrintWriter(socket.getOutputStream());
                    writer.println(msg);
                    System.out.println("Printing");
                    writer.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    // Send information to the specified peer.
    public void sendResInfo(String msg, Socket aimSocket){
        try {
            PrintWriter writer = new PrintWriter(aimSocket.getOutputStream());
            writer.println(msg);
            System.out.println("Printing ResInfo");
            writer.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // Display the UHPT and UHRT on the UI
    public void writeByFile(){
        File fileUHPT = new File(UHPTSrc);
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(fileUHPT));
            String str;
            while ((str = in.readLine()) != null)
            {
                UHPT.append(str+'\n');
            }
            in.close();
        }
        catch (IOException e)
        {
            e.getStackTrace();
        }
        File fileUHRT = new File(UHRTSrc);
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(fileUHRT));
            String str;
            while ((str = in.readLine()) != null)
            {
                UHRT.append(str+'\n');
            }
            in.close();
        }
        catch (IOException e)
        {
            e.getStackTrace();
        }
    }

}
