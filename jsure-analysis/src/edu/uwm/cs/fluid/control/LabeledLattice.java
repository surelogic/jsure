/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/LabeledLattice.java,v 1.13 2008/06/24 19:13:16 thallora Exp $*/
package edu.uwm.cs.fluid.control;

import edu.cmu.cs.fluid.control.LabelList;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * A specialized lattice like structure for handling labeled lattice values:
 * Each value is similar to an association list, but the values are immutable.
 * This class was designed to avoid object construction. Perhaps this is a
 * ridiculous optimization, perhaps not.
 * 
 * @author boyland
 */
public final class LabeledLattice<T> {

  final Lattice<T> lattice;

  @SuppressWarnings("unchecked")
  private final T secret = (T) new Object();

  public LabeledLattice(Lattice<T> lat) {
    lattice = lat;
  }

  // a binary operator with an extra argument so we can avoid creating
  // short-lived
  // instances.
  public static interface Combiner<T2, U> {
    T2 combine(T2 x, T2 y, U arg);

    UnaryOp<T2, U> bindLeftBottom();

    UnaryOp<T2, U> bindRightBottom();
  }

  public static abstract class AbstractCombiner<T2, U> implements Combiner<T2, U> {
    protected final UnaryOp<T2, U> identityOp = new UnaryOp<T2, U>() {
      @Override
      public T2 operate(T2 x, U v) {
        return x;
      }
    };

    @Override
    public UnaryOp<T2, U> bindLeftBottom() {
      return identityOp;
    }

    @Override
    public UnaryOp<T2, U> bindRightBottom() {
      return identityOp;
    }
  }

  public final Combiner<T, Void> joinCombiner = new AbstractCombiner<T, Void>() {
    @Override
    public T combine(T v1, T v2, Void arg) {
      return lattice.join(v1, v2);
    }
  };
  public final Combiner<T, Void> widenCombiner = new AbstractCombiner<T, Void>() {
    @Override
    public T combine(T v1, T v2, Void arg) {
      return lattice.widen(v1, v2);
    }
  };

  // a unary operator with an extra argument to avoid creating instances
  // frequently.
  public static interface UnaryOp<T2, U> {
    T2 operate(T2 x, U arg);
  }

  public final UnaryOp<T, Void> identityOp = new UnaryOp<T, Void>() {
    @Override
    public T operate(T x, Void v) {
      return x;
    }
  };

  public static interface LabelOp<U> {
    /**
     * Perform an operation on a label
     * 
     * @param ll
     *          label list to operate on
     * @param arg
     *          extra argument necessary to do work and avoid mem-alloc
     * @return changed label list, or null if this label not applicable to
     *         operation
     */
    LabelList operate(LabelList ll, U arg);
  }

  public static class LabeledValue<T2> {
    final LabelList labelList;
    final T2 value;
    final LabeledValue<T2> next;

    public LabeledValue(LabelList ll, T2 v, LabeledValue<T2> n) {
      if (v == null)
        throw new NullPointerException("Lattice values cannot be null");
      labelList = ll;
      value = v;
      next = n;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      for (LabeledValue<T2> l = this; l != null; l = l.next) {
        sb.append("(");
        sb.append(l.labelList);
        sb.append(":");
        sb.append(l.value);
        sb.append(")");
        if (l.next != null)
          sb.append(" ");
      }
      sb.append("}");
      return sb.toString();
    }

    public String toString(Lattice<T2> lattice) {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      for (LabeledValue<T2> l = this; l != null; l = l.next) {
        sb.append("(");
        sb.append(l.labelList);
        sb.append(":");
        sb.append(lattice.toString(l.value));
        sb.append(")");
        if (l.next != null)
          sb.append(" ");
      }
      sb.append("}");
      return sb.toString();
    }
  }

  public int length(LabeledValue<T> lv) {
    int l = 0;
    while (lv != null) {
      ++l;
      lv = lv.next;
    }
    return l;
  }

  public T getValue(LabeledValue<T> lv, LabelList ll, T defaultValue) {
    while (lv != null) {
      if (lv.labelList == ll)
        return lv.value;
      lv = lv.next;
    }
    return defaultValue;
  }

  public LabeledValue<T> setValue(LabeledValue<T> lv, LabelList ll, T newValue) {
    T value = getValue(lv, ll, secret);
    if (value != secret) {
      if (lattice.equals(value, newValue))
        return lv;
      lv = removeValue(lv, ll);
    }
    return new LabeledValue<>(ll, newValue, lv);
  }

