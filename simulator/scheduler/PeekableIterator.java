/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Iterator that provides peek function to look at next value without advancing the iterator.
 * (Implemented with a normal iterator, which would be advanced...)
 * 
 * Of general interest, but only used for StatefulScheduler so moved into scheduler package.
 */

package simulator.scheduler;

import java.util.Iterator;

public class PeekableIterator<E> implements Iterator<E> {

    E realNext;  //next element if we've read it for a peek (null if none)
    Iterator<E> innerIterator;  //iterator we get contents from

    public PeekableIterator(Iterator<E> it) {
	realNext = null;
	innerIterator = it;
    }

    public boolean hasNext() {
	//returns true if the iteration has more elements.
	return (realNext != null) || innerIterator.hasNext();
    }

    public E next() {
	//returns the next element in the iteration and advances iterator
	if(realNext != null) {
	    E retVal = realNext;
	    realNext = null;
	    return retVal;
	}
	return innerIterator.next();
    }

    public E peek() {
	//returns next element in iteration w/o advancing iterator
	if(realNext == null)
	    realNext = innerIterator.next();
	return realNext;
    }

    public void	remove() {
	//would remove element just returned, but can't do this with peeking so unsupported
	throw new UnsupportedOperationException();
    }
}
