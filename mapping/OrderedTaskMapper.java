/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**This class is an implementation of TaskMapper which iterates through
 * all natural numberings of an allocation, and also the transpose of
 * each numbering and returns the best mapping.
 */

package mapping;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import simulator.MeshLocation;

public class OrderedTaskMapper extends TaskMapper {
    
	private class Order implements Comparator<MeshLocation> {

		int first; //the index of the first dimension to be compared
		int second; //index of the second dimension to be compared
		int third; //third dimension
		int[] directions; //1 is right, up, or out. 0 is left, down, or in

		public Order(int first, int second, int[] directions) {
			this.first = first; //must be 0,1, or 2
			this.second = second; //0, 1, or 2
			this.third = 3-(first+second);
			this.directions = directions;
		}

		/**This compare method compares two MeshLocations according 
		 * to the current scheme. The "directions" array specifies
		 * whether the current dimension is going to the right (up, or out)
		 * or to the left (down, in). In the implementation, this is handled
		 * by 
		 */
		public int compare(MeshLocation m1, MeshLocation m2) {
			int[][] points = new int[][] {{m1.x, m1.y, m1.z},{m2.x,m2.y,m2.z}};

			int a1 = points[directions[first]][first];
			int a2 = points[(1+directions[first])%2][first];

			int b1 = points[directions[second]][second];
			int b2 = points[(1+directions[second])%2][second];

			int c1 = points[directions[third]][third];
			int c2 = points[(1+directions[third])%2][third];

			if(a1 == a2) {
				if(b1 == b2) {
					return c1 - c2;
				}
				return b1 - b2;
			}
			return a1 - a2;
		}

	}

    /**Takes an original mapping, and returns the transpose of that mapping. 
     * In effect, maps the job as if it is a y by x job, instead of x by y.
     * All neighborings are preserved when scoring the mapping.
     */
    //TODO: might move to TaskMapper
    public Map<Integer, MeshLocation>
    transpose(Map<Integer, MeshLocation> original, JobDimension dim) {
    	Map<Integer, MeshLocation> retMap = new HashMap<Integer, MeshLocation>();

    	int y = dim.y;
    	int mod = (dim.y*dim.x)-1;
    	int last = dim.totProcs()-1;

    	int n = 0;

    	for (int i=1; i < last; i++) {
    		n = (n+y)%mod;
    		retMap.put(n, original.get(i));
    	}

    	retMap.put(0, original.get(0)); 
    	retMap.put(last, original.get(last)); 

    	return retMap;
    }

    public Map<Integer, MeshLocation> 
    mapHelper(MeshLocation[] chosen_procs, 
    		JobDimension dim, Comparator<MeshLocation> order) {

    	Map<Integer, MeshLocation> retMap = new HashMap<Integer, MeshLocation>();

    	Arrays.sort(chosen_procs, order);

    	for (int i = 0; i<chosen_procs.length; i++) {
    		retMap.put(i, chosen_procs[i]);
    		//System.out.println(i+"->"+chosen_procs[i]);
    	}

    	return retMap;
    }

    public Map<Integer, MeshLocation> mesh_map(MeshLocation[] chosen_procs, JobDimension dim) {
    	
    	orderJobDimensions(chosen_procs, dim);

    	int bestScore = Integer.MAX_VALUE;
    	Map<Integer, MeshLocation> bestMap = new HashMap<Integer, MeshLocation>();
    	Map<Integer, MeshLocation> tempMap = new HashMap<Integer, MeshLocation>();
    	Map<Integer, MeshLocation> transMap = new HashMap<Integer, MeshLocation>();
    	int transScore;
    	int tempScore;
    	Comparator<MeshLocation> comp;
    	//This loop returns all possible comparators
    	for (int i=0; i<3; i++) {
    		int first = i; //Indicates the first dimension to be compared
    		for (int j=1; j<3; j++) {
    			int second = (i+j)%3; //ensures i!=j, assigns the second dimension
    			for (int x=0; x<2; x++) {
    				for (int y=0; y<2; y++ ) {
    					for (int z=0; z<2; z++) {
    						int[] dirs = new int[] {x,y,z};
    						//The order each dimension is compared
    						comp = new Order(first, second, dirs);
 
    						//Non transposed mapping
    						tempMap = mapHelper(chosen_procs, dim, comp);
    						tempScore = this.score(tempMap, dim);
    						//Transposed mapping
    						transMap = this.transpose(tempMap, dim);
    						transScore = this.score(transMap, dim);

    						//Finds the best mapping so far
    						if (tempScore < bestScore || transScore < bestScore) {
    							if (tempScore <= transScore) {
    								bestScore = tempScore;
    								bestMap = tempMap;
    							}
    							else {
    								bestScore = transScore;
    								bestMap = transMap;
    							}
    						}
    					}
    				}
    			}
    		}
    	} 
    	return bestMap;
    }
}
