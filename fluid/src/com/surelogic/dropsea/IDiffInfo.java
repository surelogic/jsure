package com.surelogic.dropsea;

import com.surelogic.Immutable;
import com.surelogic.NonNull;
import com.surelogic.ValueObject;

@Immutable
@ValueObject
public interface IDiffInfo {

  /*
   * Key constants for diff-info values used by JSure
   */

  public static final String FAST_TREE_HASH = "fAST-tree-hash";
  public static final String FAST_CONTEXT_HASH = "fAST-context-hash";
  /**
   * Offset from the first statement/expression/decl in the enclosing declaration
   * (unless it's a parameter, in which case it's from the start of the enclosing
   * method)
   */
  public static final String DECL_RELATIVE_OFFSET = "decl-relative-offset";
  /**
   * Offset from the end of the text representing the enclosing declaration
   */
  public static final String DECL_END_RELATIVE_OFFSET = "decl-end-relative-offset";

  public static final int UNKNOWN = -1;
  
  /*
   * Interface to key/value pairs.
   */

  @NonNull
  String getKey();

  @NonNull
  String getValueAsString();

  long getValueAsLong(long valueIfNotRepresentable);

  int getValueAsInt(int valueIfNotRepresentable);

  @NonNull
  String encodeForPersistence();
}
