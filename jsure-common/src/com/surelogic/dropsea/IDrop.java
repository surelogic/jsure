package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;

/**
 * The interface for for all drops within the sea, intended to allow multiple
 * implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IDrop {

  /**
   * Returns the general type of this drop.
   * 
   * @return the general type of this drop.
   */
  @NonNull
  DropType getDropType();

  /**
   * Returns the simple name of the underlying IR drop class as given in the
   * source code. For the IR dropsea this is implemented as
   * {@code return getClass().getSimpleName();}
   * <p>
   * For the IR-free dropsea it is implemented as a string lookup (passed from
   * the XML snapshot).
   * 
   * @return the simple name of the underlying IR drop class as given in the
   *         source code.
   * 
   * @see Class#getSimpleName()
   */
  @NonNull
  String getSimpleClassName();

  /**
   * Returns the name of the underlying IR drop class as given in the source
   * code. For the IR dropsea this is implemented as
   * {@code return getClass().getName();}
   * <p>
   * For the IR-free dropsea it is implemented as a string lookup (passed from
   * the XML snapshot).
   * 
   * @return the name of the underlying IR drop class as given in the source
   *         code.
   * 
   * @see Class#getName()
   */
  @NonNull
  String getFullClassName();

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
   * Returns if this information is from or about source code or another
   * location, such as a binary or configuration file.
   * 
   * @return {@code true} if this information is from or about source code,
   *         {@code false} otherwise
   */
  boolean isFromSrc();

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

  /**
   * Returns the diff-info value to which the specified key is mapped as a
   * {@link IJavaRef}, or throws IllegalArgumentException if this drop contains
   * no mapping for the key or the diff-info value cannot be represented as a
   * {@link IJavaRef}.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @return the diff-info value to which the specified key is mapped as a
   *         {@link IJavaRef}.
   * @throws IllegalArgumentException
   *           if this drop contains no mapping for the key or the diff-info
   *           value cannot be represented as a {@link IJavaRef}.
   */
  IJavaRef getDiffInfoAsJavaRefOrThrow(String key);

  /**
   * Returns the diff-info value to which the specified key is mapped as a
   * {@link IJavaRef}, or {code null} if this drop contains no mapping for the
   * key or the diff-info value cannot be represented as a {@link IJavaRef}.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @return the diff-info value to which the specified key is mapped as a
   *         {@link IJavaRef}, or {code null} if this drop contains no mapping
   *         for the key or the diff-info value cannot be represented as a
   *         {@link IJavaRef}.
   */
  IJavaRef getDiffInfoAsJavaRefOrNull(String key);

  /**
   * Returns the diff-info value to which the specified key is mapped as a
   * {@link IDecl}, or throws IllegalArgumentException if this drop contains no
   * mapping for the key or the diff-info value cannot be represented as a
   * {@link IDecl}.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @return the diff-info value to which the specified key is mapped as a
   *         {@link IDecl}.
   * @throws IllegalArgumentException
   *           if this drop contains no mapping for the key or the diff-info
   *           value cannot be represented as a {@link IDecl}.
   */
  IDecl getDiffInfoAsDeclOrThrow(String key);

  /**
   * Returns the diff-info value to which the specified key is mapped as a
   * {@link IDecl}, or {code null} if this drop contains no mapping for the key
   * or the diff-info value cannot be represented as a {@link IDecl}.
   * <p>
   * Diff-info values are used to heuristically match drop instances, with the
   * overall purpose of creating a "diff" between two scans. They are used to
   * highlight changes from one scan to another in the JSure user interface as
   * well as by the JSure regression test suite to match scan results with a set
   * of "oracle" results.
   * 
   * @param key
   *          a key.
   * @return the diff-info value to which the specified key is mapped as a
   *         {@link IDecl}, or {code null} if this drop contains no mapping for
   *         the key or the diff-info value cannot be represented as a
   *         {@link IDecl}.
   */
  IDecl getDiffInfoAsDeclOrNull(String key);
  
  /**
   * Returns whether we need to diff this drop
   */
  boolean includeInDiff();
}
