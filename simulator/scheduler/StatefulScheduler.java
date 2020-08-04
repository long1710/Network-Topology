/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Implements schedulers that need to keep a plan for when jobs run in
 * the future.  Specific schedulers implemented:
 *
 * Conservative backfilling (only backfill if no job is
 * delayed past its reservation)
 *
 * Opportunistic backfilling compresses by moving jobs in the order of
 * a separate priority rather than following their order in the
 * tentative schedule.  Since the orders differ, it returns to the
 * beginning of the list of jobs each time it successfully reschedules
 * a job.  Since this can be bad for performance, it supports a
 * facility to bound the number of jobs it will move this way before
 * reverting to standard compression.
 *
 * ...
 */

package simulator.scheduler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import simulator.Event;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Pair;
import simulator.Statistics;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;

public class StatefulScheduler extends Scheduler implements CopyableScheduler {

	private int numProcs;  //total number of processors
	private int freeProcs; //number of currently-free processors

	private TreeSet<SchedChange> estSched;  //current schedule, stored as list of changes

	private static boolean debug = false;  //whether to include debug printouts
	private static int plans=0;

	//whether to record comparision between hp and conservative guarantees:
	private static final boolean hp_guarantees = false;

	private static boolean whatsRunning = false || hp_guarantees;  //whether to keep track of running jobs
	protected TreeSet<Job> running;

	//map of jobs to start events
	protected HashMap<Job, EstJobStart> jobToEvents;

	//determines how to react when a job finishes early
	protected Manager heart;

	//whether we're in a TimedRunScheduler:
	//(disables maintenance and checks since schedule will be rebuilt)
	private boolean insideTimedRun;

	private long eventsVisited;
	//number of events examined while placing jobs during entire run
	//(instrumentation to compare algorithm effectiveness)

	public String name;


	public void insideTimedRun() {
		insideTimedRun = true;
	}

	public abstract class Manager {
		/* A helper class for schedulers that keep state to allow them
		 * to be combined by abstracting out the differences into this
		 * abstract class.
		 */

		protected StatefulScheduler scheduler;//to get accesss to scheuler methods

		public void arrival(Job j, long time) {} //to add job to backfilling queue

		public void start(Job j, long time) {}  //to remove from backfilling queue

		public void tryToStart(long time) {} //called when tryToStart is called in stateful scheduler

		public void printPlan() {}  //for debuging, print any state
		//this holds

		public void onTimeFinish(Job j, long time) {}

		public void reset() {} //remove state when called

		public void done() {}

		abstract public Manager copy(Scheduler sched);
		
		public void earlyFinish(Job j, long time) {}
		//to deal with what happens when a job finishes early

		public void removeJob(Job j, long time) {}
		//called when a job is removed from the schedule

		public void compress(long time) {
			//compresses the schedule; removes and reads each job in running order

			if (StatefulScheduler.debug)
				System.out.println("Beginning compression");

			TreeSet<SchedChange> oldEstSched = scheduler.estSched;
			scheduler.estSched = new TreeSet<SchedChange>();

			//first pass; pick up unmatched ends (put ends with a match into a set to "mark" them)
			//this must be done first so they appear when jobs are added
			HashSet<Job> matchedEnds = new HashSet<Job>();  //set of jobs whose EstJobStart we've seen
			Iterator<SchedChange> it = oldEstSched.iterator();
			while (it.hasNext()) {
				SchedChange sc = it.next();
				if (sc instanceof EstJobStart) {
					matchedEnds.add(((EstJobStart)sc).getPartner().job);  //so we ignore its partner
				} else { //sc must be an EstJobEnd
					if (!matchedEnds.contains(sc.job)) {
						//checking if we already saw its partner
						//otherwise, it is the end of a running job so just copy it over
						scheduler.estSched.add(sc);
					}
				}
			}

			//second pass; add the jobs whose starts appeared
			it = oldEstSched.iterator();
			while (it.hasNext()) {
				SchedChange sc = it.next();
				if (sc instanceof EstJobStart) {
					//add this job to the schedule, making sure it starts no later
					if (StatefulScheduler.debug)
						System.out.println(time 
								+ ": attempting to compress");

					long scTime = sc.getTime();
					long newStartTime = scheduler.scheduleJob(sc.job, time);

					if (newStartTime > scTime)
						Main.error("Attempt to delay estimated start of " +
								sc.job);

					if (newStartTime < scTime) { //improved so count

						if (StatefulScheduler.debug)
							System.out.println(time + 
									": compression of job "
									+ sc.job + " successful");
					}
				}
			}
			if (StatefulScheduler.debug)
				System.out.println(time + ": compression finished.");
		}
	}


	/*
	 *  Conservative: Just compresses, no backfilling
	 */

	public class ConservativeManager extends Manager {

		public ConservativeManager(StatefulScheduler scheduler) {
			this.scheduler = scheduler;
		}

		public void earlyFinish(Job j, long time) {
			this.compress(time);  //just compress for normal conservative
		}

		public void removeJob(Job j, long time) {
			this.compress(time);
		}

		@Override
		public Manager copy(Scheduler sched) {
			ConservativeManager consManager = new ConservativeManager((StatefulScheduler)sched);
			return consManager;
		}
	}

	/*
	 * Opportunistic: backfilling and compression
	 * AKA less conservative and Prioritized compression
	 */
	public class OpportunisticManager extends Manager {

		protected TreeSet<Job> backfill;//backfilling queue

		protected int fillTimes;  //max # times to backfill (-1=infinite)

		private int[] numSBF;  //cell i = #times backfill succeeded i times
		
		private Comparator<Job> comp;

