/*
 * Copyright (c) 2014, Knox College
 * All rights reserved.
 *
 * This file is part of the PReMAS software package. For license information, see the LICENSE file
 * in the top level directory of the distribution.
 */

//TODO: description

package mapping;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import simulator.MeshLocation;

public class TwoWayPGTaskMapper extends TaskMapper {

	//this comparator takes the coordinates of a node
	//and compares MeshLocations based on their L1
	//distance from that node
	private class PointComparator implements Comparator<MeshLocation> {

		int x;
		int y;
		int z;
		boolean up;

		public PointComparator(int X, int Y, int Z, boolean UP) {
			this.x = X;
			this.y = Y;
			this.z = Z;
			this.up = UP;
		}

		public int compare(MeshLocation loc, MeshLocation otherLoc){
			if(loc.L1DistanceTo(new MeshLocation(x, y, z)) - otherLoc.L1DistanceTo(new MeshLocation(x, y, z)) != 0)
				return loc.L1DistanceTo(new MeshLocation(x, y, z)) - otherLoc.L1DistanceTo(new MeshLocation(x, y, z));
			else if(loc.y != otherLoc.y) {
				if(up)
					return loc.y - otherLoc.y;
				else
					return otherLoc.y - loc.y;
			} else if(loc.x != otherLoc.x) {
				if(up)
					return loc.x - otherLoc.x;
				else
					return otherLoc.x - loc.x;
			} else {
				if(up)
					return loc.z - otherLoc.z;
				else
					return otherLoc.z - loc.z;
			}

		}
	}

	public Map<Integer, MeshLocation> mesh_map(MeshLocation[] chosen_procs, JobDimension dim) {
		Map<Integer, MeshLocation> retMap = new HashMap<Integer, MeshLocation>();

		orderJobDimensions(chosen_procs, dim);
		
		Comparator<MeshLocation> c = new PointComparator(meshMinX, meshMinY, meshMinZ, true);
		Arrays.sort(chosen_procs, c);

		int i = 0;  //current job processor number
		int numAlloced = 0;  //number processors allocated so far

		//as long as there are still MeshLocations in
		//the chosen_procs array
		while(chosen_procs.length > 0) {
			//take the first MeshLocation in chosen_procs
			MeshLocation m = chosen_procs[0];

			//remove the first MeshLocation from chosen_procs
			MeshLocation[] temp = new MeshLocation[chosen_procs.length - 1];
			for(int k = 1; k < chosen_procs.length; k++)
				temp[k - 1] = chosen_procs[k];
			chosen_procs = temp;

			retMap.put(i, m);  //add the processor and MeshLocation to the mapping

			numAlloced++;
			if(numAlloced == dim.x * dim.y * dim.z)
				break;  //stop mapping if size of job reached

			//update the comparator to give priority to the coordinate
			//nearest the expected location of the next location
			boolean up;
			if(numAlloced % 2 != 0) {
				up = false;
				i = (dim.x * dim.y * dim.z) - 1 - (numAlloced / 2);
				MeshLocation next = this.loc(i, dim);
				c = new PointComparator(meshMaxX - (dim.x - 1 - next.x), meshMaxY - (dim.y - 1 - next.y), meshMaxZ - (dim.z - 1 - next.z), up);
			} else {
				up = true;
				i = numAlloced / 2;
				MeshLocation next = this.loc(i, dim);
				c = new PointComparator(next.x + meshMinX, next.y + meshMinY, next.z + meshMinZ, up);
			}
			Arrays.sort(chosen_procs, c);
		}

		return retMap;
	}
}
