/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Class for helper functions that may be reusable
 */

package simulator;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class Utility {
	public static int getSign(long num) {
		//returns -1, 0, 1 to match sign of num
		//used when writing comparators that include a comparison of longs

		if(num > 0)
			return 1;
		if(num < 0)
			return -1;
		return 0;
	}

	public static <E> void addToSortedList(List<E> list, E toInsert,
			Comparator<E> comp) {
		//add toInsert to sorted list (sorted by comp)

		if (list.size() == 0) {  	//Empty list. Just add.
			list.add(toInsert);
			return;
		}

		//look for first list element that goes after element to insert
		ListIterator<E> it = list.listIterator();
		while(it.hasNext()) {
			E inList = it.next();
			if (comp.compare(toInsert, inList) <= 0) {
				it.previous();
				it.add(toInsert);
				return;
			}
		}

		//reached end of list without finding place to insert so insert at end
		list.add(toInsert);
	}
}
