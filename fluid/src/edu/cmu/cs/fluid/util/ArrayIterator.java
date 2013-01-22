package edu.cmu.cs.fluid.util;

import java.util.NoSuchElementException;

/**
 * An iterator over an array.  It is the responsibility of the
 * creator of the iterator to insure that the contents and size
 * of the array do not change during the lifetime of the iterator.
 */
public final class ArrayIterator<T> extends AbstractRemovelessIterator<T> {
  /** The array to iterate over. */
  private final T[] array;
  /** The next position in the array to be returned. */
  private int pos = 0;

  public ArrayIterator(final T[] o) {
    array = o;
  }

  // Inherit javadoc
  @Override
  public T next() {
    if (pos >= array.length) {
      throw new NoSuchElementException("Iterated too far.");
    } else {
      return array[pos++];
    }
  }

  // Inherit javadoc
  @Override
  public boolean hasNext() {
    return pos < array.length;
  }
}