  protected LabeledValue<T> removeValue(LabeledValue<T> lv, LabelList ll) {
    if (lv.labelList == ll)
      return lv.next;
    return new LabeledValue<>(lv.labelList, lv.value, removeValue(lv.next, ll));
  }

  public LabeledValue<T> join(LabeledValue<T> in1, LabeledValue<T> in2, LabeledValue<T> out) {
    return merge(in1, in2, joinCombiner, null, out);
  }

  /**
   * Produce a merging of two labeled lists, using combine when labeled lists
   * from both sides have values. This combiner is often but not always join (
   * {@link #joinCombiner}). Since it isn't always join, the combiner will
   * sometimes be called with one argument bottom.
   * 
   * @param <U>
   *          Type of argument to pass to combiner
   * @param in1
   *          first labeled value to merge
   * @param in2
   *          second labeled value to merge
   * @param combiner
   *          combination function to run, no matter what.
   * @param arg
   *          argument to pass combiner
   * @param out
   *          previous result: reuse if possible.
   * @return
   */
  public <U> LabeledValue<T> merge(LabeledValue<T> in1, LabeledValue<T> in2, Combiner<T, U> combiner, U arg, LabeledValue<T> out) {
    if (in1 == null) {
      // calling "combine" is mandatory so we convert into a map.
      return map(in2, combiner.bindLeftBottom(), arg, out);
    } else if (in2 == null) {
      return map(in1, combiner.bindRightBottom(), arg, out);
    }
    LabelList ll = in1.labelList;
    T head;
    T v2 = getValue(in2, ll, secret); // get the value from the second labeled
                                      // values
    LabeledValue<T> lv1; // for recursive call, often tail
    LabeledValue<T> lv2;
    if (ll == in2.labelList) {
      // both inputs have label list first!
      // (Nice case)
      head = combiner.combine(in1.value, v2, arg);
      lv1 = in1.next;
      lv2 = in2.next;
    } else if (v2 == secret) {
      // second value doesn't have label from first list
      head = combiner.combine(in1.value, lattice.bottom(), arg);
      lv1 = in1.next;
      lv2 = in2;
    } else if (getValue(in1, in2.labelList, secret) == secret) {
      // first value doesn't have label from second list
      ll = in2.labelList;
      head = combiner.combine(lattice.bottom(), in2.value, arg);
      lv1 = in1;
      lv2 = in2.next;
    } else {
      // bad situation, the label lists are in opposite order.
      head = combiner.combine(in1.value, v2, arg);
      lv1 = in1.next;
      lv2 = removeValue(in2, ll); // conses up a new list :-(
    }
    // now we have head,ll,lv1,lv2 set up, we see if "out" matches what we want.
    if (out == null || out.labelList != ll && getValue(out, ll, secret) != secret) {
      return new LabeledValue<>(ll, head, merge(lv1, lv2, combiner, arg, null));
    }
    LabeledValue<T> tail;
    if (out.labelList != ll) {
      tail = merge(lv1, lv2, combiner, arg, out);
    } else {
      tail = merge(lv1, lv2, combiner, arg, out.next);
    }
    return makeLabeledValue(ll, head, tail, out);
  }

  public <U> LabeledValue<T> map(LabeledValue<T> in, UnaryOp<T, U> op, U arg, LabeledValue<T> out) {
    if (in == null)
      return null;
    T head = op.operate(in.value, arg);
    if (head == null) {
      return map(in.next, op, arg, out);
    }
    LabelList ll = in.labelList;
    if (out == null || out.labelList != ll && getValue(out, ll, secret) != secret) {
      return new LabeledValue<>(ll, head, map(in.next, op, arg, null));
    }
    LabeledValue<T> tail;
    if (out.labelList != ll) {
      tail = map(in.next, op, arg, out);
    } else {
      tail = map(in.next, op, arg, out.next);
    }
    return makeLabeledValue(ll, head, tail, out);
  }

  public <U> LabeledValue<T> labelMap(LabeledValue<T> in, LabelOp<U> op, U arg, LabeledValue<T> out) {
    if (in == null)
      return null;
    return labelMap2(in, op, arg, null, null, null, out);
    /*
     * // for now, do the easiest, space inefficient way if (in ==null) return
     * null; LabelList before = in.labelList; T value = in.value; LabelList
     * after = op.operate(before,arg); if (after == null) { return
     * labelMap(in.next,op,arg,out); } LabeledValue<T> tail; if (out == null ||
     * getValue(out,after,secret) == secret) { tail =
     * labelMap(in.next,op,arg,out); } else if (out.labelList == after) { tail =
     * labelMap(in.next,op,arg,out.next); } else { // space inefficient: tail =
     * labelMap(in.next,op,arg,removeValue(out,after)); } T newValue = value; T
     * prevValue = getValue(tail,after,secret); if (prevValue != secret) { //
     * need to merge: can be space inefficient tail = removeValue(tail,after);
     * newValue = lattice.join(newValue,prevValue); } return
     * makeLabeledValue(after,newValue,tail,out);
     */
  }