		public OpportunisticManager(StatefulScheduler scheduler, Comparator<Job> comp, int fillTimes) {
			this.scheduler = scheduler;
			this.backfill = new TreeSet<Job>(comp);
			this.fillTimes = fillTimes;
			numSBF = new int[fillTimes+1];
			for(int i=0; i<numSBF.length; i++) {
				numSBF[i] = 0;
			}
			this.comp=comp;
		}

		public void reset() {
			for(int i=0; i<numSBF.length; i++) {
				numSBF[i] = 0;
			}
			backfill.clear();
		}

		public void arrival(Job j, long time) {
			backfill.add(j);  //add job to backfilling queue
		}

		public void start(Job j, long time) {
			backfill.remove(j);  //remove job from queue
		}

		public void printPlan() { //print backfill
			boolean first = true;
			for (Job job: backfill) {
				if (first)
					System.out.println("    backfilling queue:");
				System.out.println("\t" + job);
				first = false;
			}
		}

		public void done() {
			for (int i = 0; i < fillTimes; i++) {
				int numTimes = numSBF[i];
				if (numTimes != 0)
					System.out.println("Backfilled Successfully " + i +
							" times in a row " + numTimes + " times");
			}
		}

		public void earlyFinish(Job j, long time) {
			//backfills and compresses as necessary

			if (StatefulScheduler.debug)
				System.out.println(time + ": " + j + " finished early, " +
						"starting backfilling");

			int times;
			boolean exit = true;
			if (fillTimes == 0) {
				compress(time);
				return;
			}
			for (times = 0; (fillTimes == -1) || (times < fillTimes); times++) {
				//only backfill a certain number of times
				for (Job filler: backfill) {//iterate through backfill queue
					EstJobStart oldStartTime = scheduler.jobToEvents.get(filler);//store old start time

					if (StatefulScheduler.debug)
						System.out.println(time + ": attempting to backfill "
								+ filler);

					//remove from current scheduler
					scheduler.estSched.remove(oldStartTime);
					scheduler.estSched.remove(oldStartTime.getPartner());

					if (StatefulScheduler.debug)
						scheduler.printPlan();

					long oldTime = oldStartTime.getTime();
					long newTime = oldTime;

					//try to backfill job
					newTime = scheduler.scheduleJob(filler, time);

					if (newTime > oldTime) { //not supposed to happen

						if(StatefulScheduler.debug) {
							System.out.println();
							scheduler.printPlan();
						}

						Main.error("Opportunistic Backfilling gave a new " +
								"reservation to " + filler + " that was later than " +
								"previous one, while backfilling: old:" + oldTime +
								" < new:" + newTime);
					}

					if(newTime < oldTime) {//backfill successful
						if(StatefulScheduler.debug)
							System.out.println(time + ": " + "successfully " +
									"backfilled " + filler +
									" to a new reservation of time "
									+ newTime + " from " + oldTime);
						exit = false;
						break;
					} else {
						if(StatefulScheduler.debug)
							System.out.println(time + ": backfill of " +
									filler + " unsuccessful");
						exit = true;
					}

				}
				if(exit)
					break;
			}

			if (StatefulScheduler.debug)
				System.out.println(time + ": finished backfilling");

			//record results
			if(fillTimes != -1)
				numSBF[times]++;

			if (!exit) //backfilling was cut off so we should compress
				compress(time);
		}

		@Override
		public Manager copy(Scheduler sched) {
			OpportunisticManager oppManager = new OpportunisticManager((StatefulScheduler)sched,comp,fillTimes);
			for (Job j: backfill) {
				oppManager.backfill.add(j);
			}
			for(int i=0; i<numSBF.length; i++) {
				oppManager.numSBF[i] = this.numSBF[i];
			}
			return oppManager;
		}
	}


	public class RestrictiveManager extends Manager {
		//Only backfills if can start job right away
		//AKA lazy and delayed compression

		protected TreeSet<Job> backfill; //list of Jobs that haven't started

		private int results;
		private Comparator<Job> comp;

		public RestrictiveManager(StatefulScheduler scheduler, Comparator<Job> comp) {
			this.scheduler = scheduler;
			backfill = new TreeSet<Job>(comp);
			results = 0;
			this.comp=comp;
		}

		public void reset() {
			results = 0;
			backfill.clear();
		}

		public void arrival(Job j, long time) {
			//remove new job from scheduler
			EstJobStart newJobStart = scheduler.jobToEvents.get(j);
			EstJobEnd newJobEnd = newJobStart.getPartner();
			scheduler.estSched.remove(newJobStart);
			scheduler.estSched.remove(newJobEnd);

			boolean moved = false;
			backfill.add(j);  //add arrived job to backfill list

			//compress(time);

			//stop hole from being filled by newly arrived job

			//other more effective methods go here
			long start = newJobStart.getTime();

			//check if any existing job can fill that spot
			for (Job job:backfill) {
				//remove from schedule

				EstJobStart oldStart = scheduler.jobToEvents.get(job);
				EstJobEnd oldEnd = oldStart.getPartner();
				if(!job.equals(j)){
					scheduler.estSched.remove(oldStart);
					scheduler.estSched.remove(oldEnd);
				}

				//check where would fit in schedule
				long newTime = scheduler.findTime(scheduler.estSched, job, time);

				//if can move it up
				if (newTime <= (start + j.getEstimatedRunningTime()) &&
						newTime != oldStart.getTime()) {

					scheduler.scheduleJob(job, time);
					if(job.equals(j))
						moved = true;
				} else {
					//else put back in old place
					if(!job.equals(j)){
						scheduler.estSched.add(oldStart);
						scheduler.estSched.add(oldEnd);
					}
				}
			}

			if(!moved)
				scheduler.scheduleJob(j, time); //reschedule new job
		}

		public void start(Job j, long time) {
			backfill.remove(j);  //remove started job from backfill list
		}

		public void tryToStart(long time) {
			fill(time);
		}

