/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Comparator that favors jobs whose estimated time is the shortest.
 */

package simulator.scheduler;

import java.util.Comparator;
import simulator.Job;

public class ShortestFirstComparator implements Comparator<Job> {
  public int compare(Job j1, Job j2) {
    
    //shortest job goes first if different lengths
    if(j1.getEstimatedRunningTime() != j2.getEstimatedRunningTime())
      return (int) (j1.getEstimatedRunningTime() - j2.getEstimatedRunningTime());

    //secondary criteria: earliest arriving job first
    if(j1.getArrivalTime() != j2.getArrivalTime()) {
      Long L = new Long(j1.getArrivalTime());
      return L.compareTo(j2.getArrivalTime());
    }

    //break ties so different jobs are never equal:
    Long L = new Long(j1.getJobNum());
    return L.compareTo(j2.getJobNum());
  }

  public boolean equals(Object other) {
    return (other instanceof ShortestFirstComparator);
  }

  public String toString(){
    return "ShortestFirstComparator";
  }
}
