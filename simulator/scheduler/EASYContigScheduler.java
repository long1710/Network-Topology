/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Version of EASY scheduler that works for (some) contiguous
 * allocators.  Requires that it implements PairTestableAllocator.
 * The scheduler object is created by contiguousAllocVersion method in
 * EASYScheduler, which checks this condition.
 */

package simulator.scheduler;

import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;
import simulator.ContiguousJob;
import simulator.Event;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Mesh;
import simulator.Statistics;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;
import simulator.allocator.ContiguousAllocator;
import simulator.allocator.PairTestableAllocator;

public class EASYContigScheduler extends EASYScheduler {

    public EASYContigScheduler(Machine machine, Collection<Job> toRun,
			       String comparator, ContiguousAllocator alloc) {
	super(machine);
	this.toRun = toRun;
	this.comparator = comparator;
	theAlloc = alloc;
    }

    private ContiguousAllocator theAlloc;  //allocator being used

    public String getSetupInfo(boolean comment){
	String com = "";
	if(comment) com="# ";
	return com+"(Contiguous allocator version of) EASY Scheduler (" + comparator + ")" ;
    }

    public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
				PriorityQueue<Event> events, Statistics stats) {
	//start first job or try to backfill

	if(alloc != theAlloc)
	    Main.ierror("Allocator changed w/o telling EASY (contig)");

	if(toRun.size() == 0)
	    return null;

	boolean succeeded = false;  //whether we found job to allocate
	boolean first = false;      //whether it was the first job

	AllocInfo allocInfo = null;  //where to allocate the chosen job
	Mesh meshMach = (Mesh)mach;
	Mesh testbed = null;         //future machine state for testing if can start

	Iterator<Job> it = toRun.iterator();
	ContiguousJob job =
	    (ContiguousJob)it.next();  //job we're trying to allocate
	ContiguousJob firstJob = (ContiguousJob)job;  //1st job (w/ guarantee)

	if(checkGuarantee && time > guaranteedStart)	//violated promise to allocate this job
	    Main.error("Failed to start job " + job + 
		       " at guaranteed time " + guaranteedStart +
		       " (current time=" + time + ")");
	if(alloc.canAllocate(job)) {   //try to allocate first job
	    succeeded = true;
	    first = true;
	    if(DEBUG)
		System.err.println(time + ": starting first job: " + job);
	} else {
	    if(DEBUG)
		System.err.println(meshMach);

	    //prepare testbed machine
	    testbed = new Mesh(meshMach);
	    Iterator<RunningInfo> it2 = running.iterator();
	    boolean reachedGuarantee = false;
	    do {
		RunningInfo ri = it2.next();
		if(ri.estComp <= guaranteedStart) {
		    testbed.deallocate(ri.ai);
		} else
		    reachedGuarantee = true;
		if(!it2.hasNext())
		    reachedGuarantee = true;
	    } while(!reachedGuarantee);
	}

	//try to allocate later jobs if first didn't work
	PairTestableAllocator pta = (PairTestableAllocator) alloc;
	while(!succeeded && it.hasNext()) {
	    job = (ContiguousJob)it.next();
	    if(time + job.getEstimatedRunningTime() <= guaranteedStart)
		succeeded = alloc.canAllocate(job);
	    else {
		allocInfo = pta.pairTest(meshMach, job, testbed, firstJob);
		if(allocInfo != null)
		    succeeded = true;
	    }
	}

	//allocate job if one was found
	if(succeeded) {
	    if(DEBUG)
		System.err.println(time + ": " + job + " starts");
	    
	    if(allocInfo == null)
		allocInfo = alloc.allocate(job);
	    toRun.remove(job);

	    //update list of running jobs
	    RunningInfo started = new RunningInfo();
	    started.jobNum = job.getJobNum();
	    started.numProcs = job.getProcsNeeded();
	    started.ai = allocInfo;
	    started.estComp = time + job.getEstimatedRunningTime(allocInfo);
	    running.add(started);

	    job.start(time, mach, allocInfo, events, stats);

	    if(first)    //update guarantee when starting first job
	    	giveGuarantee(time);

	    if(DEBUG) {
	    	Iterator<RunningInfo> dit = running.iterator();
	    	System.err.print("Currently running jobs: ");
	    	while(dit.hasNext()) {
	    		RunningInfo ri = dit.next();
	    		System.err.print(ri.jobNum + " ");
	    	}
	    	System.err.println();
	    }
	    
	    return allocInfo;
	}

	return null;
    }

    protected void giveGuarantee(long time) {
	//takes current time
	//sets the time when the first job is guaranteed to run by

	if(DEBUG) {
	    System.err.print(time + ": ECS.giveGuarantee( ");
	    Iterator<Job> it2 = toRun.iterator();
	    while(it2.hasNext()) {
		Job job = it2.next();
		System.err.print(job.getJobNum() + " ");
	    }
	    System.err.println(")");
	}

	if(toRun.size() == 0)    //nothing to do if no jobs waiting
	    return;
	Job firstJob = toRun.iterator().next();
	Mesh testbed = new Mesh((Mesh)machine);

	long lastGuarantee = guaranteedStart;  //guarantee at beginning
	boolean succeeded = false;             //whether made guarantee

	//see if it can run now
	if(theAlloc.canAllocate(firstJob, testbed) != null) {
	    guaranteedStart = time;
	    succeeded = true;
	    if(DEBUG)
		System.err.println(testbed);
	}

	//if not, then look at future times
	Iterator<RunningInfo> it = running.iterator();
	while(!succeeded && it.hasNext()) {
	    RunningInfo info = it.next();
	    testbed.deallocate(info.ai);
	    if(DEBUG)
		System.err.println("Looking at after deallocating " + info.ai.job);
	    if(theAlloc.canAllocate(firstJob, testbed) != null) {
		guaranteedStart = info.estComp;
		succeeded = true;
		if(DEBUG)
		    System.err.println(testbed);
	    }
	}

	if(succeeded) {
	    if(DEBUG)
		System.err.println(time + ": Giving " + firstJob +
				   " guarantee of time " + guaranteedStart);
	    
	    if(checkGuarantee &&
	       (firstJob.getJobNum() == prevFirstJobNum) && (lastGuarantee < guaranteedStart))
		Main.ierror("EASY (contig) gave new guarantee worse than previous one for job " + firstJob);
	    prevFirstJobNum = firstJob.getJobNum();
	} else {
	    //reached end of list without finding enough processors...
	    Main.ierror("EASY (contig) unable to make reservation for 1st job (" +
			firstJob + ')');
	}
    }

    public Scheduler contiguousAllocVersion(Allocator alloc) {
	Main.ierror("contigousAllocVersion called on EASYContigScheduler");
	return null;
    }
}
