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
   * Returns the set of analysis hints about this drop.
   * 
   * @return the set of analysis hints about this drop.
   */
  @NonNull
  Collection<? extends IHintDrop> getHints();

  /**
   * Checks if this drop has a mapping to a diff-info value for the passed key.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @return {@code true} if a value exists for <tt>key</tt>, {@code false}
   *         otherwise.
   */
  boolean containsDiffInfoKey(String key);

  /**
   * Returns the diff-info value to which the specified key is mapped, or
   * {@code null} if this drop contains no mapping for the key. This method will
   * return a string version of the diff-info value for any mapping&mdash;even
   * if the value is an int or a long.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @return the diff-info value to which the specified key is mapped, or
   *         {@code null} if this drop contains no mapping for the key.
   */
  String getDiffInfoOrNull(String key);

  /**
   * Returns the diff-info value to which the specified key is mapped as a long,
   * or <tt>valueIfNotRepresentable</tt> if this drop contains no mapping for
   * the key or the diff-info value cannot be represented as a long.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @param valueIfNotRepresentable
   *          a value.
   * @return the diff-info value to which the specified key is mapped as a long,
   *         or <tt>valueIfNotFound</tt> if this drop contains no mapping for
   *         the key or the diff-info value cannot be represented as a long.
   */
  long getDiffInfoAsLong(String key, long valueIfNotRepresentable);

  /**
   * Returns the diff-info value to which the specified key is mapped as an int,
   * or <tt>valueIfNotRepresentable</tt> if this drop contains no mapping for
   * the key or the diff-info value cannot be represented as an int. For
   * example, if the diff-info value was set as a long then this method will
   * return its value if it fits into an int, or return <tt>valueIfNotFound</tt>
   * if it doesn't.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @param valueIfNotRepresentable
   *          a value.
   * @return the diff-info value to which the specified key is mapped as an int,
   *         or <tt>valueIfNotFound</tt> if this drop contains no mapping for
   *         the key or the diff-info value cannot be represented as an int.
   */
  int getDiffInfoAsInt(String key, int valueIfNotRepresentable);

  /**
   * Returns the diff-info value to which the specified key is mapped as an
   * enum, or <tt>valueIfNotRepresentable</tt> if this drop contains no mapping
   * for the key or the diff-info value cannot be represented as an enum.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @param valueIfNotRepresentable
   *          a value.
   * @param elementType
   *          the class object of the element type for the resulting enum.
   * @return the diff-info value to which the specified key is mapped as an
   *         enum, or <tt>valueIfNotFound</tt> if this drop contains no mapping
   *         for the key or the diff-info value cannot be represented as an
   *         enum.
   */
  <T extends Enum<T>> T getDiffInfoAsEnum(String key, T valueIfNotRepresentable, Class<T> elementType);
}