  public <U> LabeledValue<T> labelMap2(LabeledValue<T> in1, LabelOp<U> op1, U arg1, LabeledValue<T> in2, LabelOp<U> op2, U arg2,
      LabeledValue<T> out) {
    // TODO: improve efficiency if this causes lots of churning
    /*
     * so many different kinds of maps: tracked merge/demerge: guaranteed to
     * only filter, not merge values LabelTest filter, drop and maybe merge
     * PendingLabelStrip drop and maybe merge. How do we ensure that no memory
     * is allocated in quiescent case?
     */
    // for now, do the easiest, space inefficient way
    if (in1 == null)
      return labelMap(in2, op2, arg2, out);
    LabelList before = in1.labelList;
    T value = in1.value;
    LabelList after = op1.operate(before, arg1);
    if (after == null) {
      return labelMap2(in1.next, op1, arg1, in2, op2, arg2, out);
    }
    LabeledValue<T> tail;
    if (out == null || getValue(out, after, secret) == secret) {
      tail = labelMap2(in1.next, op1, arg1, in2, op2, arg2, out);
    } else if (out.labelList == after) {
      tail = labelMap2(in1.next, op1, arg1, in2, op2, arg2, out.next);
    } else {
      // space inefficient:
      tail = labelMap2(in1.next, op1, arg1, in2, op2, arg2, removeValue(out, after));
    }
    T newValue = value;
    T prevValue = getValue(tail, after, secret);
    if (prevValue != secret) {
      // need to merge: can be space inefficient
      tail = removeValue(tail, after);
      newValue = lattice.join(newValue, prevValue);
    }
    return makeLabeledValue(after, newValue, tail, out);
  }

  /**
   * @param ll
   * @param head
   * @param tail
   * @param out
   * @return
   */
  protected LabeledValue<T> makeLabeledValue(LabelList ll, T head, LabeledValue<T> tail, LabeledValue<T> out) {
    if (out != null && out.next == tail && lattice.equals(head, out.value)) {
      return out;
    }
    return new LabeledValue<>(ll, head, tail);
  }

  /**
   * Compare two labeled values and see if they are identical. For simplicity,
   * if the labels are in different order, the labeled values are considered
   * different. If you want to compare lattice equality, use equals
   * 
   * @param lv1
   * @param lv2
   * @return whether they are structurally identical
   * @see #equals(LabeledValue, LabeledValue)
   */
  public boolean identical(LabeledValue<T> lv1, LabeledValue<T> lv2) {
    while (lv1 != null && lv2 != null) {
      if (lv1 == lv2)
        return true;
      if (lv1.labelList != lv2.labelList || lv1.value != lv2.value)
        return false;
      lv1 = lv1.next;
      lv2 = lv2.next;
    }
    return lv1 == lv2;
  }

  public boolean equals(LabeledValue<T> lv1, LabeledValue<T> lv2) {
    return identical(lv1, lv2) || lessEq(lv1, lv2) && lessEq(lv2, lv1);
  }

  public boolean lessEq(LabeledValue<T> lv1, LabeledValue<T> lv2) {
    while (lv1 != null && lv2 != null) {
      if (lv1 == lv2)
        return true;
      LabelList ll = lv1.labelList;
      LabeledValue<T> nlv2;
      T v2;
      if (lv2.labelList == ll) {
        nlv2 = lv2;
        v2 = lv2.value;
      } else {
        nlv2 = lv2;
        v2 = getValue(lv2, ll, lattice.bottom());
      }
      if (!lattice.lessEq(lv1.value, v2)) {
        return false;
      }
      lv2 = nlv2;
      lv1 = lv1.next;
    }
    return lv1 == null;
  }

  public T joinAll(LabeledValue<T> lv) {
    if (lv == null)
      return lattice.bottom();
    T res = lv.value;
    for (lv = lv.next; lv != null; lv = lv.next) {
      res = lattice.join(res, lv.value);
    }
    return res;
  }

  public String toString(LabeledValue<T> lv) {
    if (lv == null)
      return "null";
    else
      return lv.toString(lattice);
  }
}
