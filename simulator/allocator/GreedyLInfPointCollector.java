/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * GreedyLInf uses the same initial strategy as the regular LInf point
 * collector, but differs in how selects points from the outer shell.
 * GreedyLInf will pick a point in the outer shell that is closest (in terms of
 * L1 distance to the rest of the group).  The next point it picks will be
 * closest to the inner+newly selected points.  In the case of a tie, between
 * two points equally close to the group of points selected so far, a comparison
 * is made between how close the point is to the center.  This method is mainly
 * useful in preventing an allocation from looking like this:
 * 
 *   *
 *    C
 *    **
 * 
 * when it could look like this:
 * 
 *   **
 *   *C
 *   
 * Both equal in terms of the regular LInf point collector, but one is definitely more correct.
 */

package simulator.allocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import simulator.MeshLocation;

public class GreedyLInfPointCollector extends PointCollector{

	/**
	 * PointInfo will keep locations, and distances together
	 */
	private class PointInfo implements Comparable<PointInfo> {
		MeshLocation point;
		int L1toGroup;
		long tieBreaker;	//This is used as the tie breaker before MeshLocation ordering

		public PointInfo(MeshLocation point, int L1toGroup){
			this.point = point;
			this.L1toGroup = L1toGroup;
			this.tieBreaker = 0;
		}

		public int compareTo(PointInfo o) {
			if (o.L1toGroup == this.L1toGroup){
				if (o.tieBreaker == this.tieBreaker){
					return this.point.compareTo(o.point);   //TODO how does using this location in the ordering add bias to the final result?
				}
				return (int) (this.tieBreaker - o.tieBreaker);
			}
			return this.L1toGroup - o.L1toGroup;
		}

		public String toString(){
			return "{"+this.point+","+this.tieBreaker+","+this.L1toGroup+"}";
		}
	}

	public MeshLocation[] getNearest(MeshLocation center, int num,
			MeshLocation[] available) {
		LInfComparator lic = new LInfComparator(center.x, center.y, center.z);
		Arrays.sort(available, lic);//LInfComparator sorts according to LInf distance from center

		//Skip to the outer shell
		int outerIndex = 0;	//The index of the first MeshLocation of the Outermost Shell
		int outerShell = available[0].LInfDistanceTo(center);	// This gives us the shell number for a MeshLocation
		// one past the last one we would normally use
		for (int i=1; i<num; ++i){
			int newOuterShell = available[i].LInfDistanceTo(center);
			if (newOuterShell > outerShell){
				outerShell = newOuterShell;
				outerIndex = i;
			}
		}

		//Put all of the Inner Shell's processors together
		ArrayList<MeshLocation> innerProcs = new ArrayList<MeshLocation>();
		for (int i=0; i<outerIndex; i++){
			innerProcs.add(available[i]);
		}

		//Put points in the outer shell into PointInfos with L1 distance to rest of group
		TreeSet<PointInfo> outerProcs = new TreeSet<PointInfo>();
		for (int i=outerIndex;i<available.length;i++){
			PointInfo outerPoint = new PointInfo(available[i], L1toInner(available[i],innerProcs));
			outerPoint.tieBreaker = outerPoint.point.L1DistanceTo(center);
			outerProcs.add(outerPoint);
		}

		//Find the minimum L1toGroup, add it to available
		int totalSelected = innerProcs.size();	//Keeps track of all the processors thus far added
		while (totalSelected < num){
			TreeSet<PointInfo> tempSet = new TreeSet<PointInfo>();
			PointInfo first = outerProcs.first();
			innerProcs.add(first.point);	//This is the current minimum of the set (lowest L1 distance to the rest of the inner procs)
			outerProcs.remove(first);
			++totalSelected;	//Make progress in the loop
			//recalculate all the other distances adding this point into the inner group.
			if (totalSelected < num){
				for (PointInfo info : outerProcs){
					//instead of recalculating the whole thing, we just need to add another length to the total distance
					info.L1toGroup = info.L1toGroup + info.point.L1DistanceTo(first.point);
					tempSet.add(info);
				}
				outerProcs = tempSet; //Switch sets back
			}
		}

		//convert back to return type
		available = innerProcs.toArray(new MeshLocation[innerProcs.size()]);
		return available;
	}

	//loc shouldn't be in innerProcs
	private int L1toInner(MeshLocation outer, ArrayList<MeshLocation> innerProcs) {
		int distance = 0;
		for (MeshLocation inner : innerProcs)
			distance += outer.L1DistanceTo(inner);
		return distance;
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"LInfPointCollector";
	}
}
