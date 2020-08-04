/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Represents a job arrival in the event queue
 */

package simulator;

import java.util.PriorityQueue;

import simulator.allocator.Allocator;
import simulator.scheduler.Scheduler;

public class ArrivalEvent extends Event {

	private boolean debug =false;
	private Job arrivingJob;

	public ArrivalEvent(Job j) {
		super(j.getArrivalTime());
		arrivingJob = j;
	}
	
	/*
	 * constructor to duplicate arrival events at different times 
	 * used for calculating relaxed fair start time
	 */
	public ArrivalEvent(Job j, long time){ 
		super(time);
		arrivingJob = j;
	}

	public int type() {
		return Event.ARRIVAL;
	}

	public void happen(Machine mach, Allocator alloc, Scheduler sched,
			PriorityQueue<Event> events, Statistics stats) {
		sched.jobArrives(arrivingJob, time);
		stats.jobArrives(time);
		happenHelper(mach, alloc, sched, events, stats);
	}
	
	public Job getJob(){
		return this.arrivingJob;
	}

}
