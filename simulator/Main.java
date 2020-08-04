/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/* 
 * Main program for simulator.
 */

package simulator;

import java.io.File;
import java.util.*;
import simulator.allocator.*;
import simulator.fatTreeMachine.FatTree;
import simulator.fatTreeMachine.PollardAllocator;
import simulator.scheduler.*;
import mapping.*;

public class Main {

	//Print extra output so we know what is going on
	public static boolean reportProgress = false;
	
	//whether to use accurate estimates:
	private static boolean accurateEsts = false;
	private static boolean debug = false;
	
	private static boolean calcStrictFST = false;
	private static boolean calcRelaxedFST = false;
	private static Factory<Machine> machineFactory;
	private static Factory<Scheduler> schedulerFactory;
	private static Factory<Allocator> allocatorFactory;
	private static Factory<Comparator<Job>> comparatorFactory;
	private static Factory<TaskMapper> taskMapFactory;

	private static Machine machine;
	private static Scheduler scheduler;
	private static Allocator allocator;

	private static TaskMapper taskMap = null;

	//cover and uncoverMachine used so TimedRunScheduler can pass a
	//different machine to its base scheduler.  Would be better to
	//implement as stack of machines or allow the machine to be passed
	//into the factory
	public static void coverMachine(Machine m) {
		coveringMachine = m;
	}

	public static void uncoverMachine() {
		coveringMachine = null;
	}

	private static Machine coveringMachine = null;

	public static Machine getMachine() {
		if(coveringMachine != null)
			return coveringMachine;
		return machine;
	}

	public static Factory<Machine> getMachineFactory(){
		return machineFactory;
	}

	public static Factory<Scheduler> getSchedulerFactory(){
		return schedulerFactory;
	}

	public static Factory<Allocator> getAllocatorFactory(){
		return allocatorFactory;
	}

	public static Factory<Comparator<Job>> getComparatorFactory(){
		return comparatorFactory;
	}

	public static void error(String mesg) {  //announce an error and end program
		System.err.println("\nERROR: " + mesg);
		//	System.exit(1);
		throw new NullPointerException();
	}

	public static void ierror(String mesg) {
		//announce internal error and end program
		//ierrors denote mistaken programming assumptions rather than user error
		System.err.println("\nINTERNAL ERROR: " + mesg);
		//	System.exit(1);
		throw new NullPointerException();  //so we generate a stack trace
	}

	public static void warning(String mesg) {  //announces a warning; doesn't end program
		System.err.println("\nWARNING: " + mesg);
	}

	public static void usage() {

		System.out.println("Usage: java Main [machine=<MachineName>] [scheduler=<SchedulerName>]"+
				" [allocator=<AllocatorName>] trace=<file name>" +
				" [-accurate] [logs=<Logs>]\n");

		System.out.println("Machines:");
		System.out.println(machineFactory.getList(true,0));

		System.out.println("\nSchedulers:");
		System.out.println(schedulerFactory.getList(true,0));

		System.out.println("\nAllocators:");
		System.out.println(allocatorFactory.getList(true,0));

		System.out.println("\nComparators:");
		System.out.println(comparatorFactory.getList(false,0));

		System.out.println("\nTask Mappers:");
		System.out.println(taskMapFactory.getList(true,0));

		System.out.println("\nLogs:");
		System.out.println("util\ntime\nwait\nalloc\nties\nvisual\n");

		System.out.println("\nDefaults:\nMachine: simple[100]\nScheduler: pqueue[fifo]"+
				"\nAllocator: simple\nLogs: time");

		System.exit(0);
	}