		public void printPlan() {
			//print out backfill list
			boolean first = true;
			for (Job job: backfill) {
				if (first)
					System.out.println("    backfilling queue:");
				System.out.println("\t" + job);
				first = false;
			}
		}

		public void done() {
			System.out.println("Backfilled " + results + " times");
		}

		public void earlyFinish(Job j, long time) {
			if (StatefulScheduler.debug)
				System.out.println(time + ": " + j + " finished early, " +
						"starting backfilling");
			fill(time);
		}

		private void fill(long time) {
			for (Job job: backfill) {
				//store old schedule changes
				EstJobStart OldStart = scheduler.jobToEvents.get(job);
				EstJobEnd OldEnd = OldStart.getPartner();

				//remove from schedule so self doesn't get in way
				scheduler.estSched.remove(OldStart);
				scheduler.estSched.remove(OldEnd);

				//check if readding to schedule allows Job to start now
				if (time == scheduler.findTime(scheduler.estSched, job, time)) {
					//if so, reschedule
					scheduler.scheduleJob(job, time);
					results++;
				} else {
					//otherwise put back in old place
					scheduler.estSched.add(OldStart);
					scheduler.estSched.add(OldEnd);
				}
			}

			if(StatefulScheduler.debug)
				System.out.println(time + ": finished backfilling");
		}

		@Override
		public Manager copy(Scheduler sched) {
			RestrictiveManager resManager = new RestrictiveManager((StatefulScheduler)sched,comp);
			resManager.results= this.results;
			for (Job j: backfill) {
				resManager.backfill.add(j);
			}
			
			return resManager;
		}
	}   //end of RestrictiveManager

	//Manager for even less conservative
	public class EvenLessManager extends Manager {

		protected TreeSet<SchedChange> guarantee;
		protected HashMap<Job,EstJobStart> guarJobToEvents;
		protected TreeSet<Job> backfill;
		protected int bftimes;

		public void deepCopy(TreeSet<SchedChange> from,TreeSet<SchedChange> to,HashMap<Job,EstJobStart> toJ) {
			to.clear();
			toJ.clear();
			for (SchedChange sc : from) {

				if (sc instanceof EstJobStart) {
					EstJobEnd je = new EstJobEnd(sc.getTime()+sc.job.getEstimatedRunningTime(),sc.job);
					EstJobStart js = new EstJobStart(sc.getTime(),sc.job,je);
					to.add(js);
					to.add(je);
					toJ.put(js.job,js);
				} else if (sc instanceof EstJobEnd) {
					if (!toJ.containsKey(sc)) {
						to.add(sc);
					}
				}
			}
		}

		public EvenLessManager(StatefulScheduler scheduler,Comparator<Job> comp,int bftimes) {
			this.scheduler = scheduler;
			backfill = new TreeSet<Job>(comp);
			guarantee = new TreeSet<SchedChange>();
			guarJobToEvents = new HashMap<Job,EstJobStart>();
			this.bftimes=bftimes;
			//debug=true;
			// scheduler.debug=true;
		}

		private void backfill(long time) {

			for (int i=0;i<bftimes;i++)
				for (Job job : backfill) {

					EstJobStart js = scheduler.jobToEvents.get(job);
					scheduler.estSched.remove(js);
					scheduler.estSched.remove(js.getPartner());
					scheduler.jobToEvents.remove(job);

					long old = js.getTime();

					long start = scheduler.findTime(scheduler.estSched,job,time);
					EstJobEnd je = new EstJobEnd(start+job.getEstimatedRunningTime(),job);
					EstJobStart js2 = new EstJobStart(start,job,je);
					scheduler.estSched.add(js2);
					scheduler.estSched.add(je);
					scheduler.jobToEvents.put(job,js2);

					if (start==time && old != time) {
						//We are trying to run something now, check if it would kill the guaranteed
						//schedule

						EstJobStart gjs = guarJobToEvents.get(job);
						EstJobEnd gje = gjs.getPartner();

						guarantee.remove(gjs);
						guarantee.remove(gje);

						//Try adding the new start time to the guarantee
						//schedule and check if it is broken.
						guarantee.add(je);
						guarantee.add(js2);

						int freeprocs = scheduler.freeProcs;
						//We keep track of the last time seen so that the order of
						//jobs at the same time does not cause a negative value
						long last = -1;
						boolean destroyed = false;
						String reason = "";
						for (SchedChange sc : guarantee) {
							//If this is a new time from the last time
							if (last!=sc.getTime() && freeprocs<0) {
								destroyed = true;
								reason = "Bad at point "+sc.getTime()+" |";
							}

							last = sc.getTime();
							freeprocs += sc.freeProcChange();
						}
						if (destroyed || freeprocs != scheduler.numProcs) {
							//The schedule is impossible.
							if (freeprocs<0)
								reason += "Negative procs at end |";
							if (freeprocs!=scheduler.numProcs)
								reason+=  "Not all procs freed";

							//if(debug){
							System.err.println("\n"+time+": backfilling of "+job
									+" destroys schedule ("+reason+")");
							//}

							//Use the estimated schedule instead.
							deepCopy(scheduler.estSched,guarantee,guarJobToEvents);
						} else {
							//Guaranteed schedule looks OK.
							//Remove the temporary changes.
							guarantee.remove(je);
							guarantee.remove(js2);
							guarantee.add(gje);
							guarantee.add(gjs);
						}
					}

					if (start < old) {
						//Backfilled to earlier time.
						if (debug) {
							System.out.println(time+": backfilled "+job
									+" to "+start+" from "+old);
						}
						break;
					} else if (start > old) {
						System.out.println(time+
								": Backfilling error, plan:");

						scheduler.printPlan();
						Main.error("ELC gave a worse start time to "+job
								+". Old: "+old+", New: "+start);
					} else {
						if (debug) {
							System.out.println(time
									+": Unable to backfill "+job);
						}
					}

				}

			this.compress(time);
		}

