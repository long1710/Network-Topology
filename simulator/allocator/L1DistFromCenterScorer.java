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
import simulator.Pair;

public class L1DistFromCenterScorer extends Scorer {
	//evaluates by sum of L1 distance from center

	public Pair<Long,Long> valueOf(MeshLocation center, MeshLocation[] procs, int num) {
		//returns sum of L1 distances from center
		long retVal = 0;
		for(int i=0; i<num; i++)
			retVal += center.L1DistanceTo(procs[i]);
		return new Pair<Long,Long>(retVal,Long.valueOf(0));
	}


	public Comparator<MeshLocation> getComparator(MeshLocation center){
		return new L1Comparator(center.x,center.y,center.z);
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"L1DistFromCenterScorer";
	}

}

