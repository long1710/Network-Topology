/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Scheduler that uses the most aggressive possible backfilling.
 * Keeps a priority-ordered list of jobs and tries them in that order
 * until it finds one that can start.  Note that this is NOT the EASY
 * scheduler, which is sometimes called "aggressive" in the
 * literature.
 */

package simulator.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.PriorityQueue;
import simulator.Event;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Statistics;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;

public class AggressiveScheduler extends Scheduler{
    private LinkedList<Job> toRun;  //List of waiting jobs
    private Comparator<Job> comp;   //Waiting jobs sorted by comp
    private int listIndex;	    //Current index in list toRun

    public AggressiveScheduler(int numProcs, Comparator<Job> comp) {
        toRun = new LinkedList<Job>();
        this.comp = comp;
        listIndex = 0;
    }

    public static AggressiveScheduler Make(ArrayList<String> params){
        Factory.argsAtLeast(0,params);
        Factory.argsAtMost(1,params);

        if(params.size()-1 == 1)
            return new AggressiveScheduler(
                    Main.getMachine().numFreeProcessors(),
                    Main.getComparatorFactory().CreateSimple(params.get(1)));
        else
            return new AggressiveScheduler(Main.getMachine().numFreeProcessors(),new FIFOComparator());	
    }

    public static String getParamHelp() {
        return "[<opt_comp>]\n"+
                "\topt_comp: Comparator to use, defaults to fifo";
    }

    public String getSetupInfo(boolean comment){
        String com;
        if(comment) com="# ";
        else com="";
        return com+"Aggressive Scheduler\n"+com+
                "\tComparator: "+comp.toString();
    }

    public void jobArrives(Job j, long time) {
        addCompJob(j, toRun);
        Object[] temp = toRun.toArray();
        toRun.clear();
        for(int i = 0; i < temp.length; i++){
            addCompJob((Job)temp[i], toRun);
        }

        addCompJob(j, toRun);		
    }

    public AllocInfo tryToStart(Allocator alloc, long time, Machine mach,
            PriorityQueue<Event> events, Statistics stats) {
        boolean succeeded = false;	    //Successful allocation
        Job job = toRun.peek();

        if (toRun.size() == 0)
            return null;

        ListIterator<Job> it = toRun.listIterator(listIndex);
        while (!succeeded && it.hasNext()  ) {
            job = it.next();
            if(alloc.canAllocate(job))	   //try to allocate a job
                succeeded = true; 
        }

        if (!succeeded)
            return null;

        AllocInfo allocInfo = alloc.allocate(job);  //Allocate job
        it.remove();			    //Remove allocated job

        listIndex = it.nextIndex();

        //actually start the job
        job.start(time, mach, allocInfo, events, stats);

        return allocInfo;
    }

    public void jobFinishes(Job j, long time) {
        listIndex = 0;
    }

    public void reset() {
        toRun.clear();
    }

    public void removeJob(Job j, long time) {
        toRun.remove(j);
    }

    //Adds job to list using the comparator comp
    private void addCompJob(Job j, LinkedList<Job> l) {
        //Empty list. Just add.
        if (l.size() == 0) {
            l.add(j);
            return;
        }

        int addLoc = 0;
        for (int i = 0; i < l.size(); i++) {
            Job inList = l.get(i);
            if (comp.compare(j, inList) > 0) {
                addLoc++;
            }
        }

        l.add(addLoc, j);
    }

	@Override
	public boolean hasJobsWaiting() {
		return !toRun.isEmpty();
	}

}



