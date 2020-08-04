/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package simulator.allocator;

import java.util.ArrayList;
import simulator.HasSetupInfo;
import simulator.Job;
import simulator.Machine;
import simulator.MeshLocation;

public abstract class Allocator implements HasSetupInfo {

    protected Machine machine;  //machine being allocated

    public boolean canAllocate(Job j) {
        //returns whether j can be allocated
        //default strategies are non-contig so "true" if enough free processors

        return (machine.numFreeProcessors() >= j.getProcsNeeded());
    }

    public boolean canAllocate(Job j, ArrayList<MeshLocation> available){
        //test possibility of allocating using given free processors
        //does not modify machine state

        return (available.size() >= j.getProcsNeeded());
    }

    abstract public AllocInfo allocate(Job job);
    //allocates job if possible
    //returns information on the allocation or null if it wasn't possible
    //(doesn't make allocation; merely returns info on possible allocation)

    public void deallocate(AllocInfo aInfo) {
        //in case the Allocator wants to know when a job is deallocated
        //added for MBS, which wants to update its data structures

        //default version does nothing...
    }

    public void done() {
        //called at end of simulation
        //allows allocator to report statistics
    }
}
