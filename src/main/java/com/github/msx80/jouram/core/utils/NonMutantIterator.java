package com.github.msx80.jouram.core.utils;

import java.util.Iterator;

/**
 * Simple decorator to make an Iterator non-mutant
 *
 * @param <E>
 */
public final class NonMutantIterator<E> implements Iterator<E> {

	Iterator<E> src;
	
	public NonMutantIterator(Iterator<E> src) {
		super();
		this.src = src;
	}

	@Override
	public void remove() {
        throw new UnsupportedOperationException("NonMutantIterator cannot remove");
    }
	
	@Override
	public boolean hasNext() {
		return src.hasNext();
	}

	@Override
	public E next() {
		return src.next();
	}

}
