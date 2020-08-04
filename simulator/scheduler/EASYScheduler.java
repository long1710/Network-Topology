/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Scheduler implementing EASY backfilling
 *
 * This version only works for non-contiguous allocators (so having
 * enough processors guarantees being able to allocate), but
 * EASYContigScheduler is similar and works for contiguous allocators.
 * 
 * Modified to allow orderings other than FCFS
 */

package simulator.scheduler;

import java.util.*;
import simulator.Event;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Statistics;
import simulator.Utility;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;
import simulator.allocator.ContiguousAllocator;
import simulator.allocator.PairTestableAllocator;

public class EASYScheduler extends Scheduler implements CopyableScheduler{

	//whether to print debug information:
	protected static final boolean DEBUG = false;

	protected class RunningInfo implements Comparable<RunningInfo> {
		//information on a running job
		//the comparison is based on estimated completion time

		public long jobNum;      //job number
		public int numProcs;     //its number of processors
		public AllocInfo ai;     //associated allocation info
		                         //(used by EASYContigScheduler)
		public long estComp;     //estimated completion time

		public int compareTo(RunningInfo other) {
			long diff = Utility.getSign(estComp - other.estComp);
			if(diff == 0)  //job number is tiebreaker so different jobs not equal
				return Utility.getSign(jobNum - other.jobNum);
			return Utility.getSign(diff);
		}
	}

	protected Collection<Job> toRun;
	//jobs waiting to run in order of arrival

	protected String comparator;
	//name of comparator used for list of jobs to run

	protected TreeSet<RunningInfo> running;
	//information on running jobs, ordered by non-decreasing estComp

	protected long prevFirstJobNum;  //ID of job to most-recently set guarantee
	protected long guaranteedStart;  //guaranteed start time for 1st job

	protected Machine machine;       //machine being scheduled

	protected boolean checkGuarantee;  //whether to check for guarantee violations
	private Comparator<Job> comp;

	public EASYScheduler(Machine machine) {
		//default constructor uses FCFS
		this.comp=new FIFOComparator();
		initialize(machine, new FIFOComparator());
	}

	public EASYScheduler(Machine machine, Comparator<Job> comp) {
		//constructor that takes comparator
		this.comp= comp;
		initialize(machine, comp);
	}

	private void initialize(Machine machine, Comparator<Job> comp) {
		//helper for constructors
		if(comp instanceof FIFOComparator)
			toRun = (Collection<Job>) new LinkedList<Job>();
		else
			toRun = (Collection<Job>) new TreeSet<Job>(comp);
		comparator = comp.toString();
		running = new TreeSet<RunningInfo>();
		this.machine = machine;
		prevFirstJobNum = -1;
		checkGuarantee = true;
		//guaranteedStart is not set
	}

	public static EASYScheduler Make(ArrayList<String> params) {
		Factory.argsAtLeast(0,params);
		Factory.argsAtMost(1,params);
	
		if(params.size()-1 == 0){
			return new EASYScheduler(Main.getMachine());
		} else { 
			return new EASYScheduler(Main.getMachine(),
					Main.getComparatorFactory().CreateSimple(params.get(1)));
		}
	}

	public static String getParamHelp() {
		return "[<opt_comp>]\n"+
		"\topt_comp: Optional comparator to order unbackfilled jobs; default is FCFS\n";
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"EASY Scheduler (" + comparator + ")" ;
	}

	public void disableGuaranteeChecking() {
		//disables error messages about guarantees being violated
		//used by TimedRunScheduler

		checkGuarantee = false;
	}

	public void jobArrives(Job j, long time) {
		//add j to waiting queue

		if(DEBUG)
			System.err.println(time + ": Job #" + j.getJobNum() + " arrives");

		toRun.add(j);
		Object[] temp = toRun.toArray();
		toRun.clear();
	        for(int i = 0; i<temp.length; i++){
	            toRun.add((Job)temp[i]);
		}

		Iterator<Job> tour = toRun.iterator();
		Job firstJob = tour.next();
		if(firstJob.getJobNum() == j.getJobNum())
			giveGuarantee(time);
	}

	public void jobFinishes(Job j, long time) {
		//remove j from list of running jobs and reestimate start of first job

		if(DEBUG)
			System.err.println(time + ": Job #" + j.getJobNum() + " completes");

		Iterator<RunningInfo> it = running.iterator();
		while(it.hasNext()) {
			RunningInfo fromList = it.next();
			if(fromList.jobNum == j.getJobNum()) {
				it.remove();
				giveGuarantee(time);
				return;
			}
		}
	}

