/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package simulator.scheduler;

import java.util.PriorityQueue;
import simulator.Event;
import simulator.HasSetupInfo;
import simulator.Job;
import simulator.Machine;
import simulator.Statistics;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;

public abstract class Scheduler implements HasSetupInfo {

	abstract public void jobArrives(Job j, long time);
	//called when j arrives; time is current time
	//tryToStart should be called after announcing arriving jobs
	//  (either after each arrives or after all jobs arriving at a
	//   given time, depending on the value of
	//   Event.handleArrivalsSeparately)

	public void jobFinishes(Job j, long time) {}
	//called when j finishes; time is current time
	//tryToStart should be called after announcing finished jobs
	//  (either after each finishes or after all jobs finishing at a
	//   given time, depending on the value of
	//   Event.handleArrivalsSeparately)

	abstract public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
			PriorityQueue<Event> events, Statistics stats);
	//allows the scheduler to start a job if desired; time is current time
	//called after calls to jobArrives and jobFinishes
	//(either after each call or after each call occuring at same time)
	//returns information on job it started or null if none
	//(if not null, should call tryToStart again)

	public void reset() {}
	//delete stored state so scheduler can be run on new input

	public void removeJob(Job j, long time) {
		//tell scheduler not to run j (which may already be running)
		//time is the current time
		//REQUIRED by TimedRunScheduler to run.

		throw new UnsupportedOperationException();
	}

	//so schedulers can print out information when simulation is done
	public void done() {};

	public Scheduler contiguousAllocVersion(Allocator alloc) {
		//returns version of scheduler that can deal with contiguous allocators
		//null if no such version exists

		return this;
	}

	abstract public boolean hasJobsWaiting();

}