		public void arrival(Job j, long time) {
			long gtime = scheduler.findTime(guarantee,j,time);
			EstJobEnd je = new EstJobEnd(gtime+j.getEstimatedRunningTime(),j);
			EstJobStart js = new EstJobStart(gtime,j,je);
			guarantee.add(js);
			guarantee.add(je);
			guarJobToEvents.put(j,js);

			backfill.add(j);

			deepCopy(guarantee,scheduler.estSched,scheduler.jobToEvents);

			backfill(time);
		}

		public void start(Job j, long time) {

			//Remove the job's current guarantees and add a guaranteed end time.

			EstJobStart js = guarJobToEvents.get(j);
			guarJobToEvents.remove(j);
			guarantee.remove(js);
			guarantee.remove(js.getPartner());
			backfill.remove(j);

			guarantee.add(new EstJobEnd(time+j.getEstimatedRunningTime(),j));

			int freeprocs = scheduler.freeProcs;
			//We keep track of the last time seen so that the order of
			//jobs at the same time does not cause a negative value
			long last = -1;
			boolean destroyed = false;
			String reason = "";
			for (SchedChange sc : guarantee) {
				//If this is a new time from the last time
				if (last!=sc.getTime() && freeprocs<0) {
					destroyed = true;
					reason = "Bad at point "+sc.getTime()+" | ";
				}

				last = sc.getTime();
				freeprocs += sc.freeProcChange();
			}
			if (destroyed || freeprocs != scheduler.numProcs) {
				//The schedule is impossible.
				if (freeprocs<0)
					reason += "Negative procs at end | ";
				if (freeprocs!=scheduler.numProcs)
					reason+=  "Not all procs freed";

				//if(debug){
				System.out.println(time+": schedule found to be destroyed (uncaught) ("+reason+")");
				//}

				//Use the estimated schedule instead.
				deepCopy(scheduler.estSched,guarantee,guarJobToEvents);
			} else {
				//deepCopy(guarantee,scheduler.estSched,scheduler.jobToEvents);
			}

			//backfill(time);

			if (debug) {
				System.out.println(time+": Starting "+j+", finishing at "+(time+j.getEstimatedRunningTime()));
			}
		}

		public void tryToStart(long time) {

		}


		public void printPlan() {
			System.out.println("Guaranteed plan:\n");
			int free = scheduler.freeProcs;
			for (SchedChange sc : guarantee) {
				System.out.println("\t"+sc+", "+(free+=sc.freeProcChange()));
			}

		}

		public void done() {
		}

		public void earlyFinish(Job j, long time) {
			for (SchedChange sc : guarantee) {
				if (sc.job.equals(j)) {
					guarantee.remove(sc);
					break;
				}
			}

			deepCopy(guarantee,scheduler.estSched,scheduler.jobToEvents);
			this.backfill(time);
		}


		public void onTimeFinish(Job j, long time) {
			for (SchedChange sc : guarantee) {
				if (sc.job.equals(j)) {
					guarantee.remove(sc);
					break;
				}
			}
		}

		@Override
		public Manager copy(Scheduler sched) {
			throw new UnsupportedOperationException();
		}
	}

	//AKA explicit Hole Preservation
	public class MaxManager extends Manager {

		protected StatefulScheduler scheduler;
		protected TreeSet<Job> backfill;
		private int results;

		private FileWriter hp_guar_output;  //log file for hp_guarantees

		public MaxManager(StatefulScheduler scheduler, Comparator<Job> comp){
			this.scheduler = scheduler;
			this.backfill = new TreeSet<Job>(comp);
			results = 0;

			hp_guar_output = null;
			if(hp_guarantees) {
				try {
					hp_guar_output = new FileWriter("hp_guarantees");
				} catch(IOException ex) {
					Main.error("Unable to open file for HP guarantee comparison");
				}
			}
		}

		//to add job to backfilling queue
		public void arrival(Job j, long time) {

			//for hp_guarantees:
			Iterator<SchedChange> it;
			SchedChange sc;
			long cons_guarantee = 0;  //conservative guarantee for this job
			if(hp_guarantees) {
				//find the guarantee that conservative would have given j
				//start w/ empty schedule, add remaining part of running jobs, and then have other active jobs "arrive"
				StatefulScheduler cons = new StatefulScheduler(numProcs);
				it = estSched.iterator();
				TreeSet<Job> active = new TreeSet<Job>(new FIFOComparator());  //non-running active jobs
				while(it.hasNext()) {
					sc = it.next();
					if(sc instanceof EstJobEnd) {
						if(running.contains(sc.job)) {
							//make running jobs also running in cons
							cons.estSched.add(sc);
							cons.freeProcs -= sc.job.getProcsNeeded();
						} else {
							//add others to queue of jobs to arrive in cons
							active.add(sc.job);
						}
					}
				}

				//now add active but non-running jobs to con schedule
				for(Job ja : active)
					cons.jobArrives(ja, ja.getArrivalTime());

				cons.jobArrives(j, time);  //then add the new job

				//find j's estimated start time in cons; this is its guarantee
				it = cons.estSched.iterator();
				sc = it.next();
				while(!j.equals(sc.job))
					sc = it.next();
				if(!(sc instanceof EstJobStart))
					Main.ierror("First event for job " + j + " is not a start");
				cons_guarantee = sc.getTime();
			}

			//actually do the scheduling for HP
			this.backfill.add(j);
			this.backfill(time);

			if(hp_guarantees) {
				//look at estimated start of j; this is its guarantee in HP
				it = estSched.iterator();
				sc = it.next();
				while(!j.equals(sc.job))
					sc = it.next();
				if(!(sc instanceof EstJobStart))
					Main.ierror("First event for job " + j + " is not a start");
				long hp_guarantee = sc.getTime();

				try {
					hp_guar_output.write(time + "\t" + j.getProcsNeeded() +
							"\t" + (cons_guarantee) +
							"\t" + (hp_guarantee) + "\n");
					//					 "\t" + (cons_guarantee - time) +
					//					 "\t" + (hp_guarantee - time) + "\n");
				} catch(IOException ex) {
					Main.error("Unable to write HP guarantee");
				}
			}
		} 

