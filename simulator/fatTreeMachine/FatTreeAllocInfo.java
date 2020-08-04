package simulator.fatTreeMachine;

import simulator.Job;
import simulator.allocator.AllocInfo;

public class FatTreeAllocInfo extends AllocInfo{
    
    public FatTreeLocation[] processors;
    public FatTreeAllocInfo(Job j){
        super(j);
        processors = new FatTreeLocation[j.getProcsNeeded()];
    }
}