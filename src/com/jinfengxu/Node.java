package com.jinfengxu;

import com.jinfengxu.utils.SHA1Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

// Nodes that can store both HRT and HPT
public class Node implements Serializable {

    // identifier
    private int identifier;
    // FingerTable maintains m <index, ChordNode>,
    // where m is the number of bits in the hash value of the selected hash function
    private HashMap<Integer, Node> fingerTable; //key: 1，2，4，8，16……
    private Node predecessor;
    private Node successor;
    // A random ID is used to ensure the uniqueness of the resource/peer
    private String guid;
    private String port;
    private List peerList = new ArrayList(); //peerList
    private int peerNumber;
    private String resourseName;
    private int routingMetric;
    private int linkPort;

    // Constructor used to generate HPT peer nodes
    public Node(int m, String port, int peerNumber, int routingMetric, int linkPort) throws Exception {
        this.linkPort = linkPort;
        this.routingMetric = routingMetric;
        this.peerNumber = peerNumber;
        this.port = port;
        // Use SHA-1 to generate a random guid
        this.guid = SHA1Util.shaEncode(port);
        // Map IP addresses to M bit identifiers to generate M table items
        this.identifier = hashFunc(m, port);
        this.fingerTable = new HashMap<Integer, Node>();
        for(int i = 0; i < m; i++){
            this.fingerTable.put((int)Math.pow(2, i), this);
        }
        this.predecessor = this;
        this.successor = this;
    }

    // Constructor used to generate HRT peer nodes
    public Node(int m, String resourseName, List peerList) throws Exception {
        this.peerList = peerList;
        this.resourseName = resourseName;
        this.guid = SHA1Util.shaEncode(resourseName);
        // Map IP addresses to M bit identifiers to generate M table items
        this.identifier = hashFunc(m, resourseName);
        this.fingerTable = new HashMap<Integer, Node>();
        for(int i = 0; i < m; i++){
            this.fingerTable.put((int)Math.pow(2, i), this);
        }
        this.predecessor = this;
        this.successor = this;
    }

    // Constructor used to update DHRT peer nodes
    public Node(int m, String resourseName, String guid, List peerList, int identifier){
        this.peerList = peerList;
        this.resourseName = resourseName;
        this.guid = guid;
        this.identifier = identifier;
        this.fingerTable = new HashMap<Integer, Node>();
        for(int i = 0; i < m; i++){
            this.fingerTable.put((int)Math.pow(2, i), this);
        }
        this.predecessor = this;
        this.successor = this;
    }

    // hash Function
    private int hashFunc(int m, String port){
        Random random = new Random(System.currentTimeMillis());
        int result = 0;
        for(int i = 0; i < port.length(); i++){
            if(port.charAt(i) <= '9' && port.charAt(i) >= '0') {
                result = (result + (int)port.charAt(i))  % (int)Math.pow(2, m);
            }
        }
        return result % (int)Math.pow(2, m);
    }


    //Responsible for the scope of the identifier
    /**
     * There are three possibilities:
     * 1. Larger identifier of the precursor node (e.g., 5 --> 1)
     * 2. The precursor node identifier is smaller (e.g., 3 --> 7)
     * 3. The precursor node identifier is the same value (when there is only one node in the ring)
     */
    public boolean inStorageBound(int aIdentifier, int m){
        var lower = this.predecessor.identifier;
        var upper = this.identifier;

        //First situation
        if(lower > upper){
            if(aIdentifier > lower && aIdentifier < (int)Math.pow(2, m)){
                return true;
            }
            if(aIdentifier < upper){
                return true;
            }
        }

        //Second situation
        if(aIdentifier > lower && aIdentifier <= upper){
            return true;
        }

        //Third situation
        //Since there is only one node, it is responsible for all identifiers
        if(lower == upper){
            return true;
        }
        return false;
    }


    // Getter and Setter
    public HashMap<Integer, Node> getFingerTable(){
        return this.fingerTable;
    }

    public int getIdentifier(){
        return this.identifier;
    }

    public Node getPredecessor(){
        return this.predecessor;
    }

    public Node getSuccessor(){
        return this.successor;
    }

    public boolean setPredecessor(Node node){
        this.predecessor = node;
        return true;
    }

    public boolean setSuccessor(Node node){
        this.successor = node;
        return true;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public List getPeerList() {
        return peerList;
    }

    public void setPeerList(List peerList) {
        this.peerList = peerList;
    }

    public int getPeerNumber() {
        return peerNumber;
    }

    public void setPeerNumber(int peerNumber) {
        this.peerNumber = peerNumber;
    }

    public String getResourseName() {
        return resourseName;
    }

    public void setResourseName(String sourseName) {
        this.resourseName = sourseName;
    }

    public int getRoutingMetric() {
        return routingMetric;
    }

    public void setRoutingMetric(int routingMetric) {
        this.routingMetric = routingMetric;
    }

    public int getLinkPort() {
        return linkPort;
    }

    public void setLinkPort(int linkPort) {
        this.linkPort = linkPort;
    }

}