		//to remove from backfilling queue
		public void start(Job j, long time) {
			this.backfill.remove(j);
		}

		//called when tryToStart is called in stateful scheduler
		public void tryToStart(long time) {
			this.backfill(time);
		}

		//for debuging, print any state
		public void printPlan() {
			//print out backfill list
			boolean first = true;
			for (Job job: backfill) {
				if (first)
					System.out.println("    backfilling queue:");
				System.out.println("\t" + job);
				first = false;
			}
		}  

		public void onTimeFinish(Job j, long time) {
			//does anything need to be done?
		}

		public void reset() {
			this.results=0;
			this.backfill.clear();
		} 

		public void done() {
			System.out.println("Backfilled " + results + " times");
			if(hp_guarantees) {
				try {
					hp_guar_output.close();
				} catch(IOException ex) {
					Main.error("Unable to close HP guarantee file");
				}
			}
		}

		//currently runs backfill alg, but maybe should compress
		public void earlyFinish(Job j, long time) {
			this.backfill(time);
		}

		public void removeJob(Job j, long time) {
			this.backfill.remove(j);
			this.backfill(time);
		}

		//compresses the schedule; removes and readds each job in running order
		public void compress(long time) {

			if (StatefulScheduler.debug)
				System.out.println("Beginning compression");

			TreeSet<SchedChange> oldEstSched = scheduler.estSched;
			scheduler.estSched = new TreeSet<SchedChange>();

			//first pass; pick up unmatched ends (null the job field in matched ends)
			//this must be done first so they appear when jobs are added

			Iterator<SchedChange> it = oldEstSched.iterator();
			while (it.hasNext()) {
				SchedChange sc = it.next();
				if (sc instanceof EstJobStart) {
					((EstJobStart)sc).getPartner().job = null;  //so we ignore its partner
				} else { //sc must be an EstJobEnd
					if (sc.job != null) {
						//if null, we already saw its partner
						//otherwise, it is the end of a running job so just copy it over
						scheduler.estSched.add(sc);
					}
				}
			}

			//second pass; add the jobs whose starts appeared
			it = oldEstSched.iterator();
			while (it.hasNext()) {
				SchedChange sc = it.next();
				if (sc instanceof EstJobStart) {
					//add this job to the schedule, making sure it starts no later
					if (StatefulScheduler.debug)
						System.out.println(time 
								+ ": attempting to compress");

					long scTime = sc.getTime();
					long newStartTime = scheduler.scheduleJob(sc.job, time);

					if (newStartTime > scTime)
						Main.error("Attempt to delay estimated start of " +
								sc.job);

					if (newStartTime < scTime) { //improved so count

						if (StatefulScheduler.debug)
							System.out.println(time + 
									": compression of job "
									+ sc.job + " successful");
					}
				}
			}
			if (StatefulScheduler.debug)
				System.out.println(time + ": compression finished.");
		}

		private void backfill(long time){
			for (Job job: this.backfill) {
				//store old schedule changes
				EstJobStart OldStart = jobToEvents.get(job);
				EstJobEnd OldEnd = OldStart.getPartner();

				//remove from schedule self doesn't get in way
				scheduler.estSched.remove(OldStart);
				scheduler.estSched.remove(OldEnd);
				long possTime = scheduler.findTime(scheduler.estSched,job,time);

				//check if re-adding to schedule allows Job to start now
				if (time == possTime || !this.isHole(possTime)) {
					//if so, reschedule
					scheduler.scheduleJob(job, time);
					results++;
				} else {
					//otherwise put back in old place
					scheduler.estSched.add(OldStart);
					scheduler.estSched.add(OldEnd);
				}
			}

			if (StatefulScheduler.debug)
				System.out.println(time + ": finished backfilling");
		}

		private boolean isHole(long time) {
			int free = this.scheduler.freeProcs;
			Iterator<SchedChange> it = this.scheduler.estSched.iterator();
			while(it.hasNext()){
				int newFree = free + it.next().freeProcChange();
				if(newFree > free)
					return true;
				free = newFree;
				/*
		if((free += it.next().freeProcChange()) < free)
		    return true;
				 */
			}
			return false;
		}

		@Override
		public Manager copy(Scheduler sched) {
			// TODO Auto-generated method stub
			return null;
		}
	}         //end of MaxManager

	

	public StatefulScheduler(int numProcs) {
		//takes number of processors in machine
		this.numProcs = freeProcs = numProcs;
		estSched = new TreeSet<SchedChange>();
		jobToEvents = new HashMap<Job, EstJobStart>();
		insideTimedRun = false;
		heart = new ConservativeManager(this);
		eventsVisited = 0;
		name="Default";
	}

	public StatefulScheduler(int numProcs,
			Comparator<Job> comp, int filltimes) {
		this.numProcs = freeProcs = numProcs;
		estSched = new TreeSet<SchedChange> ();
		jobToEvents = new HashMap<Job, EstJobStart>();
		insideTimedRun = false;
		heart = new OpportunisticManager(this, comp, filltimes);
		eventsVisited = 0;
	}