	private static void initialize() {
		machineFactory = new Factory<Machine>();
		schedulerFactory = new Factory<Scheduler>();
		allocatorFactory = new Factory<Allocator>();
		comparatorFactory = new Factory<Comparator<Job>>();
		taskMapFactory = new Factory<TaskMapper>();

		machineFactory.registerClass("simple", SimpleMachine.class);
		machineFactory.registerClass("mesh", Mesh.class);
		machineFactory.registerClass("FatTree", FatTree.class);

		schedulerFactory.registerClass("aggressive", AggressiveScheduler.class);
		schedulerFactory.registerClass("easy", EASYScheduler.class);
		schedulerFactory.registerClass("cons", StatefulScheduler.class);
		schedulerFactory.registerClass("pqueue", PQScheduler.class);
		schedulerFactory.registerClass("timed", TimedRunScheduler.class);
		schedulerFactory.registerClass("opportunistic", StatefulScheduler.class);
		schedulerFactory.registerClass("restric", StatefulScheduler.class);
		schedulerFactory.registerClass("max", StatefulScheduler.class);
		schedulerFactory.registerClass("elc", StatefulScheduler.class);
		schedulerFactory.registerClass("scan", ScanScheduler.class);

		allocatorFactory.registerClass("linear-best", BestFitAllocator.class);
		allocatorFactory.registerClass("linear-first", FirstFitAllocator.class);
		allocatorFactory.registerClass("linear-sorted", SortedFreeListAllocator.class);
		allocatorFactory.registerClass("nearest", NearestAllocator.class);
		allocatorFactory.registerClass("genAlg", NearestAllocator.class);
		allocatorFactory.registerClass("MM", NearestAllocator.class);
		allocatorFactory.registerClass("MC1x1", NearestAllocator.class);
		allocatorFactory.registerClass("OldMC1x1", NearestAllocator.class);
		allocatorFactory.registerClass("random", RandomAllocator.class);
		allocatorFactory.registerClass("simple", SimpleAllocator.class);
		allocatorFactory.registerClass("MBS", MBSAllocator.class);
		allocatorFactory.registerClass("OctetMBS", OctetMBSAllocator.class);
		allocatorFactory.registerClass("GranularMBS", GranularMBSAllocator.class);
		allocatorFactory.registerClass("FirstFitContig", FirstFitContiguousAllocator.class);
		allocatorFactory.registerClass("MaxPeriphLengthAllocator", MPLAllocator.class);
		allocatorFactory.registerClass("rndFatTree", PollardAllocator.class);

		comparatorFactory.registerClass("fifo", FIFOComparator.class);
		comparatorFactory.registerClass("largefirst", LargestFirstComparator.class);
		comparatorFactory.registerClass("smallfirst", SmallestFirstComparator.class);

		comparatorFactory.registerClass("shortfirst", ShortestFirstComparator.class);

		comparatorFactory.registerClass("longfirst", LongestFirstComparator.class);
		comparatorFactory.registerClass("ssd", LeastWorkFirstComparator.class);
		comparatorFactory.registerClass("lsd", MostWorkFirstComparator.class);
		comparatorFactory.registerClass("wfp", WidestFirstComparator.class);

		taskMapFactory.registerClass("colMajor", ColumnMajorTaskMapper.class);
		taskMapFactory.registerClass("rowMajor", RowMajorTaskMapper.class);
		taskMapFactory.registerClass("corner", CornerTaskMapper.class);
		taskMapFactory.registerClass("allCorner", AllCornerTaskMapper.class);
		taskMapFactory.registerClass("preserveGrid", PreserveGridTaskMapper.class);
		taskMapFactory.registerClass("ordered", OrderedTaskMapper.class);
		taskMapFactory.registerClass("twoPG", TwoWayPGTaskMapper.class);
		taskMapFactory.registerClass("geom", GeometricTaskMapper.class);
	}