	public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
			PriorityQueue<Event> events, Statistics stats) {
		//start first job or try to backfill
		
		if(toRun.size() == 0)
			return null;

		boolean succeeded = false;  //whether we found job to allocate
		boolean first = false;      //whether it was the first job

		//try to allocate first job
		Iterator<Job> it = toRun.iterator();
		Job job = it.next();
		if(checkGuarantee && time > guaranteedStart)	//violated promise to allocate this job
			Main.error("Failed to start job #" + job.getJobNum() +
			" at guaranteed time");
		if(alloc.canAllocate(job)) {   //try to allocate first job
			succeeded = true;
			first = true;
		}

		AllocInfo allocInfo = null;

		//try to allocate later jobs if first didn't work
		while(!succeeded && it.hasNext()) {
			job = it.next();
			if(alloc.canAllocate(job) &&
			   (allocInfo = doesntDisturbFirst(alloc, job, time)) != null) {
				succeeded = true;
			}
		}

		//allocate job if one was found
		if(succeeded) {
			if(DEBUG) {
				System.err.println(time + ": " + job + " starts");
			}

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
			System.err.print(time + ": giveGuarantee ( ");
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

		long lastGuarantee = guaranteedStart;  //guarantee at beginning
		boolean succeeded = false;             //whether made guarantee

		int size = firstJob.getProcsNeeded();  //size of first job

		int free = machine.numFreeProcessors();   //# free processors
		if(!checkGuarantee) {
			//find out if any processors are busy without known to be in use
			//(TimedRunScheduler causes this to happen)
			//want to give guarantees as if these processors become free
			int freeable = free;  //#processors free when everything finishes
			Iterator<RunningInfo> it = running.iterator();
			while(it.hasNext()) {
				RunningInfo info = it.next();
				freeable += info.numProcs;
			}
			free += (machine.numProcs() - freeable);
		}

		//if can run immediately, set guarantee
		
		if(free >= size) {
			guaranteedStart = time;
			succeeded = true;
		}

		//otherwise go through running until enough processors will be free
		int futureFree = free;   //# free processors at future times
		Iterator<RunningInfo> it = running.iterator();
		while(!succeeded && it.hasNext()) {
			RunningInfo info = it.next();
			futureFree += info.numProcs;
			if(futureFree >= size) {
				guaranteedStart = info.estComp;
				succeeded = true;
			}
		}
		
		if(succeeded) {
			if(DEBUG)
				System.err.println(time + ": Giving " + firstJob +
						" guarantee of time " + guaranteedStart);

			if(checkGuarantee &&
					(firstJob.getJobNum() == prevFirstJobNum) &&
					(lastGuarantee < guaranteedStart))
				Main.error("EASY scheduler gave new guarantee worse than previous one");
			prevFirstJobNum = firstJob.getJobNum();
		} else {
			//reached end of list without finding enough processors...
			Main.ierror("EASY unable to make reservation for 1st job (" +
					firstJob + ')');
		}
	}

	public void reset() {
		//clear scheduler state

		toRun.clear();
		running.clear();
	}

	public void removeJob(Job j, long time) {

		if(DEBUG)
			System.err.println(time + ": " + j + " removed");

		toRun.remove(j);       //in case j has not been started
		jobFinishes(j, time);  //in case it has
	}

	private AllocInfo doesntDisturbFirst(Allocator alloc, Job j, long time) {
		//returns whether j would delay the first job if started now
		if (!alloc.canAllocate(j))
			return null;

		AllocInfo retVal = alloc.allocate(j);

		if(time + j.getEstimatedRunningTime(retVal) <= guaranteedStart)
		    return retVal;

		int avail = machine.numFreeProcessors();  //count of processors available at guarantee
		Iterator<RunningInfo> it = running.iterator();
		RunningInfo ri;
		while(it.hasNext() && ((ri = it.next()).estComp <= guaranteedStart))
			avail += ri.numProcs;
		Job firstJob = toRun.iterator().next();
		if(avail - j.getProcsNeeded() >= firstJob.getProcsNeeded()){
			return retVal;
		}

		//if we made it this far, it disturbs the first job
		alloc.deallocate(retVal);  //so MBS can undo changes to its data structures
		return null;
	}

    public Scheduler contiguousAllocVersion(Allocator alloc) {
	if(alloc instanceof PairTestableAllocator)
	    return new EASYContigScheduler(machine, toRun, comparator, (ContiguousAllocator)alloc);
	else
	    Main.error("EASY can only use contiguous allocators that implement PairTestableAllocator");
	return null;    //never reaches this statement
    }

	@Override
	public Scheduler copy() {
		EASYScheduler duplicate = new EASYScheduler(Main.getMachine(), this.comp);
		Iterator<Job> it = toRun.iterator();
		while(it.hasNext()){
			Job j=it.next();
			duplicate.toRun.add(j);
		}
		duplicate.comparator = this.comparator;
		duplicate.running = new TreeSet<RunningInfo>();
		Iterator<RunningInfo> iterator = running.iterator();
		while(iterator.hasNext()){
			RunningInfo runInfo=iterator.next();
			duplicate.running.add(runInfo);
		}
		duplicate.guaranteedStart=this.guaranteedStart;
		duplicate.prevFirstJobNum = this.prevFirstJobNum;
		duplicate.checkGuarantee = this.checkGuarantee;
		return duplicate;
	}

	@Override
	public boolean hasJobsWaiting() {
		Iterator<Job> it = toRun.iterator();
		while (it.hasNext()) {
			return true;			
		}
		return false;
	}
}
