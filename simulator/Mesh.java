/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Represents a machine organized as a 3D mesh
 */

package simulator;

import java.util.ArrayList;
import java.util.Scanner;
import simulator.allocator.AllocInfo;
import simulator.allocator.MeshAllocInfo;

public class Mesh extends Machine {

	private int xdim;              //size of mesh in each dimension
	private int ydim;
	private int zdim;

	private boolean[][][] isFree;  //whether each processor is free

	public Mesh(int Xdim, int Ydim, int Zdim) {
		//constructor that takes mesh dimensions

		xdim = Xdim;
		ydim = Ydim;
		zdim = Zdim;
		numProcs = Xdim * Ydim * Zdim;
		isFree = new boolean[xdim][ydim][zdim];
		reset();
	}

	public Mesh(boolean[][][] IsFree) {
		//constructor for testing
		//takes array telling which processors are free

		isFree = IsFree;
		xdim = isFree.length;
		ydim = isFree[0].length;
		zdim = isFree[0][0].length;

		numAvail = 0;
		for(int x=0; x<xdim; x++)
			for(int y=0; y<ydim; y++)
				for(int z=0; z<zdim; z++)
					if(isFree[x][y][z])
						numAvail++;
	}

	public Mesh(Mesh other) {  //copy constructor
		xdim = other.xdim;
		ydim = other.ydim;
		zdim = other.zdim;
		numAvail = other.numAvail;
		numProcs = other.numProcs;
		isFree = new boolean[xdim][ydim][zdim];
		for(int i=0; i<xdim; i++)
			for(int j=0; j<ydim; j++)
				for(int k=0; k<zdim; k++)
					isFree[i][j][k] = other.isFree[i][j][k];
	}

	public Mesh(Scanner scan) {
		//Currently there is no testing that a scanner satifies requirements

		if (!scan.hasNext()) {
			System.exit(1);
		}
		this.xdim = scan.nextInt();
		this.ydim = scan.nextInt();
		this.zdim = scan.nextInt();

		isFree = new boolean[xdim][ydim][zdim];
		String row;

		//Ordering traverses the column, then row, then plane
		for(int z = 0; z < zdim; z++) {
			scan.nextLine(); //skips blank line
			for(int y = 0; y < ydim; y++) {
				row = scan.nextLine();
				for(int x = 0; x < xdim; x++) {
					if (row.charAt(x) == 'X') {
						isFree[x][(ydim-1)-y][z] = true;
					}
					else {
						isFree[x][(ydim-1)-y][z] = false;
					}
				}
			}
		}
	}

	public static Mesh Make(ArrayList<String> params) {
		Factory.argsAtLeast(3,params);
		Factory.argsAtMost(3,params);

		return new Mesh(Integer.parseInt(params.get(1)),
				Integer.parseInt(params.get(2)),
				Integer.parseInt(params.get(3)));
	}

	public static String getParamHelp() {
		return "[<x dim>,<y dim>,<z dim>]\n\t3D Mesh with specified dimensions";
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+xdim+"x"+ydim+"x"+zdim+" Mesh";
	}

	public int getXDim() {
		return xdim;
	}

	public int getYDim() {
		return ydim;
	}

	public int getZDim() {
		return zdim;
	}

	public int getMachSize() {
		return xdim*ydim*zdim;
	}

	public boolean getIsFree(int x, int y, int z) {
		return isFree[x][y][z];
	}

	public void reset() {
		numAvail = xdim * ydim * zdim;
		for(int i=0; i<xdim; i++)
			for(int j=0; j<ydim; j++)
				for(int k=0; k<zdim; k++)
					isFree[i][j][k] = true;
	}

	public ArrayList<MeshLocation> freeProcessors() {
		//returns list of free processors

		ArrayList<MeshLocation> retVal = new ArrayList<MeshLocation>();
		for(int i=0; i<xdim; i++)
			for(int j=0; j<ydim; j++)
				for(int k=0; k<zdim; k++)
					if(isFree[i][j][k]) {
						MeshLocation loc = new MeshLocation(i, j, k);
						loc.setMesh(this);
						retVal.add(loc);
					}
		return retVal;
	}

	public ArrayList<MeshLocation> usedProcessors() {
		//returns list of used processors
		ArrayList<MeshLocation> retVal = new ArrayList<MeshLocation>();
		for(int i=0; i<xdim; i++)
			for(int j=0; j<ydim; j++)
				for(int k=0; k<zdim; k++)
					if(!isFree[i][j][k]) {
						MeshLocation loc = new MeshLocation(i, j, k);
						loc.setMesh(this);
						retVal.add(loc);
					}
		return retVal;
	}

	public void allocate(AllocInfo allocInfo) {
		//allocate list of processors in allocInfo

		MeshLocation[] procs = ((MeshAllocInfo)allocInfo).processors;

		for(int i=0; i<procs.length; i++) {
			if(!isFree[procs[i].x][procs[i].y][procs[i].z])
				Main.error("Attempt to allocate a busy processor: "
						+ procs[i]);
			isFree[procs[i].x][procs[i].y][procs[i].z] = false;
		}
		numAvail -= procs.length;
	}

	public void deallocate(AllocInfo allocInfo) {
		//deallocate list of processors in allocInfo

		MeshLocation[] procs = ((MeshAllocInfo)allocInfo).processors;

		for(int i=0; i<procs.length; i++) {
			if(isFree[procs[i].x][procs[i].y][procs[i].z])
				Main.error("Attempt to deallocate a free processor: "
						+ procs[i]);
			isFree[procs[i].x][procs[i].y][procs[i].z] = true;
		}
		numAvail += procs.length;
	}

	public static long pairwiseL1Distance(MeshLocation[] locs) {
		//returns total pairwise L_1 distance between all array members
		return pairwiseL1Distance(locs, locs.length);
	}

	public static long pairwiseL1Distance(MeshLocation[] locs, int num) {
		//returns total pairwise L_1 distance between 1st num members of array
		long retVal = 0;
		for(int i=0; i<num; i++)
			for(int j=i+1; j<num; j++)
				retVal += locs[i].L1DistanceTo(locs[j]);
		return retVal;
	}

	public String toString() {
		//returns human readable view of which processors are free
		//presented in layers by z-coordinate (0 first), with the
		//  (0,0) position of each layer in the bottom left
		//uses "X" and "." to denote free and busy processors respectively

		int xdim = isFree.length;
		int ydim = isFree[0].length;
		int zdim = isFree[0][0].length;

		String retVal = "";
		retVal += xdim + " " + ydim + " " + zdim + "\n";
		for(int z=0; z<zdim; z++) {
			for(int y=0; y<ydim; y++) {
				for(int x=0; x<xdim; x++)
					if(isFree[x][(ydim-1)-y][z])
						retVal += "X";
					else
						retVal += ".";
				retVal += "\n";
			}

			if(z != zdim-1)  //add blank line between layers
				retVal += "\n";
		}

		return retVal;
	}
}

