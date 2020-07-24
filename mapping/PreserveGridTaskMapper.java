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

public class PreserveGridTaskMapper extends TaskMapper {
  
  //this comparator takes the coordinates of a node
  //and compares MeshLocations based on their L1
  //distance from that node
  private class PointComparator implements Comparator<MeshLocation> {

    int x;
    int y;
    int z;

    public PointComparator(int X, int Y, int Z) {
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
    
    orderJobDimensions(chosen_procs, dim);
      
    Comparator<MeshLocation> c = new PointComparator(meshMinX, meshMinY, meshMinZ);

    Arrays.sort(chosen_procs, c);
    
    int i = 0;      //current job processor number

    //as long as there are still MeshLocations in chosen_procs
    while(chosen_procs.length > 0) {
      //take the first MeshLocation in chosen_procs
      MeshLocation m = chosen_procs[0];

      //remove the first MeshLocation from chosen_procs
      MeshLocation[] temp = new MeshLocation[chosen_procs.length - 1];
      for(int k = 1; k < chosen_procs.length; k++)
        temp[k - 1] = chosen_procs[k];
      chosen_procs = temp;
      
      //add the processor number and the MeshLocation to
      //the mapping
      retMap.put(i, m);

      i++;

      //update the comparator to give priority to the coordinate
      //nearest the expected location of the next location
      MeshLocation next = this.loc(i, dim);
      c = new PointComparator(next.x + meshMinX, next.y + meshMinY, next.z + meshMinZ);
      Arrays.sort(chosen_procs, c);

      //increment i, stop mapping if size of job reached
      if(i == dim.x * dim.y * dim.z)
        break;

      //if no next processor available,
      //print out "Mapping Error" and stop mapping
      if(i == -1) {
        System.out.println("Mapping Error");
        break;
      }
    }

    return retMap;
  }

}
