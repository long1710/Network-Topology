/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 *  First Fit Contiguous Allocator
 *  This allocates a submesh by selecting the first place that works
 **/

package simulator.allocator;

import java.util.ArrayList;
import java.util.Iterator;
import simulator.ContiguousJob;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;
import simulator.ThreeDimension;

public class FirstFitContiguousAllocator extends ContiguousAllocator implements
		PairTestableAllocator {

	protected Mesh meshMachine;
	private MeshLocation start = new MeshLocation(0, 0, 0);
	private boolean startValid = false;
	private static boolean allShapes; // whether to try all possible shapes

	public FirstFitContiguousAllocator(Mesh m) {
		this.meshMachine = m;
		this.machine = m;
	}

	public static FirstFitContiguousAllocator Make(ArrayList<String> params) {
		Factory.argsAtLeast(0, params);
		Factory.argsAtMost(1, params);

		if (params.size() > 0)
			allShapes = true;
		else
			allShapes = false;

		Machine mach = Main.getMachine();
		if (mach instanceof Mesh) {
			Mesh m = (Mesh) mach;
			return new FirstFitContiguousAllocator(m);
		} else {
			Main.error("FirstFitContiguousAllocator requires a mesh machine");
		}
		return null;
	}

	/**
	 * finds an open processor to use as a starting point, then calls
	 * helpCanAllocate, which will return whether or not that is a valid
	 * starting point
	 */
	public boolean canAllocate(Job job) {
		ContiguousJob cjob = (ContiguousJob) job;
		startValid = false;

		if (machine.numFreeProcessors() < job.getProcsNeeded())
			return false;

		MeshLocation ml = canAllocate(job, meshMachine);

		Iterator<ThreeDimension> it;

		if (allShapes) {
			cjob.populateShapes();
			it = cjob.shapes.iterator();
		} else {
			cjob.populateRotations();
			it = cjob.rotations.iterator();
		}
		while (it.hasNext()) {
			if (ml != null) {
				start = ml;
				startValid = true;
				return true;
			}
			ThreeDimension temp = it.next();
			cjob.setDims(temp);
			ml = canAllocate(job, meshMachine);
		}
		cjob.setOriginalDims();
		return false;
	}

	public MeshLocation canAllocate(Job job, Mesh mach) {
		// helper for single-argument version of canAllocate and also
		// used by contiguous schedulers to try possible future
		// situations. Because of 2nd use, does not modify any state.
		// Returns a possible allocation base location (bottom-left
		// corner); null if none.

		ContiguousJob c = (ContiguousJob) job;

		for (int k = 0; k <= mach.getZDim() - c.getZ(); k++) {
			for (int j = 0; j <= mach.getYDim() - c.getY(); j++) {
				for (int i = 0; i <= mach.getXDim() - c.getX(); i++) {
					if (helpCanAllocate(i, j, k, c, mach) == -1) {
						return new MeshLocation(i, j, k);
					}
				}
			}
		}

		return null;
	}

	/**
	 * takes a starting location and see job could be allocated there returns
	 * the x coordinate of the conflict if there is one and -1 if there isn't
	 * one (ie. the allocation is possible)
	 */
	private int helpCanAllocate(int x, int y, int z, ContiguousJob c, Mesh mach) {
		for (int i = x; i < x + c.getX(); i++) {
			for (int j = y; j < y + c.getY(); j++) {
				for (int k = z; k < z + c.getZ(); k++) {
					if (!mach.getIsFree(i, j, k))
						return i;
				}
			}
		}
		return -1;
	}

	public AllocInfo allocate(Job job) {
		ContiguousJob cjob = (ContiguousJob) job;

		if (!canAllocate(cjob)) {
			return null;
		}

		MeshAllocInfo retVal = new MeshAllocInfo(cjob);
		int numProcs = cjob.getProcsNeeded();

		if (startValid == false)
			return null;

		// the coordinates of the starting location
		int xCoord = start.x;
		int yCoord = start.y;
		int zCoord = start.z;

		startValid = false; // since we're using it now

		int i = 0;
		for (int l = zCoord; l <= zCoord + cjob.getZ() - 1; l++) {
			for (int j = xCoord; j <= xCoord + cjob.getX() - 1; j++) {
				for (int k = yCoord; k <= yCoord + cjob.getY() - 1; k++) {
					MeshLocation loc = new MeshLocation(j, k, l);
					retVal.processors[i] = loc;
					i++;
				}
			}
		}

		if (i != numProcs)
			Main.error("FFContigAlloc allocation problem.");

		return retVal;
	}

	public String getSetupInfo(boolean comment) {
		String com;
		if (comment)
			com = "# ";
		else
			com = "";
		return com + "First Fit Contiguous Allocator";
	}

	public AllocInfo pairTest(Mesh m1, ContiguousJob job1, Mesh m2,
			ContiguousJob job2) {
		// checks if j1 can be allocated in m1 w/o using processors needed
		// to allocate j2 in m2. Returns the allocation of j1 if this is
		// possible; null otherwise.

		for (int k1 = 0; k1 <= m1.getZDim() - job1.getZ(); k1++)
			for (int j1 = 0; j1 <= m1.getYDim() - job1.getY(); j1++)
				for (int i1 = 0; i1 <= m1.getXDim() - job1.getX(); i1++) {
					int conflict = helpCanAllocate(i1, j1, k1, job1, m1);
					if (conflict != -1)
						i1 = conflict; // advance loop to avoid the conflict
					else
						for (int k2 = 0; k2 <= m2.getZDim() - job2.getZ(); k2++)
							for (int j2 = 0; j2 <= m2.getYDim() - job2.getY(); j2++)
								for (int i2 = 0; i2 <= m2.getXDim()
										- job2.getX(); i2++)
									if (!intersect(i1, j1, k1, job1, i2, j2,
											k2, job2)) {
										int conflict2 = helpCanAllocate(i2, j2,
												k2, job2, m2);
										if (conflict2 != -1)
											i2 = conflict2; // advance loop to
															// avoid conflict
										else {
											// found a working allocation;
											// return 1st job's part
											MeshAllocInfo ai = new MeshAllocInfo(
													job1);
											int i = 0;
											for (int l = k1; l < k1
													+ job1.getZ(); l++)
												for (int j = i1; j < i1
														+ job1.getX(); j++)
													for (int k = j1; k < j1
															+ job1.getY(); k++) {
														MeshLocation loc = new MeshLocation(
																j, k, l);
														ai.processors[i] = loc;
														i++;
													}
											return ai;
										}
									}
				}
		return null;
	}

	private boolean intersect(int x1, int y1, int z1, ContiguousJob j1, int x2,
			int y2, int z2, ContiguousJob j2) {
		// returns whether j1 based at (x1, y1, z1) intersects j2 based at (x2,
		// y2, z2)
		// helper for pairTest

		// check for separation in each dimension
		if ((x1 >= x2 + j2.getX()) || (x2 >= x1 + j1.getX()))
			return false;
		if ((y1 >= y2 + j2.getY()) || (y2 >= y1 + j1.getY()))
			return false;
		if ((z1 >= z2 + j2.getZ()) || (z2 >= z1 + j1.getZ()))
			return false;

		return true;
	}
}