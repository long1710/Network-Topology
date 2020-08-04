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

import simulator.Job;
import simulator.Mesh;
import simulator.MeshLocation;

public abstract class ContiguousAllocator extends Allocator {
    
    public Mesh meshMachine;

    abstract public MeshLocation canAllocate(Job job, Mesh mesh);
    //whether it could allocate job in a given machine state
    //returns possible base point (making it useful as helper function
    //  to make allocations); returns null if none
}
