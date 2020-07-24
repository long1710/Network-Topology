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

import java.util.*;
import simulator.MeshLocation;
import simulator.Main;

public class AllCornerTaskMapper extends TaskMapper {

    /**
     * Comparator based on L1 distance to a specific node
     */
    private class CornerComparator implements Comparator<MeshLocation> {
        private int x;  //coordinates of node
        private int y;
        private int z;

        public CornerComparator(int X, int Y, int Z) {
            this.x = X;
            this.y = Y;
            this.z = Z;
        }

        public int compare(MeshLocation loc, MeshLocation otherLoc){
            if(loc.L1DistanceTo(new MeshLocation(x, y, z)) - otherLoc.L1DistanceTo(new MeshLocation(x, y, z)) != 0)
                return loc.L1DistanceTo(new MeshLocation(x, y, z)) - otherLoc.L1DistanceTo(new MeshLocation(x, y, z));
            else if(loc.y != otherLoc.y)
                return loc.y - otherLoc.y;
            else if(loc.x != otherLoc.x)
                return loc.x - otherLoc.x;
            else
                return loc.z - otherLoc.z;
        }
    }

    public Map<Integer, MeshLocation> mesh_map(MeshLocation[] chosen_procs, JobDimension dim) {
    	Map<Integer, MeshLocation> retMap = new HashMap<Integer, MeshLocation>();

    	this.orderJobDimensions(chosen_procs, dim);  //rotate the job

    	//sets up 1 comparator for each corner
    	CornerComparator a = new CornerComparator(meshMinX, meshMinY, meshMinZ);
    	CornerComparator b = new CornerComparator(meshMinX, meshMaxY, meshMinZ);
    	CornerComparator c = new CornerComparator(meshMaxX, meshMaxY, meshMinZ);
    	CornerComparator d = new CornerComparator(meshMaxX, meshMinY, meshMinZ);
    	CornerComparator e = new CornerComparator(meshMinX, meshMinY, meshMaxZ);
    	CornerComparator f = new CornerComparator(meshMinX, meshMaxY, meshMaxZ);
    	CornerComparator g = new CornerComparator(meshMaxX, meshMaxY, meshMaxZ);
    	CornerComparator h = new CornerComparator(meshMaxX, meshMinY, meshMaxZ);

    	Arrays.sort(chosen_procs, a);

    	int i = 0;           //current processor number
    	int numAlloced = 0;  //# processors mapped so far
    	
    	//TreeMaps to store job MeshLocations (and their numbers) ordered by distance to a corner
    	TreeMap<MeshLocation, Integer> next = new TreeMap<MeshLocation, Integer>(a);
    	TreeMap<MeshLocation, Integer> next2 = new TreeMap<MeshLocation, Integer>(b);
    	TreeMap<MeshLocation, Integer> next3 = new TreeMap<MeshLocation, Integer>(d);
    	TreeMap<MeshLocation, Integer> next4 = new TreeMap<MeshLocation, Integer>(c);
    	TreeMap<MeshLocation, Integer> next5 = new TreeMap<MeshLocation, Integer>(e);
    	TreeMap<MeshLocation, Integer> next6 = new TreeMap<MeshLocation, Integer>(f);
    	TreeMap<MeshLocation, Integer> next7 = new TreeMap<MeshLocation, Integer>(h);
    	TreeMap<MeshLocation, Integer> next8 = new TreeMap<MeshLocation, Integer>(g);

    	//adding all MeshLocations to each next TreeMap
    	for(int q = 0; q < (dim.x * dim.y * dim.z); q++) {
    		next.put(this.loc(q, dim), new Integer(q));
    		next2.put(this.loc(q, dim), new Integer(q));
    		next3.put(this.loc(q, dim), new Integer(q));
    		next4.put(this.loc(q, dim), new Integer(q));
    		next5.put(this.loc(q, dim), new Integer(q));
    		next6.put(this.loc(q, dim), new Integer(q));
    		next7.put(this.loc(q, dim), new Integer(q));
    		next8.put(this.loc(q, dim), new Integer(q));
    	}

    	TreeSet<Integer> taken = new TreeSet<Integer>();      	//taken spots

    	//TODO: redo this loop to just keep track of the number to map
    	//TODO: could also find min rather than sort entire array...
    	while(chosen_procs.length > 0) {
    		MeshLocation m = chosen_procs[0];  //closest processor
    		MeshLocation[] temp = new MeshLocation[chosen_procs.length - 1];  //remove it
    		for(int k = 1; k < chosen_procs.length; k++)
    			temp[k - 1] = chosen_procs[k];
    		chosen_procs = temp;

    		retMap.put(i, m);  //add to mapping
    		taken.add(i);

    		//depending on how many processors have been
    		//mapped, choose which corner and get the next
    		//processor number using the appropriate next TreeMap,
    		//then sort according to distance from that corner
    		if((numAlloced % 8) == 7) {
    			i = this.getNext(next, taken);
    			Arrays.sort(chosen_procs, a);
    		} else if((numAlloced % 8) == 6) {
    			i = this.getNext(next5, taken);
    			Arrays.sort(chosen_procs, e);
    		} else if((numAlloced % 8) == 5) {
    			i = this.getNext(next4, taken);
    			Arrays.sort(chosen_procs, c);
    		} else if((numAlloced % 8) == 4) {
    			i = this.getNext(next6, taken);
    			Arrays.sort(chosen_procs, f);
    		} else if((numAlloced % 8) == 3) {
    			i = this.getNext(next3, taken);
    			Arrays.sort(chosen_procs, d);
    		} else if((numAlloced % 8) == 2) {
    			i = this.getNext(next7, taken);
    			Arrays.sort(chosen_procs, h);
    		} else if((numAlloced % 8) == 1) {
    			i = this.getNext(next2, taken);
    			Arrays.sort(chosen_procs, b);
    		} else if((numAlloced % 8) == 0) {
    			i = this.getNext(next8, taken);
    			Arrays.sort(chosen_procs, g);
    		}

    		numAlloced++;
    		if(numAlloced == dim.x * dim.y * dim.z)
    			break;

    		if(i == -1)  //no next processor available
    			Main.ierror("Mapping Error: no next node available for AllCornerTaskMapper");
    	}

    	return retMap;
    }

    //this method gets the next processor number
    //It takes a next TreeMap and the list of taken job processors
    public int getNext(TreeMap<MeshLocation, Integer> next, TreeSet<Integer> taken) {

        //remove all taken job processors from the next TreeMap
        while(taken.contains(next.get(next.firstKey()))) {
            next.remove(next.firstKey());
            if(next.isEmpty())
                break;
        }

        //if next TreeMap still contains any MeshLocations,
        //get the number associated with the first one in the
        //list, then remove that entry from the next TreeMap
        int i = -1;
        if(!next.isEmpty()) {
            i = next.get(next.firstKey());
            next.remove(next.firstKey());
        }
        return i;
    }
}
