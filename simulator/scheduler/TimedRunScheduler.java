/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * TimedRunScheduler schedules jobs that run within a given runTime first.
 * All long jobs are placed in a second list, and run after all jobs in the
 * first list have been run.
 *
 * So that the base scheduler acts as if extra jobs are running, it
 * uses a fake machine (called the base machine), which is a
 * SimpleMachine of the same size as the original machine.  The base
 * machine is updated so at all times its number of free processors is
 * the number available on the real machine plus those used by jobs
 * whose timer has expired (so a job uses processors on the base
 * machine during its trial run, but not once the trial run expires even
 * if the job continues running).
 *
 * Note that baseMachine is not maintained if the base scheduler is a
 * StatefulScheduler since it will be reset and reinitialized before
 * the scheduler is consulted.
 *
 * It also uses the CheatAllocator, which wraps the real allocator.
 * It decides whether a job can be allocated by ignoring the jobs
 * whose timers have expired.  It kills them if necessary when asked
 * to produce an actual allocation.
 *
 * TODO: the stateful scheduler part isn't finished...
 */

package simulator.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;
import simulator.DepartureEvent;
import simulator.Event;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Pair;
import simulator.SimpleMachine;
import simulator.Statistics;
import simulator.Utility;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;
import simulator.allocator.SimpleAllocator;

public class TimedRunScheduler extends TimingScheduler {

	private LinkedList<Job> toRun; // list of jobs waiting for a trial run
	private HashMap<Job, Long> running; // Jobs doing trial run (includes
										// expired)
										// jobs map to the end of their
										// scheduled trial run
										// (used for StatefulScheduler base
										// schedulers)

	private Scheduler sched; // base scheduler
	private Machine baseMachine; // machine the base scheduler sees

	private Comparator<Job> comp; // Order jobs in toRun
	private long runFor; // Length of trial runs
	private HashMap<Long, DepartureEvent> dept; // map from job # to departure
												// event
	private LinkedList<Job> timedJobList; // list of running jobs whose trial
											// runs expired

	private HashSet<Job> finishedJobs; // set of all jobs we've finished
	// only used when base scheduler is StatefulScheduler so we can cull jobs
	// we've finished

	private static final boolean debug = false;

	// Use a given comparator to order jobs
	public TimedRunScheduler(int runFor, Comparator<Job> comp, Machine m) {
		// m is base machine; must be SimpleMachine of size = to real machine
		initialize(runFor, comp, new PQScheduler(new FIFOComparator()), m);
	}

	// Use a given comparator to order jobs, with scheduler s for 2nd queue
	public TimedRunScheduler(int runFor, Comparator<Job> comp, Scheduler s,
			Machine m) {
		// m is base machine; must be SimpleMachine of size = to real machine

		initialize(runFor, comp, s, m);
	}

	private void initialize(int runFor, Comparator<Job> comp, Scheduler s,
			Machine m) {
		// helper for constructors
		// m is base machine; must be SimpleMachine of size = to real machine

		if (!(m instanceof SimpleMachine))
			Main.error("Base machine to TimedRunScheduler must be a SimpleMachine");

		this.runFor = runFor;
		this.comp = comp;
		toRun = new LinkedList<Job>();
		running = new HashMap<Job, Long>();
		sched = s;
		if (s instanceof EASYScheduler)
			((EASYScheduler) s).disableGuaranteeChecking();
		if (s instanceof StatefulScheduler) {
			((StatefulScheduler) s).insideTimedRun();
			finishedJobs = new HashSet<Job>();
		}
		baseMachine = m;
		dept = new HashMap<Long, DepartureEvent>();
		timedJobList = new LinkedList<Job>();
	}

	public String getSetupInfo(boolean comment) {
		String com;
		if (comment)
			com = "# ";
		else
			com = "";
		return com + "TimedRunScheduler with runFor=" + runFor + "\n" + com
				+ "\tComparator: " + comp.toString() + "\n" + com
				+ "\tBase scheduler: " + sched.getSetupInfo(comment);
	}

	public static TimedRunScheduler Make(ArrayList<String> params) {
		Factory.argsAtLeast(2, params);
		Factory.argsAtMost(3, params);

		Machine baseMachine = (Machine) new SimpleMachine(Main.getMachine()
				.numProcs());
		Main.coverMachine(baseMachine);
		Scheduler baseScheduler = Main.getSchedulerFactory().Create(
				params.get(1));
		Main.uncoverMachine();

		if (params.size() - 1 == 2)
			return new TimedRunScheduler(Integer.parseInt(params.get(2)),
					new FIFOComparator(), baseScheduler, baseMachine);
		else
			return new TimedRunScheduler(Integer.parseInt(params.get(2)), Main
					.getComparatorFactory().CreateSimple(params.get(3)),
					baseScheduler, baseMachine);
	}