	//Parse the argument list to create objects for this simulation
	//Return name of the trace
	public static String parseArgs(String[] args) {
		List<String> arglist = Arrays.asList(args);

		String mach = "simple[100]";
		String alloc = "simple";
		String sched = "pqueue[fifo]";
		String trace = "";
		String map = "";

		for(int i=0; i<arglist.size(); i++) {
		    String s = arglist.get(i);
			if(s.startsWith("machine=")) {
				mach=s.substring(8);
			} else if(s.startsWith("scheduler=")) {
				sched=s.substring(10);
			} else if(s.startsWith("allocator=")) {
				alloc=s.substring(10);
			} else if(s.startsWith("trace=")) {
				trace=s.substring(6);
			} else if(s.startsWith("mapper=")) {
				map = s.substring(7);
			} else if(s.contains("logs=") && s.substring(0,5).equals("logs=")) {
				ActualStatistics.SetupLogs(s.substring(5).split(","));
			} else if(s.equals("-accurate")) {
				accurateEsts = true;
			} else {
				if(!s.equals("-h") && !s.equals("--help"))
					System.err.println("Unknown command line option "+s);
				usage();
			}
		}

		if(trace.equals("")) {
			Main.error("You must specify the input trace (trace=<trace name>). Use --help for usage");
		}

		System.out.println("#Machine: "+mach);
		System.out.println("#Scheduler: "+sched);
		System.out.println("#Allocator: "+alloc);

		
		machine = machineFactory.Create(mach);
		scheduler = schedulerFactory.Create(sched);
		allocator = allocatorFactory.Create(alloc);
		if(allocator instanceof ContiguousAllocator) {
			//make sure scheduler can handle contiguous allocation
			scheduler = scheduler.contiguousAllocVersion(allocator);
			if(scheduler == null)
				error("Cannot use scheduler \"" + sched + "\" with a contiguous allocator");
		}
		
		//TODO: If Fair Start Time is being computed, check (scheduler instanceof CopyableScheduler) and that machine is SimpleMachine
		
		if(!map.equals("")){
			System.out.println("#Task Mapper: " + map);
			taskMap = taskMapFactory.Create(map);
		}

		return trace;
	}

	public static Scanner getInputScanner(String traceFileName) {
		//returns Scanner from which to read input

		Scanner retVal = null;

		//first try to use SIMINPUTtraceFileName
		String inputBase = System.getenv("SIMINPUT");
		if(inputBase != null) {
			try {
				retVal = new Scanner(new File(inputBase+traceFileName));
				return retVal;
			} catch(java.io.FileNotFoundException exc) {
				//try without SIMINPUT (below)
			}
		}

		//that didn't work so try it without SIMINPUT
		try {
			retVal = new Scanner(new File(traceFileName));
		} catch(java.io.FileNotFoundException exc) {
			error("File not found: " + traceFileName);
		}
		return retVal;
	}

	public static void main(String[] args) {

		initialize();
		String traceFileName = parseArgs(args);  //name of trace

		PriorityQueue<Event> events = new PriorityQueue<Event>();

		ArrayList<Job> jobs = new ArrayList<Job>();

		if(reportProgress){
			System.err.println("Reading trace:");
		}

		Scanner input = getInputScanner(traceFileName);

		boolean jobsHaveDim = allocator instanceof ContiguousAllocator;	
		boolean sw=true; //Switch for progress reporting output
		int prnum = 0;
		while(input.hasNext()) {
			Job j = null;
			if(jobsHaveDim)
				j = new ContiguousJob(input, accurateEsts, taskMap);
			else 
				j = new Job(input, accurateEsts, taskMap);

			//check for invalid jobs
			if (j.getProcsNeeded() <= 0) {
				//System.out.println("line1 execute");
				Main.warning("Job " + j.getJobNum() + " requests " + j.getProcsNeeded() +
						" processors; ignoring it");
				continue;
			}
			if (j.getActualTime() < 0){  //time 0 also a bit strange, but perhaps rounded down
				//System.out.println("line 2 execute");
				Main.warning("Job " + j.getJobNum() + " has running time of " + j.getActualTime() +
						"; ignoring it");
				continue;
			}
			if(j.getProcsNeeded() > machine.numFreeProcessors()){
				//System.out.println("line 3 execute");
				Main.error("Job "+j.getJobNum()+" requires "+j.getProcsNeeded()+" processors but"+
						" only "+machine.numFreeProcessors()+" are in the machine");
			}

			if(jobsHaveDim) {
				//System.out.println("this work");
				ContiguousJob c = (ContiguousJob) j;
				Mesh mach = (Mesh) machine;

				if(c.getX() > mach.getXDim() 
						|| c.getY() > mach.getYDim()
						|| c.getZ() > mach.getZDim()) {
					Main.error("Job " + c.getJobNum() + " requires " +
							c.getX() + "x" + c.getY() + "x" + c.getZ() 
							+ " processors but the " + "machine has " 
							+ mach.getXDim() + "x" +
							mach.getXDim() + "x" +
							mach.getZDim() + " processors");
				}
			}
			//otherwise, the job is valid
			jobs.add(j);
			if(reportProgress && (prnum==0 || !input.hasNext())) {
				//print the current number of jobs that have been read.
				if(!sw){
					System.err.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
				} 
				sw=false;
				System.err.print("Jobs: ");
				String numJobsToPrint = "" + jobs.size();
				while(numJobsToPrint.length() < 8)
				    numJobsToPrint = " " + numJobsToPrint;
				System.err.print(numJobsToPrint);
				prnum=1000;
			}
			prnum--;
		}
		if(reportProgress)
			System.err.println("\nFinished.");
		input.close();

		machine.reset();
		scheduler.reset();
		events.clear();
		for(Job j : jobs) {
			ArrivalEvent ae = new ArrivalEvent(j);
			events.add(ae);
		}		
		
		runSim(machine, scheduler, allocator, events, traceFileName);
		System.out.println("simulation finish");
		allocator.done();
	}
	
