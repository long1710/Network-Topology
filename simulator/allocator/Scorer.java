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

import java.util.Comparator;
import simulator.HasSetupInfo;
import simulator.MeshLocation;
import simulator.Pair;

public abstract class Scorer implements HasSetupInfo {
    //a way to evaluate a possible allocation; low is better
    
    abstract public Pair<Long,Long> valueOf(MeshLocation center, MeshLocation[] procs,int num);
    //returns score associated with first num members of procs
    //center is the center point used to select these

    abstract public Comparator<MeshLocation> getComparator(MeshLocation center);
}

