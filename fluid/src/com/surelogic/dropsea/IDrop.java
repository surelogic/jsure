package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.irfree.drops.IRFreeDrop;

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

  /**
   * Returns a canonical version of the analysis result typically used for
   * comparisons in the regression test suite.
   * <p>
   * The resulting string is constructed by calling
   * {@link I18N#resc(int, Object...)} if this drop's message was constructed
   * using {@link I18N}. For example, {@code I18N.resc(2001, "foo", 5)} returns
   * <tt>"(2001,foo,5)"</tt>.
   * <p>
   * If this drop's message was not constructed using {@link I18N} than the
   * result of this call is {@code null}.
   * 
   * @return a canonical version of the message describing this drop, usually
   *         used by the regression test suite for comparisons.
   * @see I18N#resc(int)
   * @see I18N#resc(int, Object...)
   */
  @Nullable
  String getMessageCanonical();

  /**
   * Gets this drop's categorizing message, or {@code null} if none.
   * <p>
   * It is intended that {@link I18N#toStringForUIFolderLabel(String, int)} is
   * called prior to display on the string returned from this method.
   * 
   * @return a categorizing string, or {@code null} if none.
   * @see I18N#toStringForUIFolderLabel(String, int)
   */
  @Nullable
  String getCategorizingMessage();

  /**
   * Gets a reference to the Java code this information is about, or
   * {@code null} if none.
   * 
   * @return a reference to the Java code this information is about, or
   *         {@code null} if none.
   */
  @Nullable
  IJavaRef getJavaRef();

  /**
   * Gets the set of proposed promises for this drop. The returned list is a
   * copy and may be modified.
   * 
   * @return the possibly empty set of proposed promises for this drop. The
   *         returned list is a copy and may be modified.
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
  long getTreeHash();

  /**
   * Computes a hash of the location in the fAST node that this drop is related
   * to.
   * 
   * @return a hash.
   */
  long getContextHash();
}
