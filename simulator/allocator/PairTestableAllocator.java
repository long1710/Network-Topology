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

import simulator.ContiguousJob;
import simulator.Mesh;

public interface PairTestableAllocator {
    public AllocInfo pairTest(Mesh m1, ContiguousJob j1,
			      Mesh m2, ContiguousJob j2);
    //checks if j1 can be allocated in m1 w/o using processors needed
    //to allocate j2 in m2.  Returns the allocation of j1 if this is
    //possible; null otherwise.
}