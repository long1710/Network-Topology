/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Responsible for generating log files
 */

package simulator;

/* Format for time file (Tabs instead of commas) :
 * jobID, Arrival_Time, Start_Time, End_Time, Run_Time, Wait_Time, Total_Time
 *
 * ties file contains a line per allocation w/ >1 distinct allocation w/ same score
 *   (for NearestAllocator).  Format of line is #procs, tab, #ways it tied
 */

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;
import simulator.allocator.MeshAllocInfo;
import simulator.scheduler.Scheduler;

public class ActualStatistics extends Statistics  {

	private static String name;	//trace name for the machine-used to create an output file, i.e "name.stat"
	private Machine machine;
	private long currentTime;
	private int procsUsed;	    //Processors used at the current time

	private boolean calcStrictFST=true;
	private boolean calcRelaxedFST=true;
	
	private static HashMap<String,FileWriter> fileHandles = null;

	private static boolean record_util   = false; //Record utilization
	private static boolean record_time   = true;  //Record times
	private static boolean record_wait   = false; //Record # waiting jobs
	private static boolean record_alloc  = false; //Record allocation info
	private static boolean record_ties   = false; //Record tie info
	private static boolean record_visual = false; //Record visual allocation info 

	private int lastUtil;       //last observed utilization value
	private long lastUtilTime;  //when it first reached this value (-1= no observations)

	//variables related to the number of waiting jobs:
	//used to print final number of waiting jobs after all events of each time
	private long lastWaitTime;  //time for which we next will record #waiting jobs
	private long lastWaitJobs;  //No. of waiting jobs last printed
	private int waitingJobs;	//current guess of what to record for that time
	private int tempWaiting;    //actual number of waiting jobs right now

	private static String fileHeader; //The commented out header for all log files

	static public String getOutputDirectory(){
		String outputDirectory=System.getenv("SIMOUTPUT");
		if(outputDirectory==null) {
			outputDirectory="./";
		}
		return outputDirectory;
	}

	public ActualStatistics() { 
		record_util = record_time = record_wait = record_alloc 
				= record_ties = record_visual = false;
	}

	public ActualStatistics(Machine mach, Scheduler sched, Allocator alloc,
			String name, boolean accurateEsts) {
		ActualStatistics.name = name.substring(Math.max(name.lastIndexOf("/")+1,0));	
		machine = mach;
		currentTime = 0;
		procsUsed = 0;
		tempWaiting = 0;
		lastUtil = 0;
		lastUtilTime = -1;
		lastWaitTime = -1;
		lastWaitJobs = -1;

		if(fileHandles == null){
			fileHandles = new HashMap<String,FileWriter>();
		}

		Date d = new Date();

		fileHeader=
				"# Simulation for trace "+name;
		if(accurateEsts)
			fileHeader += " (accurate estimates)";
		fileHeader += " started "+d+"\n"+
				"# [Machine] \n"+mach.getSetupInfo(true) + "\n";

		fileHeader +=
				"# [Scheduler] \n"+sched.getSetupInfo(true) + "\n"+
						"# [Allocator] \n"+alloc.getSetupInfo(true) + "\n";

		//Initialize logs
		if(record_time){
			ActualStatistics.InitializeLog("time");
			if(!calcStrictFST && !calcRelaxedFST){
				ActualStatistics.AppendToLog("\n# Job \tArrival\tStart\tEnd\tRun\tWait\tResp.\tProcs\n",
						"time");
			}else if(!calcRelaxedFST && calcStrictFST){
				ActualStatistics.AppendToLog("\n# Job \tArrival\tStart\tEnd\tRun\tWait\tResp.\tProcs\tStrictFST\n",
						"time");
			}else if(!calcStrictFST && calcRelaxedFST){
				ActualStatistics.AppendToLog("\n# Job \tArrival\tStart\tEnd\tRun\tWait\tResp.\tProcs\tRelaxedFST\n",
						"time");
			}else if(calcStrictFST && calcRelaxedFST ){
				ActualStatistics.AppendToLog("\n# Job \tArrival\tStart\tEnd\tRun\tWait\tResp.\tProcs\tS-FST\tR-FST\n",
						"time");
			}
		}
		if(record_util){
			ActualStatistics.InitializeLog("util");
			ActualStatistics.AppendToLog("\n# Time\tUtilization\n","util");
		}
		if(record_wait){
			ActualStatistics.InitializeLog("wait");
			ActualStatistics.AppendToLog("\n# Time\tWaiting Jobs\n","wait");
		}
		if(record_alloc){
			ActualStatistics.InitializeLog("alloc");
			ActualStatistics.AppendToLog("\n# Procs Needed\tActual Time\t Pairwise L1 Distance\n","alloc");
		}
		if(record_ties){
			ActualStatistics.InitializeLog("ties");
			ActualStatistics.AppendToLog("\n#Num procs\tways tied\n","ties");
		}
		if(record_visual && machine instanceof Mesh){
			Mesh mesh = (Mesh) machine;
			ActualStatistics.InitializeLog("visual");
			ActualStatistics.AppendToLog("MESH " + mesh.getXDim() + " " + mesh.getYDim() +
					" "+ mesh.getZDim()+"\n\n","visual");
		}
	}

