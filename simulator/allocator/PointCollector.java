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

import simulator.HasSetupInfo;
import simulator.MeshLocation;

//actual implementations don't just return the nearest; they reorder
//the points and culling is done by the scorer
//TODO: change the name of getNearest to reflect this...

public abstract class PointCollector implements HasSetupInfo {
    //a way to gather nearest free processors to a given center
    
    public abstract MeshLocation[] getNearest(MeshLocation center, int num,
					      MeshLocation[] available);
    //returns num nearest locations to center from available
    //may reorder available and return it
}

