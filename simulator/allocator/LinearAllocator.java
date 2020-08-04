/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Abstract class to act as superclass for allocators based on curves.
 *
 * Format for file giving curve:
 * list of pairs of numbers (separated by whitespace)
 *   the first member of each pair is the processor 0-indexed
 *     rank if the coordinates are treated as a 3-digit number
 *     (z coord most significant, x coord least significant)
 *     these values should appear in order
 *   the second member of each pair gives its rank in the desired order
 *
 */

package simulator.allocator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.TreeSet;
import simulator.Job;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public abstract class LinearAllocator extends Allocator {

	private static final boolean DEBUG = false;

	protected class MeshLocationOrdering implements Comparator<MeshLocation> {
		//represent linear ordering

		private int xdim;     //size of mesh in each dimension
		private int ydim;
		private int zdim;

		private int[] rank;   //way to store ordering
		//(x,y,z) has position rank[x+y*xdim+z*xdim*ydim] in ordering

		public MeshLocationOrdering(Mesh m, String filename) {
			//constructor taking machine and name of file with ordering
			//file format is as described in comment at top of this file

			xdim = m.getXDim();
			ydim = m.getYDim();
			zdim = m.getZDim();

			Scanner input = null;
			String curveDirectory = System.getenv("SIMCURVE");
			if(curveDirectory != null) {
				try {
					input = new Scanner(new File(curveDirectory+filename));
				} catch(java.io.FileNotFoundException ex) {
				}
			}
			if(input == null) {
				try {
					input = new Scanner(new File(filename));
				} catch(java.io.FileNotFoundException exc) {
				}
			}
			String sizeSuffix = "." + xdim + "." + ydim + "." + zdim;  //try suffix w/ mesh dimensions
			if((input == null) && (curveDirectory != null)) {
				try {
					input = new Scanner(new File(curveDirectory+filename+sizeSuffix));
				} catch(java.io.FileNotFoundException exc) {
				}
			}
			if(input == null) {
				try {
					input = new Scanner(new File(filename+sizeSuffix));
				} catch(java.io.FileNotFoundException exc) {
					Main.error("File not found: " + filename);
				}
			}
			rank = new int[xdim*ydim*zdim];
			int nextBaseRank = 0;
			while(input.hasNextInt() && (nextBaseRank < rank.length)) {
				int val = input.nextInt();
				if(val != nextBaseRank)
					Main.error("Incorrect value in file w/ linear order: "
							+ val + " (expected " + nextBaseRank + ")");
				if(!input.hasNextInt())
					Main.error("File w/ linear order terminates mid-pair");
				rank[nextBaseRank] = input.nextInt();
				nextBaseRank++;
			}
			input.close();
			if(nextBaseRank != rank.length)
				Main.error("File with linear order has wrong number of pairs");
		}

		public int rankOf(MeshLocation L) {
			//returns the rank of a given location
			return rank[L.x + L.y*xdim + L.z * xdim * ydim];
		}

		public MeshLocation locationOf(int Rank) {
			//return MeshLocation having given rank
			//raises exception if Rank is out of range

			int pos = -1;   //ordinal value of position having rank
			int x, y, z;    //coordinate values

			//find Rank in array with ordering
			int i=0;
			while(pos == -1) {
				if(rank[i] == Rank)
					pos = i;
				i++;
			}

			//translate position in ordering into coordinates
			x = pos % xdim;
			pos = pos / xdim;
			y = pos % ydim;
			z = pos / ydim;
			return new MeshLocation(x, y, z);
		}

		public int compare(MeshLocation L1, MeshLocation L2) {
			int rank1 = rankOf(L1);
			int rank2 = rankOf(L2);
			return (rank1 - rank2);
		}

		public boolean equals(Object other) {
			if((other == null) || !(other instanceof MeshLocationOrdering))
				return false;
			MeshLocationOrdering mloOther = (MeshLocationOrdering)other;
			if(rank.length != mloOther.rank.length)
				return false;
			for(int i=0; i<rank.length; i++)
				if(rank[i] != mloOther.rank[i])
					return false;
			return true;
		}
	}

	protected MeshLocationOrdering ordering;

	public LinearAllocator(Mesh m, String filename) {
		//takes machine to be allocated and name of the file with ordering
		//file format is as described in comment at top of this file

		machine = m;
		ordering = new MeshLocationOrdering(m, filename);
	}



	protected ArrayList<ArrayList<MeshLocation>> getIntervals() {
		//returns list of intervals of free processors
		//each interval represented by a list of its locations

		TreeSet<MeshLocation> avail = new TreeSet<MeshLocation>(ordering);
		avail.addAll(((Mesh)machine).freeProcessors());

		ArrayList<ArrayList<MeshLocation>> retVal =  //list of intervals so far
				new ArrayList<ArrayList<MeshLocation>>();
		ArrayList<MeshLocation> curr =               //interval being built
				new ArrayList<MeshLocation>();
		int lastRank = -2;                           //rank of last element
		//-2 is sentinel value

		for(MeshLocation ml : avail) {
			int mlRank = ordering.rankOf(ml);
			if((mlRank != lastRank + 1) && (lastRank != -2)) {
				//need to start new interval
				retVal.add(curr);
				curr = new ArrayList<MeshLocation>();
			}
			curr.add(ml);
			lastRank = mlRank;
		}
		if(curr.size() != 0)   //add last interval if nonempty
			retVal.add(curr);

		if(DEBUG) {
			System.err.println("getIntervals:");
			for(ArrayList<MeshLocation> ar : retVal) {
				System.err.print("Interval: ");
				for(MeshLocation ml : ar)
					System.err.print(ml + " ");
				System.err.println();
			}
		}

		return retVal;
	}

	protected AllocInfo minSpanAllocate(Job job) {
		//version of allocate that just minimizes the span

		MeshLocation[] avail = new MeshLocation[machine.numFreeProcessors()];
		avail = ((Mesh)machine).freeProcessors().toArray(avail);
		Arrays.sort(avail, ordering);
		int num = job.getProcsNeeded();

		//scan through possible starting locations to find best one
		int bestStart = 0;   //index of best starting location so far
		int bestSpan = ordering.rankOf(avail[num-1])   //best location's span
				- ordering.rankOf(avail[0]);
		for(int i=1; i<=avail.length-num; i++) {
			int candidate = ordering.rankOf(avail[i+num-1]) 
					- ordering.rankOf(avail[i]);
			if(candidate < bestSpan) {
				bestStart = i;
				bestSpan = candidate;
			}
		}

		//return the best allocation found
		MeshAllocInfo retVal = new MeshAllocInfo(job);
		for(int i=0; i<num; i++)
			retVal.processors[i] = avail[bestStart+i];
		return retVal;
	}
}
