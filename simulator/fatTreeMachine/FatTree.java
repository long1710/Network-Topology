package simulator.fatTreeMachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import simulator.Factory;
import simulator.Machine;
import simulator.Main;
import simulator.allocator.AllocInfo;

public class FatTree extends Machine {

    final int T1 = 1;
    final int T2 = 2;
    final int T3 = 3;
    
    boolean[] isFree = new boolean[3];
    int NodesPerSwitch, maxPorts;
    int aggSwitchPerPod, edgeSwitchPerPod;

    public class L1Switch implements Comparable<L1Switch>{
        int[] nodes;
        int freeNodes;
        public int highestJob = T1;
        public L1Switch(int nodes){
            this.nodes = new int[nodes];
            maxPorts += nodes;
            freeNodes = nodes;
        }

        public int compareTo(L1Switch sw) {
            if(this.freeNodes >= sw.freeNodes){
                return -1;
            } else {
                return 1;
            }
        }
    }

    public class Pod implements Comparable<Pod>{//Pods
        List<L1Switch> l1Switches;
        public boolean T3Job = false;
        public int freeNodePerPods = NodesPerSwitch/2 * NodesPerSwitch/2;
        public Pod(List<L1Switch> l1Switches){
            this.l1Switches = l1Switches;
        }

        public int compareTo(Pod pod) {
            if(this.freeNodePerPods >= pod.freeNodePerPods){
                return 1;
            } else {
                return -1;
            }
        }
    }

    List<Pod> pods;
    //map to keep track of toString and debug
    //maximum fat tree ports are (n^3)/4
    HashMap<Pod, HashMap<L1Switch, String>> map = new HashMap<>();
    public FatTree(int NodesPerSwitch) {
        //Initiatlizing pods
        this.NodesPerSwitch = NodesPerSwitch;
        this.pods = new ArrayList<>();
        this.aggSwitchPerPod = (NodesPerSwitch/2);
        this.edgeSwitchPerPod = (NodesPerSwitch/2);
        //number of pods = NodesPerSwitch

        for(int i = 0; i< NodesPerSwitch;i++){   
            HashMap<L1Switch, String> temp_map = new HashMap<>();
            List<L1Switch> switches = new ArrayList<>();
            for(int o = 0; o < edgeSwitchPerPod; o++){
                L1Switch sw = new L1Switch(NodesPerSwitch/2);
                temp_map.put(sw, "pod: "+ i + " switch: " + o);
                switches.add(sw);
            }
            this.pods.add(new Pod(switches));
            map.put(this.pods.get(i), temp_map);
        }
        reset();
    
    }

    //TODO: make a fat tree that has number of ports inside
    public static FatTree Make(ArrayList<String> params){
        //Factory.argsAtLeast(1, params);
        //return new FatTree(Integer.parseInt(params.get(1)));
        return new FatTree(11);
    }

    public String getSetupInfo(boolean comment) {
        String com;
        if(comment){
            com = "# ";
        } else {
            com = "";
        }
        return com + maxPorts+ "max ports";
    }

    public int getmachSize(){
        return maxPorts;
    }

    public boolean getIsFree(L1Switch sw, int node){
        return sw.nodes[node] == 0;
    }

    public void reset() {
        numAvail = maxPorts;
        numProcs = maxPorts;
        for(Pod pod: pods){
            pod.T3Job = false;
            pod.freeNodePerPods =  NodesPerSwitch/2 * NodesPerSwitch/2;
            for(L1Switch sw: pod.l1Switches){
                sw.freeNodes = sw.nodes.length;
                sw.highestJob = T1;
                for(int node = 0; node < sw.nodes.length; node++){
                    sw.nodes[node] = 0;
                }
                sw.freeNodes = sw.nodes.length;
            }
        }
    }

    public ArrayList<FatTreeLocation> freeProcessors(){
        ArrayList<FatTreeLocation> retVal = new ArrayList<FatTreeLocation>();
        for(Pod pod: pods){
            for(L1Switch sw: pod.l1Switches){
                for(int node = 0; node < sw.nodes.length; node++){
                    if(sw.nodes[node] == 0){
                        FatTreeLocation loc = new FatTreeLocation(pod, sw, node);
                        loc.setFatTree(this);
                        retVal.add(loc);
                    }
                }
            }
        }
        return retVal;
    }
    
    public ArrayList<FatTreeLocation> usedProcessors(){
        ArrayList<FatTreeLocation> retVal = new ArrayList<FatTreeLocation>();
        for(Pod pod: pods){
            for(L1Switch sw: pod.l1Switches){
                for(int node = 0; node < sw.nodes.length; node++){
                    if(sw.nodes[node] != 0){
                        FatTreeLocation loc = new FatTreeLocation(pod, sw, node, sw.nodes[node]);
                        loc.setFatTree(this);
                        retVal.add(loc);
                    }
                }
            }
        }
        return retVal;
    }
   
    public void allocate(AllocInfo allocInfo) {
        //locations to allocate to machine
        FatTreeLocation[] procs = ((FatTreeAllocInfo) allocInfo).processors;
        for(int i = 0; i < procs.length; i++){
            if(procs[i].sw_.nodes[procs[i].node] != 0)
                Main.error("Attemp to allocate a busy processor: " + procs[i] + " "+procs.length);                 
            procs[i].sw_.nodes[procs[i].node] = procs[i].type;
            procs[i].sw_.freeNodes--;
            procs[i].pod_.freeNodePerPods--;
            //allocate tier3 Job
            if(procs[i].type == T3){
                procs[i].pod_.T3Job = true;
                procs[i].sw_.highestJob = T3;
            }
            //allocate tier 2 job
            if(procs[i].type == T2){
                procs[i].sw_.highestJob = T2;
            }
        }
        numAvail -= procs.length;
    }
    
    public void deallocate(AllocInfo allocInfo) {
        FatTreeLocation[] procs = ((FatTreeAllocInfo) allocInfo).processors;
        for(int i = 0; i < procs.length; i++){
            if(procs[i].sw_.nodes[procs[i].node] == 0)
                Main.error("Attemp to allocate a free processor: " + procs[i] + " "+procs.length);
            procs[i].sw_.nodes[procs[i].node] = 0;
            procs[i].sw_.freeNodes++;
            procs[i].pod_.freeNodePerPods++;

            // each switch can only contain T2 or T3
            // so if we deallocate T2 or T3, either T1 or 0 is left - 0 or 1 doesnt affect allocate
            if(procs[i].type != T1)
                procs[i].sw_.highestJob = T1;
            //Deallocate T3 job
            if(procs[i].type == T3){
                procs[i].pod_.T3Job = false;
            }
        }
        numAvail += procs.length;
    }

    //print out string representation of current machine;
    public String toString(){
        String representation = "";
        for(Pod pod: pods){
            for(L1Switch sw: pod.l1Switches){
                representation += map.get(pod).get(sw) + "\n" +Arrays.toString(sw.nodes) +"\n";
            }
        }
        return representation == "" ? "no toString representation is available" : representation;
    }

    //print out representation of one FatTreeLocation in machine
    public String toString(Pod pod, L1Switch sw, int node){
        String representation = "";
        representation += map.get(pod).get(sw) + "\n" +Arrays.toString(sw.nodes) +"\n";
        return representation;
    }
}