	public StatefulScheduler(int numProcs,
			Comparator<Job> comp) {
		this.numProcs = freeProcs = numProcs;
		estSched = new TreeSet<SchedChange> ();
		jobToEvents = new HashMap<Job, EstJobStart>();
		insideTimedRun = false;
		heart = new RestrictiveManager(this, comp);
		eventsVisited = 0;
	}

	public StatefulScheduler(int numProcs,
			Comparator<Job> comp, boolean maxMode) {
		this.numProcs = freeProcs = numProcs;
		estSched = new TreeSet<SchedChange>();
		jobToEvents = new HashMap<Job, EstJobStart>();
		insideTimedRun = false;
		heart = new MaxManager(this, comp);
		if(whatsRunning)
			running = new TreeSet<Job>();
		eventsVisited = 0;
	}

	public StatefulScheduler(int numProcs,
			Comparator<Job> comp, int filltimes,boolean dummy) {
		this.numProcs = freeProcs = numProcs;
		estSched = new TreeSet<SchedChange> ();
		jobToEvents = new HashMap<Job, EstJobStart>();
		insideTimedRun = false;
		heart = new EvenLessManager(this, comp, filltimes);
		eventsVisited = 0;
	}



	public static StatefulScheduler Make(ArrayList<String> params) {
		//conservative
		if (params.get(0).equals("cons")) {
			Factory.argsAtLeast(0, params);
			Factory.argsAtMost(0, params);
			return new StatefulScheduler(
					Main.getMachine().numFreeProcessors());
		}

		//opportunistic
		if (params.get(0).equals("opportunistic")) {
			Factory.argsAtLeast(1, params);
			Factory.argsAtMost(2, params);
			if(params.size()-1 == 1)  //if only 1 argument
				return new StatefulScheduler(
						Main.getMachine().numFreeProcessors(),
						Main.getComparatorFactory().CreateSimple(params.get(1)),
						-1);
			return new StatefulScheduler(
					Main.getMachine().numFreeProcessors(),
					Main.getComparatorFactory().CreateSimple(params.get(1)),
					Integer.parseInt(params.get(2)));
		}

		//restrictive
		if (params.get(0).equals("restric")) {
			Factory.argsAtLeast(1, params);
			Factory.argsAtMost(1, params);
			return new StatefulScheduler(
					Main.getMachine().numFreeProcessors(),
					Main.getComparatorFactory().CreateSimple(params.get(1)));
		}

		//Max
		if (params.get(0).equals("max")) {
			Factory.argsAtLeast(1, params);
			Factory.argsAtMost(1, params);
			return new StatefulScheduler(
					Main.getMachine().numFreeProcessors(),
					Main.getComparatorFactory().CreateSimple(params.get(1)),
					true);
		}


		//even less
		if (params.get(0).equals("elc")) {
			Factory.argsAtLeast(2, params);
			Factory.argsAtMost(2, params);
			return new StatefulScheduler(
					Main.getMachine().numFreeProcessors(),
					Main.getComparatorFactory().CreateSimple(params.get(1)),
					Integer.parseInt(params.get(2)),true);
		}

		Main.error("Conservative has received invalid input");
		return new StatefulScheduler(
				Main.getMachine().numFreeProcessors());
	}

	public static String getParamHelp() {
		return "[<comparator>,<fillTimes>]\n" +
				"\tcomparator: Comparator for backfilling (not for conservative)\n"+
				"\tfillTimes: maximum number of times to backfill (for opportunistic (optional) and elc)\n";
	}

	public void reset() {  //reset state so scheduler can be run on new input
		if(debug)
			System.out.println("Resetting stateful scheduler");
		heart.reset();
		estSched.clear();
		freeProcs = numProcs;
		eventsVisited = 0;
	}





	public void jobArrives(Job j, long time) {
		//called when j arrives; time is current time
		//find its place in the schedule; add appropriate events
		
		scheduleJob(j, time);

		heart.arrival(j, time);
	}

	private long scheduleJob(Job job, long time) {
		//helper function for jobArrives and jobFinishes
		//adds job to the schedule, starting no earlier than time

		long startTime;

		startTime = findTime(estSched, job, time);

		//put the job in the schedule, starting at startTime
		if (debug)
			System.out.println(time + ": Adding " + job + " to start at time "
					+ startTime);
		EstJobEnd endChange = new EstJobEnd(startTime+job.getEstimatedRunningTime(), job);
		EstJobStart startChange = new EstJobStart(startTime, job, endChange);
		estSched.add(startChange);
		estSched.add(endChange);


		jobToEvents.put(job, startChange);
		if (debug)
			printPlan();
		return startTime;
	}

