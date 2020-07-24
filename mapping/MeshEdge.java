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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import simulator.MeshLocation;

public class MeshEdge {

  public MeshLocation loc1;
  public MeshLocation loc2;
  public int direction = -1;

  //A MeshEdge is an edge between two adjacent mesh nodes
  //MeshEdges keep track of their directionality, but
  //this is not taken into account when MeshEdges are compared
  //for equality. MeshLocations in a MeshEdge are required to
  //be adjacent, or an IllegalArgumentException is thrown.
  public MeshEdge(MeshLocation Loc1, MeshLocation Loc2) {
    loc1 = Loc1;
    loc2 = Loc2;
    if(loc1.L1DistanceTo(loc2) != 1) {
      System.out.println("Loc1: " + loc1 + " | Loc2: " + loc2);
      throw new IllegalArgumentException();
    }
    if((loc1.x == loc2.x) && (loc1.y == loc2.y)) {
      if(loc1.z < loc2.z)
        direction = 5;
      else if(loc1.z > loc2.z)
        direction = 4;
    } else if((loc1.y == loc2.y) && (loc1.z == loc2.z)) {
      if(loc1.x < loc2.x)
        direction = 3;
      else if(loc1.x > loc2.x)
        direction = 2;
    } else {
      if(loc1.y < loc2.y)
        direction = 1;
      else if(loc1.y > loc2.y)
        direction = 0;
    }
    if(direction == -1) {
      System.out.println("Direction not found");
      throw new IllegalArgumentException();
    }

  }

  public boolean equals(Object o) {
    if(o instanceof MeshEdge)
      return this.equals((MeshEdge) o);
    return false;
  }

  //if a MeshEdge contains the same two MeshLocations as another,
  //the two are considered equal by this equals method
  public boolean equals(MeshEdge other) {
    return ((this.loc1.equals(other.loc1) && this.loc2.equals(other.loc2))
        || (this.loc1.equals(other.loc2) && this.loc2.equals(other.loc1)));
  }

  //This method takes two MeshLocations, which do not need to be adjacent,
  //and determines all of the MeshEdges that lie on the Manhattan routes
  //between the two, assuming that all messages travel first in the
  //x direction, then in the y direction, then in the z direction
  public static Set<MeshEdge> edgesBetween(MeshLocation start, MeshLocation end) {
    Set<MeshEdge> retSet = new HashSet<MeshEdge>();
    if(start.x < end.x) {
      for(int i = start.x; i < end.x; i++) {
        retSet.add(new MeshEdge(new MeshLocation(i, start.y, start.z), new MeshLocation(i + 1, start.y, start.z)));
      }
      for(int i = end.x; i > start.x; i--) {
        retSet.add(new MeshEdge(new MeshLocation(i, end.y, end.z), new MeshLocation(i - 1, end.y, end.z)));
      }
    } else if(start.x > end.x) {
      for(int i = end.x; i < start.x; i++) {
        retSet.add(new MeshEdge(new MeshLocation(i, end.y, end.z), new MeshLocation(i + 1, end.y, end.z)));
      }
      for(int i = start.x; i > end.x; i--) {
        retSet.add(new MeshEdge(new MeshLocation(i, start.y, start.z), new MeshLocation(i - 1, start.y, start.z)));
      }
    }
    if(start.y < end.y) {
      for(int i = start.y; i < end.y; i++) {
        retSet.add(new MeshEdge(new MeshLocation(end.x, i, start.z), new MeshLocation(end.x, i + 1, start.z)));
      }
      for(int i = end.y; i > start.y; i--) {
        retSet.add(new MeshEdge(new MeshLocation(start.x, i, end.z), new MeshLocation(start.x, i - 1, end.z)));
      }
    } else if(start.y > end.y) {
      for(int i = end.y; i < start.y; i++) {
        retSet.add(new MeshEdge(new MeshLocation(start.x, i, end.z), new MeshLocation(start.x, i + 1, end.z)));
      }
      for(int i = start.y; i > end.y; i--) {
        retSet.add(new MeshEdge(new MeshLocation(end.x, i, start.z), new MeshLocation(end.x, i - 1, start.z)));
      }
    }
    if(start.z < end.z) {
      for(int i = start.z; i < end.z; i++) {
        retSet.add(new MeshEdge(new MeshLocation(end.x, end.y, i), new MeshLocation(end.x, end.y, i + 1)));
      }
      for(int i = end.z; i > start.z; i--) {
        retSet.add(new MeshEdge(new MeshLocation(start.x, start.y, i), new MeshLocation(start.x, start.y, i - 1)));
      }
    } else if(start.z > end.z) {
      for(int i = end.z; i < start.z; i++) {
        retSet.add(new MeshEdge(new MeshLocation(start.x, start.y, i), new MeshLocation(start.x, start.y, i + 1)));
      }
      for(int i = start.z; i > end.z; i--) {
        retSet.add(new MeshEdge(new MeshLocation(end.x, end.y, i), new MeshLocation(end.x, end.y, i - 1)));
      }
    }
    return retSet;
  }


  //Calculates how much this MeshEdge is used. Requires a mapping,
  //the mesh it was done on, the mapper it was done by, and the size
  //of the job mapped.
  public double usage(Map<Integer, MeshLocation> map, TaskMapper mppr, JobDimension dim) {
    double retVal = 0.0;
    TreeSet<Integer> taken = new TreeSet<Integer>();
    for(Map.Entry<Integer, MeshLocation> e : map.entrySet()) {
      int[] neighbors = mppr.neighbors(e.getKey(), dim);
      if(!taken.contains(e.getKey())) {
        //System.out.println("Proc not checked yet: " + e.getKey());
        for(int i = 0; i < neighbors.length; i++) {
          if((neighbors[i] != -1) && (!taken.contains(new Integer(neighbors[i])))) {
            //System.out.println("This neighbor (" + neighbors[i] + ") not checked yet");
            for(MeshEdge me : MeshEdge.edgesBetween(e.getValue(), map.get(new Integer(neighbors[i])))) {
              if(me.equals(this)) {
                int numDims = 0;
                if(e.getValue().x != map.get(new Integer(neighbors[i])).x)
                  numDims++;
                if(e.getValue().y != map.get(new Integer(neighbors[i])).y)
                  numDims++;
                if(e.getValue().z != map.get(new Integer(neighbors[i])).z)
                  numDims++;
                if(numDims > 1)
                  retVal += 0.5;
                else
                  retVal += 1.0;
                //System.out.println("Edge is used: " + me.toString());
              }
            }
          }
        }
        taken.add(e.getKey());
      }
    }
    return retVal;
  }

  //generates a string representation of this MeshEdge
  public String toString() {
    return loc1.toString() + " to " + loc2.toString();
  }

  //generates a hashcode for this MeshEdge
  public int hashCode() {
    return loc1.x + (loc1.y * 2) + (loc1.z * 3) + (loc2.x * 5) + (loc2.y * 7) + (loc2.z * 11);
  }
}
