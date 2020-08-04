/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Nominally responsible for generating log files, but most 
 * functionality moved to ActualStatistics
 * (needed for "side simulations" when computing Fair Start Times)
 */

package simulator;

import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;
import simulator.scheduler.Scheduler;

/* Format for time file (Tabs instead of commas) :
 * jobID, Arrival_Time, Start_Time, End_Time, Run_Time, Wait_Time, Total_Time
 *
 * ties file contains a line per allocation w/ >1 distinct allocation w/ same score
 *   (for NearestAllocator).  Format of line is #procs, tab, #ways it tied
 */

public class Statistics {

	static public String getOutputDirectory(){
		return null ;
	}
	
	public Statistics() {}

	 public Statistics(Machine mach, Scheduler sched, Allocator alloc,
			String name, boolean accurateEsts){}

	 static public void SetupLogs(String[] list){}

	/*
	Initialize the log file with specified extension.
	Only call this after an instance of Statistics has been created.
	 */
	 static private void InitializeLog(String extension){}

	 static public void writeTie(String message){}

	 static public boolean recordingTies(){
		return false;}

	 public void writeVisual(String message){}

	 static public void AppendToLog(String message, String extension){}

	/*
	 * Called after all events have occurred
	 */
	 public void done() {}

	//jobStarts() is called every time a job starts
	//Apart from recording the details of the job, it also
	//records when the particular job starts.
	 public void jobStarts(AllocInfo allocInfo, long time){}

	 public void jobFinishes(AllocInfo allocInfo, long time){}

	public void jobStopped(AllocInfo allocInfo, long time){}

	 private void jobFinishesOrStopped(AllocInfo allocInfo, long time){}

	//Method to write utilization statistics to file
	//Force it to write last entry by setting time = -1
	  private void writeUtil(long time){}

	
	//write time statistics to a file
	  private void writeTime(AllocInfo allocInfo, long time){}

	 private void writeWaiting(long time){}

	 private void writeAlloc(AllocInfo allocInfo){}

	 public void jobArrives(long time) {}
}

