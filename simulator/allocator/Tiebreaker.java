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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public class Tiebreaker {

	private long maxshells;

	private long availFactor;
	private long wallFactor;
	private long borderFactor;

	private long curveFactor = 0;
	private long curveWidth = 2;

	public String lastTieInfo;

	// Takes mesh center, available processors sorted by correct comparator,
	// and number of processors needed and returns tiebreak value.
	public long getTiebreak(MeshLocation center, MeshLocation[] avail, int num) {
		long ret = 0;

		lastTieInfo = "0\t0\t0";

		if (avail.length == num)
			return 0;

		LInfComparator lc = new LInfComparator(center.x, center.y, center.z);
		Arrays.sort(avail, lc);

		if (maxshells == 0)
			return 0;

		long ascore, wscore, bscore;
		ascore = wscore = bscore = 0;

		long lastshell = center.LInfDistanceTo(avail[num - 1]);
		long lastlook = lastshell + maxshells;
		lastTieInfo = "";
		// lastTieInfo=center+"\t"+lastshell+"\t"+lastlook+"\t";
		Mesh mesh = (Mesh) Main.getMachine();
		long ydim = mesh.getYDim();

		// Add to score for nearby available processors.
		if (availFactor != 0) {
			for (int i = num; i < avail.length; i++) {
				long dist = center.LInfDistanceTo(avail[i]);
				if (dist > lastlook)
					break;
				else {
					ret += availFactor * (lastlook - dist + 1);
					ascore += availFactor * (lastlook - dist + 1);
				}
			}
		}

		// Subtract from score for nearby walls
		if (wallFactor != 0) {
			long xdim = mesh.getXDim();
			long zdim = mesh.getZDim();
			for (int i = 0; i < num; i++) {
				long dist = center.LInfDistanceTo(avail[i]);
				if (((avail[i].x == 0 || avail[i].x == xdim - 1) && xdim > 2)
						|| ((avail[i].y == 0 || avail[i].y == ydim - 1) && ydim > 2)
						|| ((avail[i].z == 0 || avail[i].z == zdim - 1) && zdim > 2)) {
					wscore -= wallFactor * (lastlook - dist + 1);
					ret -= wallFactor * (lastlook - dist + 1);
				}
			}
		}

		// Subtract from score for bordering allocated processors
		if (borderFactor != 0) {
			ArrayList<MeshLocation> used = mesh.usedProcessors();
			Collections.sort(used, new LInfComparator(center.x, center.y,
					center.z));
			for (MeshLocation ml : used) {
				long dist = center.LInfDistanceTo(ml);
				if (dist > lastlook)
					break;
				else if (dist == lastshell + 1) {
					ret -= borderFactor * (lastlook - dist + 1);
					bscore -= borderFactor * (lastlook - dist + 1);
				}
			}
		}

		// Add to score for being at a worse curve location
		// Only works for 2D now.
		long cscore = 0;
		if (curveFactor != 0) {
			long centerLine = center.x / curveWidth;
			long tsc = ydim * centerLine;
			tsc += (centerLine % 2 == 0) ? (center.y) : (ydim - center.y);
			cscore += curveFactor * tsc;
			ret += cscore;
		}

		// lastTieInfo = ascore+"\t"+wscore+"\t"+bscore;
		lastTieInfo = lastTieInfo + ascore + "\t" + wscore + "\t" + bscore
				+ "\t" + cscore;
		return ret;
	}

	public Tiebreaker(long ms, long af, long wf, long bf) {
		maxshells = ms;
		availFactor = af;
		wallFactor = wf;
		borderFactor = bf;
	}

	public void setCurveFactor(long cf) {
		curveFactor = cf;
	}

	public void setCurveWidth(long cw) {
		curveWidth = cw;
	}

	public String getInfo() {
		return "(" + maxshells + "," + availFactor + "," + wallFactor + ","
				+ borderFactor + "," + curveFactor + "," + curveWidth + ")";
	}

	public long getMaxShells() {
		return maxshells;
	}

}
