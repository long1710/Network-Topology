/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Comparator to make a PQScheduler that runs the job requiring the
 * fewest processors first
 */

package simulator.scheduler;

import java.util.Comparator;
import simulator.Job;

public class SmallestFirstComparator implements Comparator<Job> {
	public int compare(Job j1, Job j2) {
		//smaller job goes first if they are different size
		if(j1.getProcsNeeded() != j2.getProcsNeeded())
			return (j1.getProcsNeeded() - j2.getProcsNeeded());

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
		return (other instanceof SmallestFirstComparator);
	}

	public String toString(){
		return "SmallestFirstComparator";
	}
}

