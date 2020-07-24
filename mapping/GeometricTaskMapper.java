/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Implementation of mapping by recursive bisection.  Originally called
 * RCB (for Recursive Coordinate Bisection) in
 *     V.J. Leung, D.P. Bunde, J. Ebbers, S.P. Feer, N.W. Price,
 *     Z.D. Rhodes, and M. Swank. Task mapping stencil computations
 *     for non-contiguous allocations. In Proc. 19th ACM SIGPLAN Symp.
 *     Principles and Practice of Parallel Programming (PPoPP),
 *     pages 377-378, 2014.
 * Subsequently renamed Geom (for Geometric).  Idea is to split the task
 * along its longest dimension, split the allocated nodes along the same
 * dimension, and then recursively map each half.
 */

package mapping;

import java.util.*;
import simulator.Main;
import simulator.MeshLocation;

public class GeometricTaskMapper extends TaskMapper {

	private class CoordComparator implements Comparator<MeshLocation> {
		//class used to split node coodinates along a particular dimension
		
		int mainDim;	//notify which dimension to split along (0=x, 1=y, 2=z)

		public CoordComparator(int mainDim) {
			//arg is which dimension to split along (0=x, 1=y, 2=z)
			this.mainDim = mainDim;
		}

		public int compare(MeshLocation loc, MeshLocation otherLoc){
			switch (mainDim){
			case 0:	
				if (loc.x != otherLoc.x)
					return loc.x - otherLoc.x;
				else if(loc.y != otherLoc.y)
					return loc.y - otherLoc.y;				
				else
					return loc.z - otherLoc.z;
			case 1:	
				if (loc.y != otherLoc.y)
					return loc.y - otherLoc.y;
				else if(loc.x != otherLoc.x)
					return loc.x - otherLoc.x;
				else
					return loc.z - otherLoc.z;
			case 2:
				if (loc.z != otherLoc.z)
					return loc.z - otherLoc.z;
				else if(loc.x != otherLoc.x)
					return loc.x - otherLoc.x;
				else
					return loc.y - otherLoc.y;
			}
			
			Main.ierror("Geom trying to split nodes in invalid dimension");
			return -999;  //shouldn't reach this
		}
	}
	
	/** This method sets up task coordinates and other arguments for the helper method.**/
	public Map<Integer, MeshLocation> mesh_map(MeshLocation[] chosen_procs, JobDimension dim) {
		
		Map<Integer, MeshLocation> retMap = new HashMap<Integer, MeshLocation>();
		
		this.orderJobDimensions(chosen_procs, dim);    //rotate job if necessary

		int jd[] = new int[] {dim.x, dim.y, dim.z};    //argument for helper method
		
		int numNodes = dim.x * dim.y * dim.z;

		//generate list of task coordinates (coords within the job)
		MeshLocation taskCoords[] = new MeshLocation[numNodes];
		int lin = 0;
		for(int k=0; k<dim.z; k++)
			for(int j=0; j<dim.y; j++)
				for(int i=0; i<dim.x; i++) {
					taskCoords[lin] = new MeshLocation(i, j, k);
					lin++;
		}

		//call the recursive helper method to do mapping
		mesh_help(chosen_procs, taskCoords, jd, retMap, 0, numNodes, dim);	
		return retMap;
	}
	
	/**
	 * Recursive helper method for mesh_map. chosen_procs is an array of chosen processors coordinates.
	 * taskCoords is an array of task coordinates. jd is an array of job dimension.
	 * retMap is the final mapping result. start and end are the boundaries of the sorting range.
	 * dim is the original job dimension.  
	 *
	 **/
	private void mesh_help(MeshLocation[] chosen_procs, MeshLocation[] taskCoords, int[] jd, 
			Map<Integer, MeshLocation> retMap, int start, int end,JobDimension dim){
		
		int numTasks = jd[0] * jd[1] * jd[2];
		if(numTasks == 1) {
		    //base case; just assign single task to single node
		    retMap.put(this.num(taskCoords[start], dim), chosen_procs[start]);
		    return;
		}

		//identify the longest dimension
		int longestDim = 0;
		if(jd[1] > jd[0])
			longestDim = 1;
		if(jd[2] > jd[longestDim])
			longestDim = 2;
		
		//split along longest dimension
		int[] first = new int[]{jd[0], jd[1], jd[2]};  //job dimensions for each part
		int[] second = new int[]{jd[0], jd[1], jd[2]};

		int halfDim = jd[longestDim] / 2;
		first[longestDim] = halfDim;
		second[longestDim] = jd[longestDim] - halfDim;
		
		//sort the part of processors and jobs array
		Comparator<MeshLocation> c = new CoordComparator(longestDim);
		Arrays.sort(chosen_procs, start, end, c);
		Arrays.sort(taskCoords, start, end, c);

		//recursive calls to map each part
		int pivot = first[0]*first[1]*first[2] + start;
		mesh_help(chosen_procs, taskCoords, first, retMap, start, pivot, dim);
		mesh_help(chosen_procs, taskCoords, second, retMap, pivot, end, dim);
	}
}
