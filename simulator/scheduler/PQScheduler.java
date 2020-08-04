/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Schedulers based around priority queues; jobs run in order given by
 * some comparator without any backfilling.
 */

package simulator.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import simulator.Event;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Statistics;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;

public class PQScheduler extends Scheduler implements CopyableScheduler {

	protected PriorityQueue<Job> toRun;  //jobs waiting to run

	private String compSetupInfo;
	private Comparator<Job> comparator;

	public PQScheduler(Comparator<Job> comp) {
		//takes comparator to use for ordering the jobs

		toRun = new PriorityQueue<Job>(11, comp);
		compSetupInfo = comp.toString();
		this.comparator=comp;
	}
	
	public static PQScheduler Make(ArrayList<String> params) {
		Factory.argsAtLeast(0,params);
		Factory.argsAtMost(1,params);

		if(params.size()-1 == 0)
			return new PQScheduler(new FIFOComparator());
		else
			return new PQScheduler(Main.getComparatorFactory().CreateSimple(params.get(1)));
	}

	public static String getParamHelp(){
		return "[<opt_comp>]\n\topt_comp: Comparator to use, defaults to fifo";
	}
	
	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"Priority Queue Scheduler\n"+com+
				"Comparator: "+compSetupInfo;
	}

	public void jobArrives(Job j, long time) {
		//called when j arrives; time is current time
		//tryToStart should be called after announcing arriving jobs
		//  (either after each arrives or after all jobs arriving at a
		//   given time, depending on the value of
		//   Event.handleArrivalsSeparately)

		toRun.add(j);
		Object[] temp = toRun.toArray();
		toRun.clear();
		for(int i = 0; i < temp.length; i++){
			toRun.add((Job)temp[i]);
		}
	}

	public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
			PriorityQueue<Event> events, Statistics stats) {
		//allows the scheduler to start a job if desired; time is current time
		//called after calls to jobArrives and jobFinishes
		//(either after each call or after each call occuring at same time)
		//returns first job to start, null if none
		//(if not null, should call tryToStart again)

		if(toRun.size() == 0) 
			return null;

		AllocInfo allocInfo = null;
		Job job = toRun.peek();
		if (alloc.canAllocate(job)) {
			allocInfo = alloc.allocate(job);
		} 
		if(allocInfo != null) {
			toRun.poll();  //remove the job we just allocated
			job.start(time, mach, allocInfo, events, stats);
		}

		return allocInfo;
	}

	public void removeJob(Job j, long time) {
		toRun.remove(j);
	}

	public void reset() {
		toRun.clear();
	}

	@Override
	public Scheduler copy() {
		PQScheduler duplicate  = new PQScheduler(comparator);
		Iterator<Job> it = toRun.iterator();
		while (it.hasNext()) {
			Job j = it.next();
			duplicate.toRun.add(j);
		}
		return duplicate;
	}


	public boolean hasJobsWaiting() {
		Iterator<Job> it = toRun.iterator();
		while (it.hasNext()) {
			return true;			
		}
		return false;
	}

}