	static long currentTime;

	public static long getCurTime(){
		return currentTime;
	}

	private static void estimateFST(Machine mach, Allocator alloc, Scheduler scheduler,PriorityQueue<Event> events,
			Event e, Statistics dummy, boolean relaxed, boolean beginWithStart,String traceName){
		SimpleMachine machine=new SimpleMachine(mach.numProcs());
		Main.coverMachine(machine);
		machine.numAvail= mach.numFreeProcessors();
		SimpleAllocator duplicateAlloc = new SimpleAllocator(machine);
		Scheduler sched =  ((CopyableScheduler)scheduler).copy();
		Statistics stats = new Statistics(mach, sched, alloc, traceName, accurateEsts);
		
		PriorityQueue<Event> duplicateEvents= new PriorityQueue<Event>();
		long currentTime=e.getTime();
		for(Event ev:events){
			if (ev instanceof DepartureEvent){
				duplicateEvents.add(ev);
			}
		}
		if(debug)
			System.out.println("Cloned Event called for job: "+e.getJob().getJobNum());
		PriorityQueue<Event> temp = new PriorityQueue<Event>(); //create a new queue to look at departureEvents
		LinkedList<Job> startedJobs = new LinkedList<Job>();  //list of jobs started inside simulation
		boolean wasInitiallyWaiting=sched.hasJobsWaiting();
		boolean alreadyAdded=false;
		if(beginWithStart){ //Start Jobs immediately
			AllocInfo allocInfo =null;
			do{
				 allocInfo = sched.tryToStart(duplicateAlloc, e.getTime(), machine, duplicateEvents, stats);
				 if(allocInfo != null){
						startedJobs.add(allocInfo.job);
					}
			}while(allocInfo != null);
			if(relaxed && wasInitiallyWaiting){
				if(!sched.hasJobsWaiting()){
					duplicateEvents.add(e);
					alreadyAdded=true;
				}
			}
		}
		
		if(!relaxed || !wasInitiallyWaiting){
			duplicateEvents.add(e);
			alreadyAdded=true;
		}
		temp = new PriorityQueue<Event>(); //create a new queue to look at departureEvents
		if(duplicateEvents.size()==0 && wasInitiallyWaiting && relaxed){
			sched.tryToStart(duplicateAlloc, e.getTime(), machine, duplicateEvents, stats);
			Event copied= duplicateEvents.peek();
			temp.add(copied);
			for(Event e3 : temp) {	
				if(!(e3 instanceof DepartureEvent)) // check to see if the event to be added is an departureEvent
					ierror("happen added a non-DepartureEvent");
				startedJobs.add(e3.getJob()); 
			}
			if(relaxed && wasInitiallyWaiting){
				if(!sched.hasJobsWaiting()){
					duplicateEvents.add(e);
					alreadyAdded=true;
				}
			}

		}
		
		while(duplicateEvents.size() > 0) {	
			Event e2 = duplicateEvents.poll();  //remove first event
			currentTime=e2.getTime();
			temp = new PriorityQueue<Event>(); //create a new queue to look at departureEvents
			Event copied = null;
			if((duplicateEvents.size() > 0) &&
					(duplicateEvents.peek().getTime() == e2.getTime())) { //check to see if it happens at the same time
				copied = duplicateEvents.peek();
				temp.add(copied);
			}
			e2.happen(machine, duplicateAlloc, sched, temp, stats);
			/*
			 * iterate through the temp queue to find the newly added departure event
			 * if we know that a new departure event has been added to the queue we 
			 * know that the job has been started hence we can add to the linkedlist of
			 * jobs that have started
			 */
			for(Event e3 : temp) {	
				if(e3 != copied) { 	//avoid the same copied event
					duplicateEvents.add(e3); //add the departure event
					if(!(e3 instanceof DepartureEvent)) // check to see if the event to be added is an departureEvent
						ierror("happen added a non-DepartureEvent");
					startedJobs.add(e3.getJob()); 
				}
			}	
			if(!sched.hasJobsWaiting() && !alreadyAdded && relaxed){
				ArrivalEvent ae = new ArrivalEvent(e.getJob(),currentTime);
				duplicateEvents.add(ae);
				alreadyAdded=true;
			}
		}
		
		if(relaxed){
			e.getJob().setRelaxedFST(e.getJob().getStartTime());
		}else{
			e.getJob().setStrictFST(e.getJob().getStartTime());
		}
		//reset the startTime of all the jobs that have been started in sideSimulation
		for(Job j : startedJobs){
			j.resetStartTime();
		}
		Main.uncoverMachine();
		if(debug)
			System.out.println("Cloned Event finished ");
	}
	
