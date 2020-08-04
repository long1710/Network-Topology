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

import java.util.ArrayList;
import java.util.List;
import simulator.Mesh;
import simulator.MeshLocation;


public class IntersectionCenterGen extends CenterGenerator {
  public IntersectionCenterGen(Mesh m) {
    super(m);
  }

  public List<MeshLocation> getCenters(ArrayList<MeshLocation> available) { 
    List<MeshLocation> retVal = (List<MeshLocation>) new ArrayList<MeshLocation>();

    //Collect available X,Y and Z coordinates
    ArrayList<Integer> X = new ArrayList<Integer>();
    ArrayList<Integer> Y = new ArrayList<Integer>();
    ArrayList<Integer> Z = new ArrayList<Integer>();

    //Uses pre-defined contains, i.e obj1 == obj2 to skip duplicate int values
    for (MeshLocation loc : available) {
      if (!X.contains(loc.x))
        X.add(loc.x);
      if (!Y.contains(loc.y))
        Y.add(loc.y);
      if(!Z.contains(loc.z))
        Z.add(loc.z);
    }

    //Make all possible intersections of the X,Y and Z coordinates
    for (int ind_x = 0; ind_x < X.size(); ind_x++) //ind_x is x val index
      for (int ind_y = 0; ind_y < Y.size(); ind_y++) 
        for (int ind_z = 0; ind_z < Z.size(); ind_z++) {
          //Get an intersection with current x, y and z values
          MeshLocation val = new MeshLocation(	X.get(ind_x),
                                		Y.get(ind_y),
						Z.get(ind_z) );                                                                  
          retVal.add(val); //Add to the return value list
        }

    return retVal;
  }

    public String getSetupInfo(boolean comment){
        String com;
        if(comment) com="# ";
        else com="";
        return com+"IntersectionCenterGen";
    }
}
          

    
    
   
    
