package com.jinfengxu;
import java.io.Serializable;
import java.util.HashMap;


/**
 * 1. Chord is a ring structure
 * 2. Each node is mapped to an m-bit identifier
 * 3. Support find, insert, and remove node operations
 * 4. Each node is responsible for storing the identifier as (i-1, i]
 * 5. Each node has a fingerTable
 * 6. Each node has a GUID generated by SHA-1
 * 7. The node can store HPT or HRT node
 */

public class ChordDHT implements Serializable {

    // Node collection, the key is its GUID
    private HashMap<String, Node> nodeSet;
    private Node lastNode;


    // Construct
    public ChordDHT() throws Exception {
        this.nodeSet = new HashMap<String, Node>();
        Node firstNode = null;
    }

    /**
     * Handle the joining of new nodes
     * In addition to changing the predecessor and successor relationship,
     * there may also be key migration,
     * Because this may change the range of identifiers that a node is responsible for
     * @param curNode Current node (the node that accepts the request to join)
     * @param id Node requesting to join
     * @param m Identifier space bits
     * @return Whether to join successfully
     */
    public boolean joinToRing(Node curNode, Node id, int m){
        // When the ring is not empty,
        // find the immediate successor of id, and set the corresponding domain
        if(this.nodeSet.isEmpty() == false){
            var successor = this.searchNode(curNode, id, m);
            var predecessor = successor.getPredecessor();
            id.setPredecessor(predecessor);
            id.setSuccessor(successor);
            successor.setPredecessor(id);
            predecessor.setSuccessor(id);
        }
        // Put the node into the ring,
        // including the case where the ring is empty
        this.nodeSet.put(id.getGuid(), id);

        // After the node is added, the fingerTable of some nodes may be changed
        for(Node node: this.nodeSet.values()){
            this.updateFingerTable(node, m);
        }
        lastNode = id;
        return true;
    }

    public void updateFingerTable(Node node, int m){
        for(int i = 0; i < m; i++){ //For each entry in the table
            var aIdentifier = ((int)Math.pow(2, i) + node.getIdentifier()) % (int)Math.pow(2, m);

            // Find the node responsible for this identifier
            for(Node item: this.nodeSet.values()){
                if(item.inStorageBound(aIdentifier, m) == true){
                    node.getFingerTable().put((int)Math.pow(2, i), item);
                    break;
                }
            }
        }
    }


    public Node searchNode(Node curNode, Node id, int m){
        Node result = null;
        // The node responsible for the id identifier must be the successor node of id
        if(curNode.inStorageBound(id.getIdentifier(), m) == true){
            result = curNode;
        }
        // Recursion
        else{
            int dis = (id.getIdentifier() - curNode.getIdentifier() + (int)Math.pow(2, m)) % (int)Math.pow(2, m);
            // Find the largest k such that 2^k <= dis
            int maxK = 0;
            while((int) Math.pow(2, maxK) <= dis) maxK++;
            maxK--;
            result = searchNode(curNode.getFingerTable().get((int)Math.pow(2, maxK)), id, m);
        }
        return result;
    }

    // Whether there is a node
    public boolean isIn(String key){
        return nodeSet.containsKey(key);
    }

    // get node by key
    public Node getNode(String key){
        return nodeSet.get(key);
    }

    // get the last Node
    public Node getLastNode() {
        return lastNode;
    }
}