	static public void SetupLogs(String[] list){
		record_wait=false;
		record_util=false;
		record_time=false;
		record_alloc=false;
		record_ties=false;
		record_visual=false;

		for(String s : list){
			if(s.equals("wait"))
				record_wait=true;
			else if(s.equals("util"))
				record_util=true;
			else if(s.equals("time"))
				record_time=true;
			else if(s.equals("alloc"))
				record_alloc=true;
			else if(s.equals("ties"))
				record_ties=true;
			else if(s.equals("visual"))
				record_visual=true;
		}
	}

	/*
	Initialize the log file with specified extension.
	Only call this after an instance of Statistics has been created.
	 */
	static private void InitializeLog(String extension){
		String outputDirectory=ActualStatistics.getOutputDirectory();

		try{
			FileWriter rec = new FileWriter(outputDirectory + name + "." + extension,false);
			ActualStatistics.fileHandles.put(extension,rec);
			rec.write(ActualStatistics.fileHeader);
		}
		catch(IOException e){
			Main.error("Unable to initialize log file " +
					outputDirectory + name + "." + extension);
		}
	}

	static public void writeTie(String message){
		if(record_ties){
			AppendToLog(message,"ties");
		}
	}

	static public boolean recordingTies() {  //whether we're recording #ties
		return record_ties;
	}

	public void writeVisual(String message){
		if(record_visual && machine instanceof Mesh){
			AppendToLog(message+"\n","visual");
		}
	}

	static public void AppendToLog(String message, String extension){
		String outputDirectory=ActualStatistics.getOutputDirectory();

		FileWriter rec = ActualStatistics.fileHandles.get(extension);

		try{
			if(rec == null)
				System.err.println("Uninitialized log: "+outputDirectory+name+"."+extension);
			rec.write(message);
		} catch(IOException e){
			System.err.println("ERROR WRITING TO "+outputDirectory+name+"."+extension);
		}

	}

	/*
	 * Called after all events have occurred
	 */
	public void done() {
		if(record_util)
			writeUtil(-1);

		if(record_wait)
			writeWaiting(-1);

		for(FileWriter fw : ActualStatistics.fileHandles.values()){
			try{
				fw.close();
			}
			catch(IOException e){
				System.err.println("Error closing log file");
			}
		}
	}

	//jobStarts() is called every time a job starts
	//Apart from recording the details of the job, it also
	//records when the particular job starts.
	public void jobStarts(AllocInfo allocInfo, long time) {
		if(machine instanceof Mesh) {
			if(record_alloc)
				writeAlloc(allocInfo);
			if(record_visual && machine instanceof Mesh)
				writeVisual("BEGIN "+allocInfo.job.getJobNum()+" "+allocInfo.getProcList(machine));
		}

		procsUsed += allocInfo.job.getProcsNeeded(); //Increment procs used
		if(record_util)
			writeUtil(time);

		this.tempWaiting--;
		if(record_wait)
			writeWaiting(time);

		currentTime = time;
	}

	public void jobFinishes(AllocInfo allocInfo, long time) {
		//called every time a job completes

		if(record_visual && machine instanceof Mesh)
			writeVisual("END "+allocInfo.job.getJobNum());

		if(record_time)
			writeTime(allocInfo, time);

		jobFinishesOrStopped(allocInfo, time);
	}

	public void jobStopped(AllocInfo allocInfo, long time) {
		//called whenever a job stops running w/o completing

		this.tempWaiting++;
		if(record_wait)
			writeWaiting(time);
		jobFinishesOrStopped(allocInfo, time);
	}

	private void jobFinishesOrStopped(AllocInfo allocInfo, long time) {
		//holds common code for jobFinishes and jobStopped
		//  (updates state for job stopping execution)

		procsUsed = procsUsed - allocInfo.job.getProcsNeeded();	//Update procs used

		if(record_util)        //Write processor utilization over time to file
			writeUtil(time);

		currentTime = time;   //Update current time
	}