	public static String getParamHelp() {
		return "[<base_scheduler>,<run_for>,<opt_comp>]\n"
				+ "\trun_for: Time to run for initially\n"
				+ "\topt_comp: Comparator to use, defaults to fifo";
	}

	private void jobStops(Machine mach, PriorityQueue<Event> e,
			Statistics stats, long jobNum) {
		// helper function called when a job is stopped

		DepartureEvent d = dept.get(jobNum);
		AllocInfo allocInfo = d.getAllocInfo();
		mach.deallocate(allocInfo);

		long time = allocInfo.job.getStartTime() + runFor;
		// TODO: only accurate if job stopped as soon as its timer expires
		
		stats.jobStopped(allocInfo, time);

		allocInfo.job.stop();

		e.remove(d); // Remove corresponding departure event

		running.remove(allocInfo.job);
	}

	public void jobFinishes(Job j, long time) {
		if (debug)
			System.err.println(time + ": TimedRun.jobFinishes " + j);

		running.remove(j);
		if (sched instanceof StatefulScheduler)
			finishedJobs.add(j);
		else {
			if (!timedJobList.remove(j))
				baseMachine.deallocate(new AllocInfo(j));
			// it's already been deallocated if the timer expired
		}

		sched.removeJob(j, time); // Remove from the base scheduler
	}

	public void jobArrives(Job j, long time) {
		if (debug)
			System.err.println(time + ": TimedRun.jobArrives " + j);

		Utility.addToSortedList(toRun, j, comp);
		sched.jobArrives(j, time);

		if (debug)
			System.err.println(time + ": " + j + " added to toRun");
	}

