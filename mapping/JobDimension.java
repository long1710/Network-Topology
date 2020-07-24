/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package mapping;

public class JobDimension {
	public int x;
	public int y;
	public int z;

	public JobDimension(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	// returns the total number of needed processors
	public int totProcs() {
		return this.x * this.y * this.z;
	}

	public int minHops() {
		return ((this.x - 1) * this.y * this.z + (this.y - 1) * this.x + this.z + (this.z - 1)
				* this.x * this.y);
	}
}
