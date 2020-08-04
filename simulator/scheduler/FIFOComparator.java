/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Comparator to make a PQScheduler that runs jobs in FIFO order
 * (Could be done faster using a queue instead of a priority queue)
 */

package simulator.scheduler;

import java.util.Comparator;
import simulator.Job;

public class FIFOComparator implements Comparator<Job> {
    public int compare(Job j1, Job j2) {
	//earliest arriving job first
	if(j1.getArrivalTime() != j2.getArrivalTime()) {
	    Long L = new Long(j1.getArrivalTime());
	    return L.compareTo(j2.getArrivalTime());
	}
	
	//break ties so different jobs are never equal:
	Long L = new Long(j1.getJobNum());
	return L.compareTo(j2.getJobNum());
    }
    
    public boolean equals(Object other) {
	    return (other instanceof FIFOComparator);
    }

    public String toString(){
      return "FIFOComparator";
    }
}