	public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
			PriorityQueue<Event> events, Statistics stats) {
		Iterator<Job> it = null;
		Job job = null;

		CheatAllocator c = new CheatAllocator(alloc, mach, events, stats, time);

		if (sched instanceof StatefulScheduler)
			rebuildStateful(time);

		// First try to start a trial run
		// Run the first job that can be allocated
		if (toRun.size() != 0) {
			it = toRun.iterator();
			while (it.hasNext()) {
				job = it.next();
				if (c.canAllocate(job)) { // try to allocate a job
					AllocInfo allocInfo = c.allocate(job);
					it.remove(); // no longer awaiting trial run

					running.put(
							job,
							time
									+ Math.min(runFor,
											job.getEstimatedRunningTime()));
					Event e = job.start(time, mach, allocInfo, events, stats,
							runFor);
					if (!(sched instanceof StatefulScheduler))
						baseMachine.allocate(allocInfo);

					// Even if job not expected to complete, add DepartureEvent
					// in case we don't stop it when timer expires
					if (e instanceof TimerEvent) {
						DepartureEvent d = new DepartureEvent(time
								+ job.getActualTime(), allocInfo);
						events.add(d);
						dept.put(job.getJobNum(), d);
					}

					if (debug)
						System.err.println(time
								+ ": TimedRun.tryToStart picks short job:"
								+ allocInfo.job);
					return allocInfo;
				}
			}
		}

		// No job in trial queue can run so let base scheduler try
		AllocInfo allocInfo = null;

		allocInfo = sched.tryToStart(c, time, mach, events, stats);
		if (allocInfo != null) {
			baseMachine.allocate(allocInfo);

			// since committed, make sure job not listed as on trial run
			timedJobList.remove(allocInfo.job);
			running.remove(allocInfo.job);
		}

		if (debug) {
			if (allocInfo == null)
				System.err
						.println(time + ": TimedRun.tryToStart picks nothing");
			else
				System.err.println(time
						+ ": TimedRun.tryToStart picks long job: "
						+ allocInfo.job);
		}

		return allocInfo;
	}

	public class ShortenedJob extends Job {
		// version of a job shortened since it has already been started

		public ShortenedJob(Job other, long endsIn) {
			super(other, endsIn);
		}

		public String toString() {
			return "Shortened" + super.toString();
		}
	}

	public class TrialJob extends Job {
		// version of a job shortened since it is on a trial run

		public TrialJob(Job other, long endsIn) {
			super(other, endsIn);
		}

		public String toString() {
			return "Trial" + super.toString();
		}
	}

	private void rebuildStateful(long currentTime) {
		// called when a trial run starts and base scheduler is a stateful
		// scheduler
		// rebuilds its estimated schedule
		// (by reseting scheduler, adding running jobs, then adding others in
		// order of plan

		StatefulScheduler ssched = (StatefulScheduler) sched;

		LinkedList<Job> jobs = new LinkedList<Job>(); // jobs to schedule in
														// order

		// put all the jobs currently on trial runs into jobs
		Set<Job> runningJobs = running.keySet();
		for (Job j : runningJobs)
			if (!timedJobList.contains(j))
				jobs.add(new TrialJob(j, running.get(j) - currentTime));

		// now copy the jobs from the scheduler's plan
		// begin with currently running jobs
		Iterator<Pair<Job, Long>> it1 = ssched.runningIterator();
		while (it1.hasNext()) {
			Pair<Job, Long> p = it1.next();
			if (!(p.getFirst() instanceof TrialJob)
					&& !finishedJobs.contains(p.getFirst()))
				jobs.add(new ShortenedJob(p.getFirst(), p.getSecond()
						- currentTime));
		}

		// now copy out the jobs that are planned, but not yet started
		Iterator<Job> it2 = ssched.estSchedIterator();
		while (it2.hasNext()) {
			Job j = it2.next();
			if (!finishedJobs.contains(j)
					&& ((j.getEstimatedRunningTime() > runFor) || !running
							.containsKey(j)))
				jobs.add(j); // don't give "long run" to short job in trial run
		}

		sched.reset(); // clean out old state
		baseMachine.reset();

		// now build up plan and start appropriate jobs
		while (!jobs.isEmpty()) {
			Job j = jobs.poll();
			sched.jobArrives(j, currentTime);
			if ((j instanceof ShortenedJob) || (j instanceof TrialJob)) {
				SimpleAllocator rebuildAlloc = new SimpleAllocator(
						(SimpleMachine) baseMachine);
				PriorityQueue<Event> rebuildEvents = new PriorityQueue<Event>();
				Statistics rebuildStats = new Statistics();
				AllocInfo ai = sched.tryToStart(rebuildAlloc, currentTime,
						baseMachine, rebuildEvents, rebuildStats);
				if ((ai == null) || (ai.job != j))
					Main.error("failed to start job " + j
							+ " when rebuilding schedule");
			}
		}

		if (debug)
			System.err.println("Done rebuilding");
	}

	public void timerExpires(Machine mach, Allocator alloc, Scheduler sched,
			PriorityQueue<Event> events, Statistics stats, Job j, long time) {
		if (debug)
			System.err.println(time + ": timer expires for " + j);

		timedJobList.add(j);
		if (!(sched instanceof StatefulScheduler))
			baseMachine.deallocate(new AllocInfo(j));
	}

	public class CheatAllocator extends Allocator {

		private Allocator ralloc; // The real allocator
		Machine mach;
		PriorityQueue<Event> events;
		Statistics stats;
		long currentTime;

		public CheatAllocator(Allocator alloc, Machine mach,
				PriorityQueue<Event> events, Statistics stats, long currentTime) {
			ralloc = alloc;
			this.mach = mach;
			this.events = events;
			this.stats = stats;
			this.currentTime = currentTime;
		}

		public boolean canAllocate(Job j) {
			boolean succeeded = false;

			if (timedJobList.contains(j)) {
				if (debug)
					System.err.println(currentTime
							+ ": CheatAllocator.canAllocate(" + j
							+ ") -> true (in timedJobList)");
				return true; // there is space for job that's already running
			}

			if (running.containsKey(j)) {
				if (debug)
					System.err.println(currentTime
							+ ": CheatAllocator.canAllocate(" + j
							+ ") -> false (in running)");
				return false;
			}

			// Check to see if we can allocate this job
			succeeded = ralloc.canAllocate(j);

			// If not, could it if we killed jobs whose trial run has expired?
			if ((timedJobList.size() != 0) && !succeeded) {
				int numToFree = 0;
				int canFree = 0; // number of processors contained in first i
									// jobs
				Iterator<Job> it = timedJobList.iterator();
				while (it.hasNext() && !succeeded) {
					numToFree++;
					Job job = it.next();
					canFree = canFree + job.getProcsNeeded();
					succeeded = (mach.numFreeProcessors() + canFree >= j
							.getProcsNeeded());
				}

				// If yes, then stop the selected jobs
				if (succeeded) {
					while (numToFree > 0) {
						Job job = timedJobList.poll(); // remove & return front
														// of list
						jobStops(mach, events, stats, job.getJobNum());
						numToFree--;
					}
				}
			}

			if (debug)
				System.err.println(currentTime
						+ ": CheatAllocator.canAllocate(" + j + ") -> "
						+ succeeded);

			return succeeded;
		}

		public AllocInfo allocate(Job job) {
			if (timedJobList.contains(job)) {
				// job already running; give it bogus allocation
				// (Job.start will ignore the command)

				return new AllocInfo(job);
			}

			if (running.containsKey(job))
				return null;

			if (!canAllocate(job)) // needed since it ends expired test runs
				return null;

			return ralloc.allocate(job);
		}

		public String getSetupInfo(boolean b) {
			return "Cheat allocator\n";
		}
	}

	@Override
	public boolean hasJobsWaiting() {
		// TODO Auto-generated method stub
		return false;
	}
}
