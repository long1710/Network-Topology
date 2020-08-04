/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Collect the points nearest to center by LInf distance
 *This point collector has been obsoleted by the new GreedyLInfPointCollector
 **/

package simulator.allocator;

import java.util.Arrays;
import simulator.MeshLocation;

public class LInfPointCollector extends PointCollector{

  public MeshLocation[] getNearest(MeshLocation center, int num,
				   MeshLocation[] available) {
    LInfComparator lic = new LInfComparator(center.x, center.y, center.z);
    Arrays.sort(available, lic);//LInfComparator sorts according to LInf dist from center
    return available;
  }

    public String getSetupInfo(boolean comment){
        String com;
        if(comment) com="# ";
        else com="";
        return com+"LInfPointCollector";
    }
 
}
