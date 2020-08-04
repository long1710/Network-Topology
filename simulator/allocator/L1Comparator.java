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
import simulator.MeshLocation;

public class L1Comparator implements Comparator<MeshLocation> {
    //compares points by L1 distance to a reference point
    //Warning: not consistent with equals (will call diff pts equal)
    
    private MeshLocation pt;  //point we measure distance from
    
    public L1Comparator(int x, int y, int z) {
	//constructor that takes coordinates of reference point
	pt = new MeshLocation(x, y, z);
    }
    
    public int compare(MeshLocation L1, MeshLocation L2) {
	return pt.L1DistanceTo(L1) - pt.L1DistanceTo(L2);
    }
    
    public boolean equals(Object other) {
	if(!(other instanceof L1Comparator))
		return false;
	L1Comparator castOther = (L1Comparator)other;
	return pt.equals(castOther.pt);
    }

    public String toString(){
      return "L1Comparator";
    }
}

