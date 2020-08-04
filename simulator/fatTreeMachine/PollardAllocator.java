//test run allocator for fat tree
package simulator.fatTreeMachine;

import simulator.fatTreeMachine.FatTree.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import simulator.Job;
import simulator.Main;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;

public class PollardAllocator extends Allocator {

    private AllocInfo info;

    public PollardAllocator(FatTree fatTree){
        machine = fatTree;
    }

    public static PollardAllocator Make(ArrayList<String> params){
        try{
            FatTree fatTree = (FatTree) Main.getMachine();
            return new PollardAllocator(fatTree);
        } catch( ClassCastException e){
            Main.error("Fat tree allocator requires FatTree");
        }
        return null;
    }
    public String getSetupInfo(boolean comment) {
        return "Pollard Allocator for Fat Tree";
    }

    @Override
    public boolean canAllocate(Job j){
        if(machine.numFreeProcessors() < j.getProcsNeeded()){
            return false;
        }
        int PortsPerL1Switch = ((FatTree)machine).NodesPerSwitch/2;
        int PortsPerPod = PortsPerL1Switch * PortsPerL1Switch;
        List<Pod> pods = ((FatTree)machine).pods;
        PollardAllocate allocator = new PollardAllocate(j, PortsPerL1Switch, PortsPerPod, pods);
        AllocInfo info = allocator.resource_allocation();
        if(info != null){
            this.info = info;
            return true;
        }
        return false;
    }

    public AllocInfo allocate(Job j) {
        if(!canAllocate(j)){
            return null;
        }
        return info;
    }

    public class PollardAllocate{
        //PollardAllocate, similar to Pollard pper
    
        int N;//N = procs per Job
        int k;//k = nodes per switch
        int p;//p = nodes per pods
        Job job;
        List<Pod> pods;
        public PollardAllocate(Job job, int k, int p, List<Pod> pods){
            this.job = job;
            this.N = job.getProcsNeeded();
            this.k = k;
            this.p = p;
            this.pods = pods;
        }

        public AllocInfo resource_allocation(){
            
            if(N <= k){
                //check if job can fit into one L1Switches
                return resource_allocate_small(pods);
            } else if(N <= p){
                //check if job can fit into one pod
                return resource_allocate_medium(pods);
            } else {
                //job fits into multiple job
                return resource_allocate_large(pods);
            }
        }

        public AllocInfo resource_allocate_small(List<Pod> pods){
            int N = this.N;
            FatTreeAllocInfo retval =  new FatTreeAllocInfo(job);
            Collections.sort(pods);
            int index = 0;
            for(Pod pod: pods){
                //sort least to most
                Collections.sort(pod.l1Switches);
                for(L1Switch sw: pod.l1Switches){
                    if(sw.freeNodes >= N){
                        for(int i = 0; i < sw.nodes.length;i++){
                            if(sw.nodes[i] == 0){
                                retval.processors[index++] = new FatTreeLocation(pod, sw, i, 1);
                                N--;
                            }
                            if(N == 0)  
                                return retval;
                        }
                    }
                }
            }
            return null;
        }

        public AllocInfo resource_allocate_medium(List<Pod> pods){
            FatTreeAllocInfo retval =  new FatTreeAllocInfo(job);
            //System.out.println("retVal needs " + retval.processors.length);
            Collections.sort(pods);
            for(Pod pod: pods){
                int N = this.N;
                int index = 0;
                //sort L1 switch base on available nodes, most to least
                Collections.sort(pod.l1Switches);
                Collections.reverse(pod.l1Switches);
                for(L1Switch sw: pod.l1Switches){
                    if(sw.highestJob == 2 || sw.highestJob == 3) continue; //skip switches with T2 and T3 job
                    for(int i = 0; i < sw.nodes.length; i++){
                       // System.out.println("allocating " + index);
                        if(sw.nodes[i] == 0){
                            retval.processors[index++] = new FatTreeLocation(pod, sw, i , 2);
                            N--;
                        }
                        if(N == 0){
                            return retval;
                        }
                    }
                }
            }
            return null;
        }
        public AllocInfo resource_allocate_large(List<Pod> pods){
            FatTreeAllocInfo retval =  new FatTreeAllocInfo(job);
            Collections.sort(pods);
            int index = 0;
            int N = this.N;
            for(Pod pod: pods){
                //skip job if contains T3 job
                if(pod.T3Job) continue; 
                //sort L1 switch base on available nodes, most to least
                Collections.sort(pod.l1Switches);
                Collections.reverse(pod.l1Switches);
                for(L1Switch sw: pod.l1Switches){
                    if(sw.highestJob == 2) continue; //skip switches with T2 job
                    for(int i = 0; i < sw.nodes.length; i++){
                        if(sw.nodes[i] == 0){
                            retval.processors[index++] = new FatTreeLocation(pod, sw, i , 3);
                            N--;
                        }
                        if(N == 0){
                            return retval;
                        }
                    }
                }
            }
            //System.out.println("fail to allocate at this time ");
            //System.out.println(((FatTree)machine).toString());
            return null;
        }
    }
    
}