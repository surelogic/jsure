package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.irfree.drops.IRFreeDrop;

import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * The interface for for all drops within the sea, intended to allow multiple
 * implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IDrop {

  /**
   * Gets the IR drop-sea type, descended from {@link Drop}, even if this is an
   * IR-free drop, descended from {@link IRFreeDrop}.
   * <p>
   * In the IR drop-sea this method simply returns <tt>getClass()</tt>. In the
   * IR-free drop-sea this method returns the {@link Class} saved, by name, in
   * the sea snapshot file that corresponds to the IR drop-sea drop type of this
   * drop before it was persisted.
   * 
   * @return the IR drop-sea type, descended from {@link Drop}.
   */
  @NonNull
  Class<?> getIRDropSeaClass();

  /**
   * Checks if this drop, in the IR drop-sea, is an instance of the passed
   * class. Typically this replaces code like: <i>receiver</i>
   * <tt>instanceof</tt> <i>type</i>. This allows type checks in the IR-free
   * drop-sea used for saving and restoring.
   * <p>
   * If you are comparing an instance of the IR drop-sea using this method the
   * code is a shortcut for <tt>type.isInstance(this)</tt>. If you are comparing
   * an instance of the IR-free drop-sea this method is a shortcut for
   * <tt>type.isAssignableFrom({@link #getIRDropSeaClass()})</tt>
   * 
   * @param type
   *          in the IR drop-sea, descended from {@link Drop}.
   * @return {@code true} if this drop, in the IR drop-sea, is an instance of
   *         the passed type, {@code false} otherwise.
   */
  boolean instanceOfIRDropSea(Class<?> type);

  /**
   * Gets this drop's message. If no message has been set then the output will
   * look like <tt>"</tt><i>SimpleTypeName</i><tt> (EMPTY)</tt>.
   * 
   * @return a message describing this drop, usually used by the UI.
   */
  @NonNull
  String getMessage();

  @NonNull
  String getMessageCanonical();

  /**
   * Gets this drop's categorizing string, or {@code null} if none.
   * 
   * @return a categorizing string, or {@code null} if none.
   */
  @Nullable
  String getCategorizingString();

  /**
   * Gets the source reference of this drop.
   * 
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  @Nullable
  ISrcRef getSrcRef();

  /**
   * Gets the set of proposed promises for this drop.
   * 
   * @return the, possibly empty but non-null, set of proposed promises for this
   *         drop.
   */
  @NonNull
  Collection<? extends IProposedPromiseDrop> getProposals();

  /**
   * Returns the set of analysis hints about this proof drop.
   * 
   * @return the set of analysis hints about this proof drop.
   */
  @NonNull
  Collection<? extends IHintDrop> getHints();

  /**
   * Computes a hash of the subtree from the fAST node that this drop is related
   * to the fAST branches.
   * 
   * @return a hash.
   */
  Long getTreeHash();

  /**
   * Computes a hash of the location in the fAST node that this drop is related
   * to.
   * 
   * @return a hash.
   */
  Long getContextHash();

  /**
   * Gets the XML element name for this drop.
   * 
   * @return the XML element name for this drop.
   */
  @Deprecated
  String getXMLElementName();
}
