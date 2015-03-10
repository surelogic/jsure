package com.surelogic.util;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import com.surelogic.Borrowed;
import com.surelogic.common.util.EmptyIterator;

public final class NullList<E> extends NullCollection<E> implements List<E> {
  @SuppressWarnings("rawtypes")
  private static final NullList prototype = new NullList();

  private NullList() {
    // TODO Auto-generated constructor stub
  }
  
  @SuppressWarnings({ "cast", "unchecked" })
  public static <T> NullList<T> prototype() {
    return (NullList<T>) prototype;
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(@Borrowed final Object o) {
    // Equal to any list with no elements
    return (o == this) || ((o instanceof List) && (((List) o).size() == 0));
  }

  @Override
  public int hashCode() {
    // Empty list has a hashcode of 1
    return 1;
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends E> c) {
    return false;
  }

  @Override
  public E get(final int index) {
    throw new IndexOutOfBoundsException("NullList is always empty");
  }

  @Override
  public E set(final int index, final E element) {
    return null;
  }

  @Override
  public void add(final int index, final E element) {
    // do nothing
  }

  @Override
  public E remove(final int index) {
    throw new IndexOutOfBoundsException("NullList is always empty");
  }

  @Override
  public int indexOf(final Object o) {
    return -1;
  }

  @Override
  public int lastIndexOf(final Object o) {
    return -1;
  }

  @Override
  public ListIterator<E> listIterator() {
    return new EmptyIterator<E>();
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    throw new IndexOutOfBoundsException("NullList is always empty");
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    throw new IndexOutOfBoundsException("NullList is always empty");
  }
}
