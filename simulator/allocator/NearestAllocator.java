/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Class to implement allocation algorithms of the family that
 * includes Gen-Alg, MM, and MC1x1; from each candidate center,
 * consider the closest points, and return the set of closest points
 * that is best.  Members of the family are specified by giving the
 * way to find candidate centers, how to measure "closeness" of points
 * to these, and how to evaluate the sets of closest points.

GenAlg - try centering at open places;
  select L_1 closest points;
  eval with sum of pairwise L1 distances
MM - center on intersection of grid in each direction by open places;
  select L_1 closest points
  eval with sum of pairwise L1 distances
MC1x1 - try centering at open places
  select L_inf closest points
  eval with L_inf distance from center
 */

package simulator.allocator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import simulator.Factory;
import simulator.Job;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;
import simulator.Pair;
import simulator.Statistics;

public class NearestAllocator extends Allocator {

	//way to generate list of possible centers:
	private CenterGenerator centerGenerator;

	//how to find candidate points from a center:
	private PointCollector pointCollector;

	//how we evaluate a possible allocation:
	private Scorer scorer;

	private String configName;

	public NearestAllocator(Mesh m, CenterGenerator cg,
			PointCollector pc, Scorer s,String name) {
		machine = m;
		centerGenerator = cg;
		pointCollector = pc;
		scorer = s;
		configName = name;

	}

	public static NearestAllocator Make(ArrayList<String> params){
		if(params.get(0).equals("nearest")){
			Factory.argsAtLeast(3,params);
			Factory.argsAtMost(3,params);
		}

		try{
			Mesh m = (Mesh) Main.getMachine();

			if(params.get(0).equals("MM"))
				return MMAllocator(m);
			if(params.get(0).equals("MC1x1"))
				return MC1x1Allocator(m);
			if(params.get(0).equals("genAlg"))
				return genAlgAllocator(m);
			if(params.get(0).equals("OldMC1x1"))
				return OldMC1x1Allocator(m);

			CenterGenerator cg=null;
			PointCollector pc=null;
			Scorer sc=null;

			String cgstr=params.get(1);

			if(cgstr.equals("free"))
				cg=new FreeCenterGenerator(m);
			else if(cgstr.equals("intersect"))
				cg=new IntersectionCenterGen(m);
			else
				Main.error("Unknown center generator "+cgstr);

			String pcstr=params.get(2);

			if(pcstr.equals("L1"))
				pc=new L1PointCollector();
			else if(pcstr.equals("LInf"))
				pc=new LInfPointCollector();
			else if(pcstr.equals("GreedyLInf"))
				pc=new GreedyLInfPointCollector();
			else
				Main.error("Unknown point collector "+pcstr);


			ArrayList<String> sclist = Factory.ParseInput(params.get(3));
			String scstr=sclist.get(0);

			if(scstr.equals("L1"))
				sc=new L1DistFromCenterScorer();
			else if(scstr.equals("LInf")){
				Factory.argsAtMost(6,sclist);
				long TB=0;
				long af=1;
				long wf=0;
				long bf=0;
				long cf=0;
				long cw=2;
				if(sclist.size()-1 >= 1)
					if(sclist.get(1).equals("M"))
						TB=Long.MAX_VALUE;
					else
						TB=Long.parseLong(sclist.get(1));
				if(sclist.size()-1 >= 2)
					af=Long.parseLong(sclist.get(2));
				if(sclist.size()-1 >= 3)
					wf=Long.parseLong(sclist.get(3));
				if(sclist.size()-1 >= 4)
					bf=Long.parseLong(sclist.get(4));
				if(sclist.size()-1 >= 5)
					cf=Long.parseLong(sclist.get(5));
				if(sclist.size()-1 >= 6)
					cw=Long.parseLong(sclist.get(6));

				Tiebreaker tb = new Tiebreaker(TB,af,wf,bf);
				tb.setCurveFactor(cf);
				tb.setCurveWidth(cw);
				sc=new LInfDistFromCenterScorer(tb);
			}
			else if(scstr.equals("Pairwise"))
				sc=new PairwiseL1DistScorer();
			else
				Main.error("Unknown scorer "+scstr);

			return new NearestAllocator(m,cg,pc,sc,"custom");
		} catch(ClassCastException e){
			Main.error("Nearest allocators require a Mesh machine");
		}
		return null;

	}

