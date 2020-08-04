/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

package simulator.allocator;

import java.util.Comparator;
import simulator.MeshLocation;

public class LInfComparator implements Comparator<MeshLocation> {
	//compares points by L infinity distance to a reference point
	//Warning: not consistent with equals (will call diff pts equal)

	private MeshLocation pt;  //point we measure distance from

	public LInfComparator(int x, int y, int z) {
		//constructor that takes coordinates of reference point
		pt = new MeshLocation(x, y, z);
	}

	public int compare(MeshLocation L1, MeshLocation L2) {
		return pt.LInfDistanceTo(L1) - pt.LInfDistanceTo(L2);
	}

	public boolean equals(Object other) {
		if(!(other instanceof LInfComparator))
			return false;
		LInfComparator castOther = (LInfComparator)other;
		return pt.equals(castOther.pt);
	}


	public String toString(){
		return "LInfComparator";
	}
}