	//Method to write utilization statistics to file
	//Force it to write last entry by setting time = -1
	private void writeUtil(long time) {
		if(lastUtilTime == -1) {  //if first observation, just remember it
			lastUtil = procsUsed;
			lastUtilTime = time;
			return;
		}
		if((procsUsed == lastUtil) && (time != -1))  
			return;  //don't record if utilization unchanged unless forced
		if(lastUtilTime == time) {  //update record of utilization for this time
			lastUtil = procsUsed;
		} else {  //actually record the previous utilization
			ActualStatistics.AppendToLog(lastUtilTime + "\t" +    //Event time
					lastUtil + "\n","util");	       //Procs used
			lastUtil = procsUsed;
			lastUtilTime = time;
		}
	}

	
	//write time statistics to a file
	private void writeTime(AllocInfo allocInfo, long time) {
		long arrival = allocInfo.job.getArrivalTime();
		long runtime = allocInfo.job.getActualTime(allocInfo);
		long startTime = allocInfo.job.getStartTime();
		int procsneeded = allocInfo.job.getProcsNeeded();
		

		if(calcStrictFST && calcRelaxedFST){
			long strictFST= allocInfo.job.getStrictFST();
			long relaxedFST= allocInfo.job.getRelaxedFST();
			ActualStatistics.AppendToLog(
					allocInfo.job.getJobNum() + "\t" +  //Job Num
							arrival + "\t" +			  //Arrival time
							startTime + "\t" +		  //Start time(currentTime)
							time + "\t" +                       //End time
							runtime+ "\t" +                     //Run time
							(startTime - arrival) + "\t" +      //Wait time
							(time - arrival) + "\t" +           //Response time
							procsneeded+ "\t" +                     //Processors needed
							strictFST+ "\t" +                     //Strict FST time
							relaxedFST + "\n","time");	          //Relaxed FST time
		}else if(calcStrictFST && !calcRelaxedFST){
			long strictFST= allocInfo.job.getStrictFST();
			ActualStatistics.AppendToLog(
					allocInfo.job.getJobNum() + "\t" +  //Job Num
							arrival + "\t" +			  //Arrival time
							startTime + "\t" +		  //Start time(currentTime)
							time + "\t" +                       //End time
							runtime+ "\t" +                     //Run time
							(startTime - arrival) + "\t" +      //Wait time
							(time - arrival) + "\t" +           //Response time
							procsneeded+ "\t" +                   //Processors needed  
							strictFST + "\n","time");	          //StrictFST time
		}else if(!calcStrictFST && calcRelaxedFST){
			long relaxedFST= allocInfo.job.getRelaxedFST();
			ActualStatistics.AppendToLog(
					allocInfo.job.getJobNum() + "\t" +  //Job Num
							arrival + "\t" +			  //Arrival time
							startTime + "\t" +		  //Start time(currentTime)
							time + "\t" +                       //End time
							runtime+ "\t" +                     //Run time
							(startTime - arrival) + "\t" +      //Wait time
							(time - arrival) + "\t" +           //Response time
							procsneeded+ "\t" +                 //Processors needed    
							relaxedFST + "\n","time");	       //fst time   
			
		}else if(!calcStrictFST && !calcRelaxedFST){
			ActualStatistics.AppendToLog(
					allocInfo.job.getJobNum() + "\t" +  //Job Num
							arrival + "\t" +			  //Arrival time
							startTime + "\t" +		  //Start time(currentTime)
							time + "\t" +                       //End time
							runtime+ "\t" +                     //Run time
							(startTime - arrival) + "\t" +      //Wait time
							(time - arrival) + "\t" +           //Response time
							procsneeded+ "\n","time");	          //Processors needed

		}
	}

	private void writeWaiting(long time) {
		//possibly add line to log recording number of waiting jobs
		//  (only prints 1 line per time: #waiting jobs after all events at that time)
		//argument is current time or -1 at end of trace

		if(lastWaitTime == -1) {  //if first observation, just remember it
			lastWaitTime = time;
			return;
		}

		if(lastWaitTime == time) {  //update record of waiting jobs for this time
			waitingJobs = tempWaiting;
			return;
		} else {  //actually record the previous # waiting jobs
			if (lastWaitJobs != waitingJobs)
				ActualStatistics.AppendToLog( lastWaitTime + "\t" +    //Event time
						waitingJobs + "\n","wait");//Procs used

			lastWaitJobs = waitingJobs;
			lastWaitTime = time;
			waitingJobs = tempWaiting;

		}
	}

	private void writeAlloc(AllocInfo allocInfo) {

		MeshAllocInfo mai = (MeshAllocInfo)allocInfo;

		ActualStatistics.AppendToLog(mai.job.getProcsNeeded() + "\t" +
				mai.job.getActualTime() + "\t" +
				Mesh.pairwiseL1Distance(mai.processors) + 
				"\n","alloc");
	}

	public void jobArrives(long time) {      //called when a job has arrived
		this.tempWaiting++;
		if(record_wait)
			writeWaiting(time);
	}
}