	public static String getParamHelp(){
		return "[<center_gen>,<point_col>,<scorer>]\n"+
				"\tcenter_gen: Choose center generator (all, free, intersect)\n"+
				"\tpoint_col: Choose point collector (L1, LInf, GreedyLInf)\n"+
				"\tscorer: Choose point scorer (L1, LInf, Pairwise)";
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"Nearest Allocator ("+configName+")\n"+com+
				"\tCenterGenerator: "+centerGenerator.getSetupInfo(false)+"\n"+com+
				"\tPointCollector: "+pointCollector.getSetupInfo(false)+"\n"+com+
				"\tScorer: "+scorer.getSetupInfo(false);
	}

	public AllocInfo allocate(Job job){
		return allocate(job,((Mesh)machine).freeProcessors());
	}

	public AllocInfo allocate(Job job, ArrayList<MeshLocation> available) {
		//allocates job if possible
		//returns information on the allocation or null if it wasn't possible
		//(doesn't make allocation; merely returns info on possible allocation)

		if(!canAllocate(job, available))
			return null;

		MeshAllocInfo retVal = new MeshAllocInfo(job);

		int numProcs = job.getProcsNeeded();
		//ArrayList<MeshLocation> available = ((Mesh)machine).freeProcessors();
		MeshLocation[] availArray =
				available.toArray(new MeshLocation[available.size()]);

		//optimization: if exactly enough procs are free, just return them
		if(numProcs == availArray.length) {
			for(int i=0; i < numProcs; i++)
				retVal.processors[i] = availArray[i];
			return retVal;
		}

		//score of best value found so far with it tie-break score:
		Pair<Long,Long> bestVal = new Pair<Long,Long>(Long.MAX_VALUE,
				Long.MAX_VALUE);

		boolean recordingTies = Statistics.recordingTies();

		//stores allocations w/ best score (no tiebreaking) if ties being recorded:
		//(actual best value w/ tiebreaking stored in retVal.processors)
		HashSet<HashSet<MeshLocation>> bestAllocs = null;
		if(recordingTies)
			bestAllocs = new HashSet<HashSet<MeshLocation>>();

		List<MeshLocation> possCenters = centerGenerator.getCenters(available);
		for(MeshLocation center : possCenters) {
			MeshLocation[] nearest = pointCollector.getNearest(center, numProcs,
					availArray);
			Pair<Long,Long> val = scorer.valueOf(center, nearest, numProcs);

			// This is the best value if its score is less than the best score or
			//if the scores are the same but its tiebreaker score is less
			if((val.getFirst().compareTo(bestVal.getFirst()) < 0) || 
					(val.getFirst().equals(bestVal.getFirst()) &&
							(val.getSecond().compareTo(bestVal.getSecond()) < 0))) {
				bestVal = val;
				for(int i=0; i<numProcs; i++)
					retVal.processors[i] = nearest[i];
				if(recordingTies)
					bestAllocs.clear();
			}

			if(recordingTies && val.getFirst().equals(bestVal.getFirst())) {
				HashSet<MeshLocation> alloc = new HashSet<MeshLocation>();
				for(int i=0; i<numProcs; i++)
					alloc.add(nearest[i]);
				bestAllocs.add(alloc);
			}
		}

		if(recordingTies && (bestAllocs.size() > 1))
			Statistics.writeTie(numProcs + "\t" + bestAllocs.size() + "\n");

		return retVal;
	}

	public static NearestAllocator genAlgAllocator(Mesh m) {
		return
				new NearestAllocator(m,
						new FreeCenterGenerator(m),
						new L1PointCollector(),
						new PairwiseL1DistScorer(),"genAlg");
	}

	public static NearestAllocator MMAllocator(Mesh m) {
		return
				new NearestAllocator(m,
						new IntersectionCenterGen(m),
						new L1PointCollector(),
						new PairwiseL1DistScorer(),"MM");
	}

	public static NearestAllocator OldMC1x1Allocator(Mesh m) {
		return
				new NearestAllocator(m,
						new FreeCenterGenerator(m),
						new LInfPointCollector(),
						new LInfDistFromCenterScorer(new Tiebreaker(0,0,0,0)),"MC1x1");
	}

	public static NearestAllocator MC1x1Allocator(Mesh m) {
		return
				new NearestAllocator(m,
						new FreeCenterGenerator(m),
						new GreedyLInfPointCollector(),
						new LInfDistFromCenterScorer(new Tiebreaker(0,0,0,0)),"MC1x1");
	}
}

