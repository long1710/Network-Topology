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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import simulator.Event;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Statistics;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;

public class ScanScheduler extends Scheduler {
	private int[] category;     //category[i] is max processors for job in category i
	//category 0 is the smallest
	private Queue<Job>[] toRun; //jobs[i] is jobs in category i waiting to run
	private int nextQueue;      //index of next queue to use

	private boolean up;         //whether we traverse from smaller to larger jobs

	public ScanScheduler(int numProcs, int numBins, boolean up) {
		setup(numProcs, numBins, up);
	}

	public ScanScheduler(int numProcs, boolean up) {  //defaults to 4 bins
		setup(numProcs, 4, up);
	}

	private void setup(int numProcs, int numBins, boolean up) {
		//helper for constructors that does actual work

		//set up max size for each category
		if(numBins <= 0) {
			//set max sizes to be powers of two
			numBins = 0;
			for(int i=1; i<numProcs; i*=2)
				numBins++;
			numBins++;
			category = new int[numBins];
			int val = 1;
			for(int i=0; i<numBins; i++) {
				category[i] = val;
				val = val * 2;
			}
			category[numBins-1] = numProcs;
		} else {
			//set of max sizes to be evenly spaced
			category = new int[numBins];
			for(int i=0; i<numBins; i++)
				category[i] = (i+1)*numProcs/numBins;
		}

		//allocate queues for waiting jobs
		toRun = (Queue<Job>[]) new Queue[numBins];
		for(int i=0; i<numBins; i++)
			toRun[i] = (Queue<Job>) new LinkedList<Job>();

		//initialize other variables
		this.up = up;	
		if(up)
			nextQueue = 0;
		else
			nextQueue = numBins - 1;
	}

	public static ScanScheduler Make(ArrayList<String> params) {
		Factory.argsAtLeast(2,params);
		Factory.argsAtMost(3,params);

		if (params.size()-1 == 2) {
			return new ScanScheduler(Main.getMachine().numProcs(),
					Integer.parseInt(params.get(1)),
					Boolean.parseBoolean(params.get(2)));
		} else 
			return new ScanScheduler(Main.getMachine().numProcs(),
					Boolean.parseBoolean(params.get(1)));
	}

	public String getSetupInfo(boolean comment) {
		String com;
		if(comment)
			com="# ";
		else
			com="";

		if (up)
			return com+"ScanUp Scheduler (" + toRun.length + " queues)" ;
		else
			return com+"ScanDown Scheduler (" + toRun.length + " queues)" ;
	}

	public static String getParamHelp() {
		return "[<num_queues>,<true|false>] (true = ScanUp, false = ScanDown)\n";
	}

	public void jobArrives(Job j, long time) {
		int i = 0;
		while(category[i] < j.getProcsNeeded())  //place job into approprate queue
			i++;
		toRun[i].add(j);
	}

	public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
			PriorityQueue<Event> events, Statistics stats) {

		//advance nextQueue until it points to a nonempty category
		int numEmpty = 0;   //number of empty categories (to detect if no jobs waiting)
		while(toRun[nextQueue].isEmpty()) {
			numEmpty++;
			if(numEmpty == toRun.length)
				return null;

			//advance nextQueue in appropriate direction
			if(up)
				nextQueue = (nextQueue + 1) % toRun.length;
			else
				nextQueue = (nextQueue - 1 + toRun.length) % toRun.length;
		}

		AllocInfo allocInfo = null;
		Job job = toRun[nextQueue].peek();
		if(alloc.canAllocate(job)) {
			allocInfo = alloc.allocate(job);
			toRun[nextQueue].poll();  //remove the job we just allocated
			job.start(time, mach, allocInfo, events, stats);
		}
		return allocInfo;	
	}

	public void removeJob(Job j) {
		for(int i=0; i<toRun.length; i++)
			if(toRun[i].remove(j))
				return;                //exit once we find the job
	}

	public void reset() {              //clear all jobs
		for(int i=0; i<toRun.length; i++)
			toRun[i].clear();
	}

	@Override
	public boolean hasJobsWaiting() {
		// TODO Auto-generated method stub
		return false;
	}
}
