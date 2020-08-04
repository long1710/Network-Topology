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
 * most processors first
 */

package simulator.scheduler;

import java.util.Comparator;

import simulator.Job;
import simulator.Main;

public class WidestFirstComparator implements Comparator<Job> {
    long currentTime = Main.getCurTime();
    public int compare(Job j1, Job j2) {
	long queuedTime1 = currentTime - j1.getArrivalTime();
	long queuedTime2 = currentTime - j1.getArrivalTime();
	long wallTime1 = queuedTime1 + j1.getEstimatedRunningTime();
	long wallTime2 = queuedTime2 + j2.getEstimatedRunningTime();
	long val1 = (queuedTime1 / wallTime1)^3 * j1.getProcsNeeded();
	long val2 = (queuedTime2 / wallTime2)^3 * j2.getProcsNeeded();
	return (int)(val2 - val1);
    }
    
    public boolean equals(Object other) {
	    return (other instanceof WidestFirstComparator);
    }

    public String toString(){
      return "LargestFirstComparator";
    }
}
