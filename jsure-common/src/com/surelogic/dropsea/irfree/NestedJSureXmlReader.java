package com.surelogic.dropsea.irfree;

import com.surelogic.common.xml.*;

public abstract class NestedJSureXmlReader extends XmlReader {

  public static final String PROJECT_ATTR = "project";

  public static final String ROOT = "sea-snapshot";

  public static final String JAVA_DECL_INFO = "java-decl-info";
  public static final String PROPERTIES = "properties";
  public static final String DEPONENT = "deponent";
  public static final String DEPENDENT = "dependent";

  public static final String UID_ATTR = "uid";
  public static final String ID_ATTR = XmlReader.ID_ATTR;

  public static final String MESSAGE_ATTR = "message";

  public static final String DROP = "drop";
  public static final String PROOF_DROP = "proof-drop";
  public static final String PROMISE_DROP = "promise-drop";
  public static final String RESULT_DROP = "result-drop";
  public static final String HINT_DROP = "hint-drop";
  public static final String PROPOSED_PROMISE_DROP = "proposed-promise-drop";
  public static final String RESULT_FOLDER_DROP = "result-folder-drop";
  public static final String MODELING_PROBLEM_DROP = "modeling-problem-drop";
  public static final String CU_DROP = "cu-drop";
  public static final String PROJECTS_DROP = "projects-drop";
  public static final String METRIC_DROP = "metric-drop";

  public static final String MESSAGE = "message";
  public static final String MESSAGE_ID = "message-id";

  public static final String MESSAGE_ID_ATTR = "message-id";
  public static final String DROP_TYPE_ATTR = "drop-type";
  public static final String TYPE_ATTR = "type";
  public static final String FULL_TYPE_ATTR = "full-type";
  public static final String CATEGORY_ATTR = "category";
  public static final String JAVA_REF = "java-ref";
  public static final String DIFF_INFO = "diff-info";

  public static final String FILE_ATTR = "file";
  public static final String LINE_ATTR = "line";

  public static final String PATH_ATTR = "path";
  public static final String OFFSET_ATTR = "offset";
  public static final String LENGTH_ATTR = "length";

  public static final String DERIVED_FROM_SRC_ATTR = "derived-from-src";
  public static final String DERIVED_FROM_WARNING_ATTR = "derived-from-warning";
  public static final String PROVED_ATTR = "proved-consistent";
  public static final String USES_RED_DOT_ATTR = "uses-red-dot";

  public static final String FLAVOR_ATTR = "flavor";

  public static final String PROJECTS = "projects";

  public static final String HINT_TYPE_ATTR = "hint-type";

  public static final String VIRTUAL = "virtual";
  public static final String FROM_SRC = "from-src";
  public static final String CHECKED_BY_ANALYSIS = "checked-by-analysis";
  public static final String TO_BE_CHECKED_BY_ANALYSIS = "to-be-checked-by-analysis";
  public static final String ASSUMED = "assumed";
  public static final String CHECKED_BY_RESULTS = "checked-by-result";
  public static final String DEPENDENT_PROMISES = "dependent-promise";
  public static final String DEPONENT_PROMISES = "deponent-promise";

  public static final String HINT_ABOUT = "hint-about";

  public static final String PROPOSED_PROMISE = "proposed-promise";
  public static final String ANNOTATION_TYPE = "annotation-type";
  public static final String CONTENTS = "contents";
  public static final String REPLACED_ANNO = "replaced-annotation";
  public static final String REPLACED_CONTENTS = "replaced-contents";
  public static final String ORIGIN = "origin";
  public static final String JAVA_ANNOTATION = "java-annotation";
  public static final String FROM_INFO = "from-info";
  public static final String TARGET_INFO = "target-info";
  public static final String FROM_REF = "from-ref";
  public static final String ANNO_ATTRS = "annotation-attrs";
  public static final String REPLACED_ATTRS = "replaced-attrs";
  public static final String NO_ANNO_ATTRS = "no-annotation-attrs";
  public static final String NO_REPLACED_ATTRS = "no-replaced-attrs";

  public static final String CHECKED_PROMISE = "checked-promise";
  public static final String TRUSTED_PROOF_DROP = "trusted-proof-drop";

  public static final String TIMEOUT = "timeout";
  public static final String VOUCHED = "vouched";
  public static final String CONSISTENT = "consistent";
  public static final String FOLDER_LOGIC_OPERATOR = "folder-logic-operator";
  public static final String USED_BY_PROOF = "used-by-proof";

  public static final String METRIC = "metric";
  public static final String METRIC_INFO = "metric-info";

  /**
   * Constructs a new instance.
   * 
   * @param l
   *          the listener handling the top-level elements
   */
  protected NestedJSureXmlReader(IXmlResultListener l) {
    super(l);
  }

  /**
   * Constructs a new instance.
   */
  protected NestedJSureXmlReader() {
    super();
  }
}
