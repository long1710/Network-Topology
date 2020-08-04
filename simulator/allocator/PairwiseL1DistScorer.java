/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package simulator.allocator;

import java.util.Comparator;
import simulator.Mesh;
import simulator.MeshLocation;
import simulator.Pair;

public class PairwiseL1DistScorer extends Scorer {
	// evaluates by sum of pairwise L1 distances

	public Pair<Long, Long> valueOf(MeshLocation center, MeshLocation[] procs,
			int num) {
		// returns pairwise L1 dist between first num members of procs
		return new Pair<Long, Long>(Mesh.pairwiseL1Distance(procs, num),
				Long.valueOf(0));
	}

	public Comparator<MeshLocation> getComparator(MeshLocation center) {
		return null;
	}

	public String getSetupInfo(boolean comment) {
		String com;
		if (comment)
			com = "# ";
		else
			com = "";
		return com + "PairwiseL1DistScorer";
	}
}