	private static void runSim(Machine mach, Scheduler sched, Allocator alloc,
			PriorityQueue<Event> events, String traceName)  {
		//System.out.println("sim start");
		ActualStatistics stats = new ActualStatistics(mach, sched, alloc, traceName, accurateEsts);
	
		if(reportProgress)
			System.err.println("Starting simulation:");
		long previousEventTime=-1;	
		boolean sw=true;
		int prnum=0;
		while(events.size() > 0) {
			//System.out.println("event check");
			Event e = events.poll();  //remove first event
			
			if (e instanceof ArrivalEvent && (calcStrictFST || calcRelaxedFST) ){			//check whether it is arrival or not
				if(calcStrictFST){					
					estimateFST(mach, alloc, sched, events, e, stats, false, false,traceName);	
				}

				if(calcRelaxedFST){	
					//Scheduler duplicate =  ((CopyableScheduler)sched).copy();
					//Statistics dummy = new Statistics(mach, duplicate, alloc, traceName, accurateEsts);
					estimateFST(mach, alloc, sched, events, e, stats, true,
							(previousEventTime==e.getTime() && !Event.handleArrivalsSeparately),traceName);	
				}
			}
			
			currentTime = e.getTime();
			previousEventTime=currentTime;
			
			e.happen(mach, alloc, sched, events, stats);
			
			if(reportProgress && prnum==0){
				if(!sw)
					System.err.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
				sw=false;
				System.err.print("Events left: ");
				String numEventsToPrint = "" + events.size();
				while(numEventsToPrint.length() < 10)
				    numEventsToPrint = " " + numEventsToPrint;
				System.err.print(numEventsToPrint);
				prnum=100;
			}
			prnum--;
		}

		if(reportProgress)
			System.err.println("\nFinished.");

		stats.done();
		sched.done();
	}
}
