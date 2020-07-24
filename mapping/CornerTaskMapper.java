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
import java.util.TreeMap;
import simulator.MeshLocation;

public class CornerTaskMapper extends TaskMapper {
  
  private class CornerComparator implements Comparator<MeshLocation> {
    public int compare(MeshLocation loc, MeshLocation otherLoc){
      if(loc.L1DistanceTo(new MeshLocation(0, 0, 0)) - otherLoc.L1DistanceTo(new MeshLocation(0, 0, 0)) != 0)
        return loc.L1DistanceTo(new MeshLocation(0, 0, 0)) - otherLoc.L1DistanceTo(new MeshLocation(0, 0, 0));
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

    CornerComparator c = new CornerComparator();
    Arrays.sort(chosen_procs, c);

    int i = 0;
    int numAlloced = 0;
    TreeMap<MeshLocation, Integer> next = new TreeMap<MeshLocation, Integer>(c);
    int[] neighbors = this.neighbors(0, dim);
    next.put(this.loc(neighbors[1], dim), new Integer(neighbors[1]));
    next.put(this.loc(neighbors[3], dim), new Integer(neighbors[3]));
    for (MeshLocation m: chosen_procs) {
      retMap.put(i, m);
      neighbors = this.neighbors(i, dim);
      if(neighbors[1] != -1)
        next.put(this.loc(neighbors[1], dim), new Integer(neighbors[1]));
      if(neighbors[3] != -1)
        next.put(this.loc(neighbors[3], dim), new Integer(neighbors[3]));
      if(!next.isEmpty()) {
        i = next.get(next.firstKey());
        next.remove(next.firstKey());
      }
      numAlloced++;
      if(numAlloced == dim.x * dim.y * dim.z)
        break;
    }

    return retMap;
  }
}
