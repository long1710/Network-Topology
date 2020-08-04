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

//Finds the sum of LInf distances of num nearest processors from center
public class LInfDistFromCenterScorer extends Scorer {


	private Tiebreaker tiebreaker;

	public Pair<Long,Long> valueOf(MeshLocation center, MeshLocation[] procs, int num) {
		//returns the sum of the LInf distances of the num closest processors

		long retVal = 0;
		for (int i = 0; i < num; i++) 
			retVal += center.LInfDistanceTo(procs[i]);

		long tiebreak = tiebreaker.getTiebreak(center,procs,num);

		return new Pair<Long,Long>(retVal,tiebreak);
	}

	public LInfDistFromCenterScorer(Tiebreaker tb){
		tiebreaker=tb;
	}

	public String getLastTieInfo(){
		return tiebreaker.lastTieInfo;
	}

	public Comparator<MeshLocation> getComparator(MeshLocation center){
		return new LInfComparator(center.x,center.y,center.z);
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"LInfDistFromCenterScorer (Tiebreaker: "+tiebreaker.getInfo()+")";
	}
} 
