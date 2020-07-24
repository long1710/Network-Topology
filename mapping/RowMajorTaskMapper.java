/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package mapping;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import simulator.MeshLocation;

public class RowMajorTaskMapper extends TaskMapper {
	private class RowComparator implements Comparator<MeshLocation> {
		public int compare(MeshLocation loc, MeshLocation otherLoc){
			if(otherLoc.y == loc.y){
				if(otherLoc.x == loc.x){
					return loc.z - otherLoc.z;
				}
				return loc.x - otherLoc.x;
			}
			return loc.y - otherLoc.y;
		}

	}

	public Map<Integer, MeshLocation> mesh_map(MeshLocation[] chosen_procs, JobDimension dim) {
		Map<Integer, MeshLocation> retMap = new HashMap<Integer, MeshLocation>();

		orderJobDimensions(chosen_procs, dim);

		RowComparator r = new RowComparator();
		Arrays.sort(chosen_procs, r);

		int i = 0;
		int numAlloced = 0;
		for (MeshLocation m: chosen_procs) {
			retMap.put(i, m);
			if(numAlloced >= ((dim.x * dim.y * dim.z) - 1))
				break;
			numAlloced++;
			MeshLocation current = this.loc(i, dim);
			if((current.x == dim.x - 1) && (current.y == dim.y - 1))
				i = this.num(new MeshLocation(0, 0, current.z + 1), dim);
			else if(current.x == dim.x - 1)
				i = this.num(new MeshLocation(0, current.y + 1, current.z), dim);
			else
				i = this.num(new MeshLocation(current.x + 1, current.y, current.z), dim);
		}

		return retMap;
	}
}
