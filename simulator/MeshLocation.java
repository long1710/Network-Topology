/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Represents node coordinates in a mesh-based system.
 * 
 * The default ordering for MeshLocations is by the component: x, y, then z.
 * Comparator used to order free blocks in MBSAllocator.
 */

package simulator;

public class MeshLocation implements Comparable<MeshLocation> {
	// representation of location within a mesh (w/ coordinates)

	public int x;
	public int y;
	public int z;

	public Mesh mesh = null;

	public MeshLocation(int X, int Y, int Z) {
		this.x = X;
		this.y = Y;
		this.z = Z;
	}

	public int L1DistanceTo(MeshLocation other) {
		return Math.abs(x - other.x) + Math.abs(y - other.y)
				+ Math.abs(z - other.z);
	}

	public int LInfDistanceTo(MeshLocation other) {
		return Math.max(Math.abs(x - other.x),
				Math.max(Math.abs(y - other.y), Math.abs(z - other.z)));
	}

	public void setMesh(Mesh MESH) {
		this.mesh = MESH;
	}

	public Mesh getMesh() {
		return this.mesh;
	}

	public int compareTo(MeshLocation loc) {
		if (this.x == loc.x) {
			if (this.y == loc.y) {
				return this.z - loc.z;
			}
			return this.y - loc.y;
		}
		return this.x - loc.x;
	}

	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

	public boolean equals(Object other) {
		if (!(other instanceof MeshLocation))
			return false;
		return equals((MeshLocation) other);
	}

	public boolean equals(MeshLocation other) {
		return (other != null) && (x == other.x) && (y == other.y)
				&& (z == other.z);
	}

	public int hashCode() {
		return x + 31 * y + 961 * z;
	}
}
