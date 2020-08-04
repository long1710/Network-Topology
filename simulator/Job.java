/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Data structure to store information on a job
 */

package simulator;

import java.util.PriorityQueue;
import java.util.Scanner;

import simulator.allocator.AllocInfo;
import simulator.scheduler.TimerEvent;

import mapping.TaskMapper;

public class Job implements Comparable<Job> { 

	protected long arrivalTime;        //when the job arrived
	protected int procsNeeded;         //how many processors it uses
	protected long actualRunningTime;  //how long it runs
	protected long estRunningTime;     //user estimated running time
	protected long startTime;	     //when the job started (-1 if not running)
	protected long strictFST;		//Strict Fair Start Time of the job
	protected long relaxedFST;		//Relaxed Fair Start Time of the job

	protected TaskMapper taskMap;

	protected long jobNum;             //ID number unique to this job
	private static long nextJobNum = 0;  //used setting jobNum

	public String toString() {
		return "Job #" + jobNum + " (" + arrivalTime + ", " + 
				procsNeeded + ", " + actualRunningTime + ", " +
				estRunningTime + ")";
	}

	public Job(Scanner input, boolean accurateEsts, TaskMapper map) {
		String line = input.nextLine();
		Scanner lineScanner = new Scanner(line);

		long arrivalTime = lineScanner.nextLong();
		int procsNeeded = lineScanner.nextInt();
		long actualRunningTime = lineScanner.nextLong();
		if(!accurateEsts && lineScanner.hasNextLong()) {
			long estRunningTime = lineScanner.nextLong();
			initialize(arrivalTime, procsNeeded, actualRunningTime,
					estRunningTime, map);
		} else
			initialize(arrivalTime, procsNeeded, actualRunningTime,
					actualRunningTime, map);
	}

	public Job(long ArrivalTime, int ProcsNeeded, long ActualRunningTime,
			long EstRunningTime){
		initialize(ArrivalTime, ProcsNeeded, ActualRunningTime, EstRunningTime, null);
	}

	protected Job() {
		//bogus constructor that creates uninitialized Job
		//so subclasses can do something before calling initialize themselves
	}

	protected void initialize(long ArrivalTime, int ProcsNeeded,
			long ActualRunningTime, long EstRunningTime, TaskMapper map) { 
		//helper for constructors

		arrivalTime = ArrivalTime;
		procsNeeded = ProcsNeeded;
		actualRunningTime = ActualRunningTime;
		estRunningTime = EstRunningTime;
		startTime = -1;
		taskMap = map;

		//make sure estimate is valid; workload log uses -1 for "no estimate"
		if(estRunningTime < actualRunningTime)
			estRunningTime = actualRunningTime;

		jobNum = nextJobNum;
		nextJobNum++;
	}

	protected Job(Job other, long endsIn) {
		//constructor to make short version of job so TimedRunScheduler can
		//   fool a StatefulScheduler base scheduler
		//creates a copy of other, but with est running time equal to endsIn

		arrivalTime = other.arrivalTime;
		procsNeeded = other.procsNeeded;
		actualRunningTime = other.actualRunningTime;
		estRunningTime = endsIn;
		startTime = -1;   //to ensure that it doesn't appear in statistics
		//	jobNum = other.jobNum;
		jobNum = nextJobNum;
		nextJobNum++;
		taskMap = other.taskMap;
	}
	
	public long getArrivalTime() {
		//used for output, but should not be used elsewhere
		return arrivalTime;
	}

	public long getStartTime() {
		if(startTime == -1) {
			System.err.println(this);
			throw new IllegalArgumentException();
		}
		return startTime;
	}

	public int getProcsNeeded() {
		return procsNeeded;
	}

	public long getJobNum() {
		return jobNum;
	}

	public int hashCode(){
		return (int) jobNum;
	}

	public long getActualRunningTime() {
		return actualRunningTime;
	}

	public long getEstimatedRunningTime() {
		return estRunningTime;
	}

	public TaskMapper getTaskMap() {
		return taskMap;
	}

	public long getEstimatedRunningTime(AllocInfo allocInfo){
		//returns estimated running time when allocation is taken into account
		long retVal = estRunningTime;
		return retVal;
	}

	public Event start(long time, Machine machine, AllocInfo allocInfo,
			PriorityQueue<Event> events, Statistics stats) {
		return this.start(time, machine, allocInfo, events, stats, -1);
	}

	public Event start(long time, Machine machine, AllocInfo allocInfo,
			PriorityQueue<Event> events, Statistics stats,
			long runFor) {
		//runFor is max time to run the job (-1 means until completion)
		//adds Timing or DepartureEvent to queue itself
		if(startTime != -1)
			return null;    //job already running
		
		//duration after effect of allocation:
		long realDuration = getActualTime(allocInfo);

		machine.allocate(allocInfo);
		
		startTime = time; 		
		stats.jobStarts(allocInfo, time);
		Event retVal;
		if((runFor == -1) || (runFor >= realDuration)) {
			retVal = new DepartureEvent(time+realDuration, allocInfo);
		} else {
			retVal = new TimerEvent(time+runFor, this, allocInfo);
		}

		events.add(retVal);
		return retVal;
	}
	
	public void stop() { //called when job is stopped before completion
		startTime = -1;
	}

	public long getActualTime() {
		return actualRunningTime;
	}

	public long getActualTime(AllocInfo allocInfo) {
		//returns running time when allocation is taken into account

		long retVal = actualRunningTime;
		return retVal;
	}

	public boolean equals(Object other) {
		if(other instanceof Job)
			return equals((Job)other);
		return false;
	}

	public boolean equals(Job other) {
		return (other != null) && (jobNum == other.jobNum);
	}

	public int compareTo(Job other) {
		return Utility.getSign(jobNum-other.jobNum);
	}
	

	public void resetStartTime() {
		startTime = -1;
	}
	
	public void setStrictFST(long time){
		this.strictFST = time;
	}

	public long getStrictFST() {
		return this.strictFST;
	}
	
	public void setRelaxedFST(long time){
		this.relaxedFST = time;
	}

	public long getRelaxedFST() {
		return this.relaxedFST;
	}

}
