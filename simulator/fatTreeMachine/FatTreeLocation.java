package simulator.fatTreeMachine;
import simulator.fatTreeMachine.FatTree.*;

public class FatTreeLocation {
    public int node, type;
    public Pod pod_;
    public L1Switch sw_;


    public FatTree fatTree = null;
    
    //allocation for Pollard
    public FatTreeLocation(Pod pod, L1Switch sw, int node, int type){
        this.pod_ = pod;
        this.sw_ = sw;
        this.node = node;
        this.type = type;
    }

    //Location for normal FatTree
    public FatTreeLocation(Pod pod, L1Switch sw, int node){
        this.pod_ = pod;
        this.sw_ = sw;
        this.node = node;
    }

    public void setFatTree(FatTree FatTree){
        this.fatTree = FatTree;
    }


    public String toString(){
        String information = "";
        try{
            information = fatTree.map.get(pod_).get(sw_);
        } catch(Exception e){
            information = e.toString();
        }
        return information;
    }

}