	private long findTime(TreeSet<SchedChange> sched, Job job, long time) {
		//helper function for scheduleJob
		//finds time to add Job

		if (job.getEstimatedRunningTime() == 0)
			return zeroCase(sched, job, time);

		time = Math.max(time, job.getArrivalTime());

		PeekableIterator<SchedChange> it = new PeekableIterator<SchedChange>(sched.iterator());
		//to traverse schedule
		int currentFree = freeProcs;  //number of procs free at time being considered
		boolean done = false;    //whether we've found working anchor point
		long anchorTime = time;  //anchor point; possible starting time for job
		SchedChange sc = null;   //first change we haven't looked at yet

		while (!done) {
			//will exit because anchor point at end of schedule must work

			if (it.hasNext()) {
				sc = it.next();
				eventsVisited++;
				long scTime = sc.getTime();
				if (scTime <= anchorTime)
					currentFree += sc.freeProcChange();
				else {  //advanced to anchor point; now test it

					boolean skip = false;    //to check if current procs falls below required amount

					while (!done && (currentFree >= job.getProcsNeeded())) {
						//process this change and any occuring at the same time
						currentFree += sc.freeProcChange();

						if (sc.job.getEstimatedRunningTime() == 0
								&& currentFree < job.getProcsNeeded())
							skip = true;

						while (it.hasNext() && (it.peek().getTime() == scTime)) {
							sc = it.next();
							eventsVisited++;
							currentFree += sc.freeProcChange();
							if (sc.job.getEstimatedRunningTime() == 0
									&& currentFree < job.getProcsNeeded())
								skip = true;
						}

						if (skip) {
							if (it.hasNext() && anchorTime == scTime) {
								sc = it.next();
								eventsVisited++;
							} else
								currentFree -= sc.freeProcChange();
							scTime = sc.getTime();
							break;
						}


						//check if we've gotten to where the job would end
						if ((scTime >= anchorTime + job.getEstimatedRunningTime()) 
								|| !it.hasNext())//Max -- change to > ? 
										//change because number of procs irrelevent 
										//to completed jobs
										done = true;    //yes; use the anchor point
						else {
							sc = it.next(); //no; advance the time we're looking at
							eventsVisited++;
						}
						scTime = sc.getTime();
					}
					if (!done) {    //not enough procs; advance anchorTime
						anchorTime = sc.getTime();
						currentFree += sc.freeProcChange();
					}
				}
			} else {  //ran out of changes before anchor point so can use it
				if (currentFree != numProcs){
					System.out.println("\nfree: "+currentFree+" numProcs: "+numProcs);//Max
					printPlan();
					Main.error("Stateful scheduler got to end of estimated schedule w/o all processors being free");
				}
				done = true;
			}
		}

		return anchorTime;
	}

	public long zeroCase(TreeSet<SchedChange> sched, Job filler, long time) {
		//helper for findTime that handles jobs of length 0
		//iterate through event list to find first time where
		//there are enough avaiable procs

		//procs currently avaiable
		int avaProcs = freeProcs;

		long lookAtTime = time;//to keep track of time being checked

		if (avaProcs >= filler.getProcsNeeded()) {
			return time;
		}

		//to traverse schedule
		SchedChange sc;//event at current time
		Iterator<SchedChange> it = sched.iterator();

		while (it.hasNext()) {
			sc = it.next();
			eventsVisited++;
			if(lookAtTime > sc.getTime()) {
				printPlan();
				Main.error("Planned schedule has events in the past (before " + lookAtTime + ")");
			}
			lookAtTime = sc.getTime();
			avaProcs += sc.freeProcChange();

			if (avaProcs >= filler.getProcsNeeded()) {  //enough procs to run right away
				return lookAtTime;
			}
		}

		if (avaProcs != numProcs){
			printPlan();
			Main.error("Stateful scheduler got to end of estimated schedule w/o all processors being free (" + avaProcs + " free)");
		}
		return lookAtTime;
	}

	public void jobFinishes(Job j, long time) {
		//called when j finishes; time is current time

		if(debug)
			System.out.println(time + ": job " + j + " finishes");

		//locate change where this job ends
		Iterator<SchedChange> it = estSched.iterator();
		boolean foundIt = false;  //whether we've found the change
		while(it.hasNext() && !foundIt) {
			SchedChange sc = it.next();
			long scTime = sc.getTime();
			if (scTime < time) {
				
				if(sc instanceof EstJobStart){
					System.out.println("(EstJobStart)ScTime: "+scTime +" Time: "+time);
				}else{
					System.out.println("(EstJobEnd)ScTime: "+scTime +" Time: "+time);
				}
				
				
				printPlan();
				Main.error("Stateful scheduler expecting events in the past");
			}
			if (j.equals(sc.job)) {
				if (sc instanceof EstJobStart)
					Main.error(j + " finished before conservative scheduler started it");
				foundIt = true;
				freeProcs += j.getProcsNeeded();
				it.remove();
				jobToEvents.remove(j);
				if(whatsRunning){
					running.remove(j);
				}
				if (time == scTime) {
					if (debug)
						System.out.println(time + ": " + sc.job + " finishes on time; no compression");
					heart.onTimeFinish(j,time);
					return;  //job ended exactly as scheduled so no compression
				}
			}
		}

		if (!foundIt) {
			printPlan();
			System.err.print("Currently running: ");
			for(Job job : running)
				System.err.print(job + " ");
			System.err.println();
			Main.error(j + " ended w/o stateful " +
					"scheduler knowing it was running");
		}

		//job ended early so need to compress the schedule
		//create new schedule data structure and add jobs to it

		//call heart of conservative's finish early method
		heart.earlyFinish(j,time);
	}

