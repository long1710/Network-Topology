/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */
 
/**
 * Comparator based on the difference between the largest and smallest components of a triple
 */

package simulator.allocator;

import java.util.Comparator;
import simulator.ThreeDimension;

public class MaxDiffComparator implements Comparator<ThreeDimension> {

	public int compare(ThreeDimension d1, ThreeDimension d2) {
		int val1 = getDiff(d1);
		int val2 = getDiff(d2);
		return val1 - val2;
	}

	private int getDiff(ThreeDimension d) {
		int largest = 0;
		int smallest = 0;
		if (d.getX() >= d.getY() && d.getX() >= d.getZ()) {
			largest = d.getX();
			if (d.getY() >= d.getZ()) {
				smallest = d.getZ();
			} else {
				smallest = d.getY();
			}
		} else if (d.getY() >= d.getX() && d.getY() >= d.getZ()) {
			largest = d.getY();
			if (d.getX() >= d.getZ()) {
				smallest = d.getZ();
			} else {
				smallest = d.getX();
			}
		} else if (d.getZ() >= d.getY() && d.getZ() >= d.getX()) {
			largest = d.getZ();
			if (d.getY() >= d.getX()) {
				smallest = d.getX();
			} else {
				smallest = d.getY();
			}
		}
		return largest - smallest;
	}

	public boolean equals(Object other) {
		return (other instanceof MaxDiffComparator);
	}

	public String toString() {
		return "MaxDiffComparator";
	}
}