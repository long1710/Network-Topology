/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Represents a job completion in the event queue
 */

package simulator;

import java.util.PriorityQueue;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;
import simulator.scheduler.Scheduler;

public class DepartureEvent extends Event {

	private AllocInfo allocationInfo;  //info on how job was allocated

	public DepartureEvent(long Time, AllocInfo allocInfo) {
		super(Time);
		allocationInfo = allocInfo;
	}

	public AllocInfo getAllocInfo() {
		return allocationInfo;
	}

	public int type() {  //returns code indicating type of event:
		return Event.DEPARTURE;
	}

	public void happen(Machine mach, Allocator alloc, Scheduler sched,
			PriorityQueue<Event> events, Statistics stats) {
		mach.deallocate(allocationInfo);
		alloc.deallocate(allocationInfo);
		stats.jobFinishes(allocationInfo, getTime());
		sched.jobFinishes(allocationInfo.job, getTime());
		happenHelper(mach, alloc, sched, events, stats);
	}
	
	public Job getJob(){
		return this.allocationInfo.job;
	}
}