	public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
			PriorityQueue<Event> events, Statistics stats) {
		//allows the scheduler to start a job if desired; time is current time
		//called after calls to jobArrives and jobFinishes
		//(either after each call or after each call occuring at same time)
		//returns first job to start, null if none
		//(if not null, should call tryToStart again)

		heart.tryToStart(time);
			
		if(!insideTimedRun) {
			if(mach.numFreeProcessors() != freeProcs) {
				Main.error(this.name+" Stateful scheduler has different number of free procs than machine (" + freeProcs + " != " + mach.numFreeProcessors() + ")");
			}
		}

		//start first job in schedule if possible
		Iterator<SchedChange> it = estSched.iterator();
		while (it.hasNext()) {
			SchedChange sc = it.next();
			long scTime = sc.getTime();
			if(scTime < time) {
			
				printPlan();
				Main.error("Stateful scheduler expecting events in the past");
			}
			if(scTime > time){
				return null;  //don't want to allocate until later time
			}
			if(sc instanceof EstJobStart) {
				AllocInfo allocInfo = alloc.allocate(sc.job);
				if (allocInfo == null) {
					//not enough procs to start it now; wait for
					//  another job to end before this one can start
					return null;
				}
				it.remove();  //remove this event since it's occurring
				freeProcs -= sc.job.getProcsNeeded();
				jobToEvents.remove(sc.job);  //so no mapping for running jobs

				heart.start(sc.job, time);

				if (debug)
					System.out.println(time + ": Starting " + sc.job);
				sc.job.start(time, mach, allocInfo, events, stats);
				return allocInfo;
			}
		}
	
		return null;  //no estimated starting times encountered
	}

	public String getSetupInfo(boolean comment) {
		//print string describing class
		//preface each line with '#' if comment

		String preface;  //what to preface each line with
		if (comment)
			preface = "# ";
		else
			preface = "";

		if (heart instanceof ConservativeManager)
			return preface + "Conservative Scheduler";
		if (heart instanceof OpportunisticManager) {
			OpportunisticManager oHeart = (OpportunisticManager)heart;
			String secondLine = "\n" + preface + "\t" + oHeart.fillTimes;
			if(oHeart.fillTimes == -1)
				secondLine = "";
			return preface + "Opportunistic Scheduler\n" + preface +
					"\t" + oHeart.backfill.comparator().toString() +secondLine;
		}
		if (heart instanceof RestrictiveManager)
			return preface + "Restrictive Less Conservative Scheduler\n"
			+ preface + "\t" +
			((RestrictiveManager)heart).backfill.comparator().toString()
			+ "\n";
		return preface;
	}

	public void printPlan() {
		//for debugging; prints planned schedule to stdout
		boolean first = true;
		int procs = freeProcs;
				
		for (SchedChange sc : estSched) {
			procs += sc.freeProcChange();
			if (first)
				System.out.print("    \nplan:"+(plans++) +"\n");
			System.out.print("          ");
			System.out.println(sc + ", " +  procs + " free");
			first = false;
		}
		heart.printPlan();
	}

	//Prints out information once trace is done(called by main)
	public void done() {
		heart.done();
		System.out.println("Scheduler visited " + eventsVisited + " events");
	}

	public void removeJob(Job j, long time) {
		//as opposed to other schedulers, we need the current time

		Iterator<SchedChange> it = estSched.iterator();
		while (it.hasNext()) {
			SchedChange sc = it.next();
			if(sc.job.equals(j))
				it.remove();
		}

		if(!insideTimedRun)
			heart.removeJob(j, time);
	}

	public Iterator<Pair<Job,Long>> runningIterator() {
		//returns read-only iterator on running jobs (w/ estimated completion time)

		return new RunningIterator();
	}

	public Iterator<Job> estSchedIterator() {
		//returns read-only iterator on non-running scheduled jobs
		//(in order of scheduled start time)

		return new NonRunningIterator();
	}

	private class RunningIterator implements Iterator<Pair<Job,Long>> {

		private Iterator<SchedChange> it;  //iterates through estimated schedule
		private Pair<Job,Long> nextRetVal; //next value to return (null if none)

		public RunningIterator() {
			it = estSched.iterator();
			advance();
		}

		public void advance() {  //set nextRetVal to next running job
			nextRetVal = null;
			while (it.hasNext()) {
				SchedChange sc = it.next();
				if ((sc instanceof EstJobEnd) &&
						(!jobToEvents.containsKey(sc.job))) {
					nextRetVal = new Pair<Job,Long>(sc.job, sc.getTime());
					return;
				}
			}
		}

		public boolean hasNext() {
			return nextRetVal != null;
		}

		public Pair<Job,Long> next() {
			if (nextRetVal == null)
				throw new NoSuchElementException();

			Pair<Job,Long> retVal = nextRetVal;
			advance();
			return retVal;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private class NonRunningIterator implements Iterator<Job> {

		private Iterator<SchedChange> it;  //iterates through estimated schedule
		private Job nextRetVal;  //next value to return (null if none)

		public NonRunningIterator() {
			it = estSched.iterator();
			advance();
		}

		private void advance() {  //set nextRetVal to next non-running job
			nextRetVal = null;
			while (it.hasNext()) {
				SchedChange sc = it.next();
				if (sc instanceof EstJobStart) {
					nextRetVal = sc.job;
					return;
				}
			}
		}

		public boolean hasNext() {
			return nextRetVal != null;
		}

		public Job next() {
			if (nextRetVal == null)
				throw new NoSuchElementException();

			Job retVal = nextRetVal;
			advance();
			return retVal;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public int getFree(){
		return freeProcs;
	}

	public Scheduler contiguousAllocVersion(Allocator alloc) {
		return null;   //can't handle contiguous allocation
	}
	
	
	@Override
	public Scheduler copy() {
		StatefulScheduler duplicate;
		duplicate= new StatefulScheduler(this.numProcs);
		duplicate.numProcs =  this.numProcs;
		duplicate.freeProcs = this.freeProcs;
		duplicate.jobToEvents = new HashMap<Job, EstJobStart>();
		Set<Entry<Job, EstJobStart>> set = this.jobToEvents.entrySet();
		Iterator<Entry<Job, EstJobStart>> i = set.iterator();
		while(i.hasNext()){
			Entry<Job, EstJobStart> e = i.next();
			Job j = e.getKey();
			EstJobStart sc = e.getValue(); 
			duplicate.jobToEvents.put(j, sc);
		}
		duplicate.insideTimedRun = false;
		duplicate.eventsVisited = this.eventsVisited;
		duplicate.name="Cloned";
		Iterator<SchedChange> it = estSched.iterator();
		while (it.hasNext()) {
			SchedChange sc = it.next();
			duplicate.estSched.add(sc);
		}
		duplicate.heart = this.heart.copy(duplicate);
		return duplicate;
	}
	
	
	public boolean hasJobsWaiting(){
		boolean retVal= false;		
		for(SchedChange sc: estSched){
			if(sc instanceof EstJobStart)
				return true;
		}
		return retVal;
	}

}
