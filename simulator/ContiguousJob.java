/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * A job that must be allocated contiguously (as a grid in a mesh)
 */

package simulator;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeSet;

import simulator.allocator.MaxDiffComparator;

import mapping.TaskMapper;

public class ContiguousJob extends Job {
	private int x;
	private int y;
	private int z;
	private ThreeDimension original;
	public ArrayList<ThreeDimension> rotations;
	public TreeSet<ThreeDimension> shapes; // for all shapes allocator

	public ContiguousJob(long ArrivalTime, int x, int y, int z,
			long ActualRunningTime, long EstRunningTime) {
		super(ArrivalTime, x * y * z, ActualRunningTime, EstRunningTime);
		initialize(x, y, z);
	}

	private void initialize(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		original = new ThreeDimension(x, y, z);
	}

	ContiguousJob(Scanner input, boolean accurateEsts, TaskMapper map) {
		String line = input.nextLine();
		Scanner lineScanner = new Scanner(line);

		long arrivalTime = lineScanner.nextLong();
		x = lineScanner.nextInt();
		y = lineScanner.nextInt();
		z = lineScanner.nextInt();
		long actualRunningTime = lineScanner.nextLong();
		if (!accurateEsts && lineScanner.hasNextLong()) {
			long estRunningTime = lineScanner.nextLong();
			initialize(arrivalTime, x * y * z, actualRunningTime,
					estRunningTime, map);
		} else
			initialize(arrivalTime, x * y * z, actualRunningTime,
					actualRunningTime, map);

		initialize(x, y, z);
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getZ() {
		return this.z;
	}

	public void setOriginalDims() {
		this.x = original.getX();
		this.y = original.getY();
		this.z = original.getZ();
	}

	public void populateShapes() {
		shapes = new TreeSet<ThreeDimension>(new MaxDiffComparator());
		for (int i = 1; i <= (x * y * z); i++) {
			int left = (x * y * z) / i;
			for (int j = 1; j <= left; j++) {
				shapes.add(new ThreeDimension(i, j, ((x * y * z) / (j * i))));
			}
		}
	}

	public void populateRotations() {
		rotations = new ArrayList<ThreeDimension>();
		rotations.add(new ThreeDimension(x, y, z));
		rotations.add(new ThreeDimension(y, x, z));
		rotations.add(new ThreeDimension(x, z, y));
		rotations.add(new ThreeDimension(z, x, y));
		rotations.add(new ThreeDimension(z, y, x));
		rotations.add(new ThreeDimension(y, z, x));
	}

	public void setDims(ThreeDimension d) {
		this.x = d.getX();
		this.y = d.getY();
		this.z = d.getZ();
	}

	public String toString() {
		return "Job #" + jobNum + " (" + arrivalTime + ", " + this.x + "x"
				+ this.y + "x" + this.z + ", " + actualRunningTime + ", "
				+ estRunningTime + ")";
	}

}
