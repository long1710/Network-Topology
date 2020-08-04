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

import java.util.Arrays;
import simulator.MeshLocation;

public class L1PointCollector extends PointCollector {
    //collects points nearest to center in terms of L1 distance
    
    public MeshLocation[] getNearest(MeshLocation center, int num,
				     MeshLocation[] available) {
	L1Comparator L1c = new L1Comparator(center.x, center.y, center.z);
	Arrays.sort(available, L1c);
	return available;
    }

    public String getSetupInfo(boolean comment){
        String com;
        if(comment) com="# ";
        else com="";
        return com+"L1PointCollector";
    }

}

