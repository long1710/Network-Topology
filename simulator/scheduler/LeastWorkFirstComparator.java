/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Comparator that orders by increasing estimated service demand of
 * job (estimated running time * number of processors)
 */

package simulator.scheduler;

import java.util.Comparator;
import simulator.Job;
import simulator.Utility;

public class LeastWorkFirstComparator implements Comparator<Job> {

  public int compare(Job j1, Job j2) {
    
    long j1val = j1.getProcsNeeded()*j1.getEstimatedRunningTime();
    long j2val = j2.getProcsNeeded()*j2.getEstimatedRunningTime();
	
    if(j1val != j2val) {
	return Utility.getSign(j1val-j2val);
    }

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
    return (other instanceof LeastWorkFirstComparator);
  }

  public String toString(){
    return "ShortestServiceComparator";
  }
}
