package com.surelogic.dropsea.irfree;

public interface DiffHeuristics {
  /*
   * Key constants for diff-info values used by JSure
   */

  public static final String FAST_TREE_HASH = "fAST-tree-hash";
  public static final String FAST_CONTEXT_HASH = "fAST-context-hash";
  /**
   * Offset from the first statement/expression/decl in the enclosing
   * declaration (unless it's a parameter, in which case it's from the start of
   * the enclosing method)
   */
  public static final String DECL_RELATIVE_OFFSET = "decl-relative-offset";
  /**
   * Offset from the end of the text representing the enclosing declaration
   */
  public static final String DECL_END_RELATIVE_OFFSET = "decl-end-relative-offset";

  /**
   * A string sent from the analysis, very rarely, to indicate that even though
   * everything else matches the drop might not really be the same&mdash;this
   * value also needs to be compared.
   * <p>
   * Use of this is rare and typically only for analysis result drops, however,
   * it can be used on any drop at all.
   */
  public static final String ANALYSIS_DIFF_HINT = "analysis-diff-hint";

  public static final int UNKNOWN = -1;
}
