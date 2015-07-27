package com.surelogic.dropsea.irfree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.Utility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.DropType;

@Utility
public final class DropTypeUtility {
  private DropTypeUtility() {
    // To prevent instantiation
  }

  /**
   * Used to lookup types in the IR drop-sea. If the type moved or has changed
   * this method trys to map the name to the new type in the code. This method
   * is used for mapping type names in scans to actual types on the classpath.
   * 
   * @param className
   *          the fully-qualified name of a drop in the IR drop-sea.
   * @return the type on the classpath, or {@code null} if it can't be found.
   */
  public static Class<?> findType(String className) {
    if (className == null)
      return null;

    final String origName = className;
    /*
     * Try to find the full type name in our cache or on the classpath. This is
     * the most common case so we try it first. Everything else is for backwards
     * scan compatibility.
     */
    Class<?> result = forNameOrNull(className);
    if (result != null)
      return result;

    /*
     * Handle classes we changed the names of
     */
    for (String[] old2new : OLDSUFFIX_TO_NEWNAME) {
      final String oldSuffix = old2new[0];
      final String newTypeName = old2new[1];
      if (className.endsWith(oldSuffix)) {
        className = newTypeName;
        // try lookup now
        result = forNameOrNull(className);
        if (result != null)
          return cacheMapping(origName, result);
      }
    }

    /*
     * Check known packages using the simple name of the type.
     */
    String simpleName = getSimpleName(className);
    for (String possibleClassName : getPossibleClassNames(simpleName)) {
      result = forNameOrNull(possibleClassName);
      if (result != null)
        return cacheMapping(origName, result);
    }

    /*
     * Check if we know this type is no longer in the system.
     */
    if (!Arrays.asList(obsoleteTypes).contains(className)) {
      SLLogger.getLogger().warning("  Unknown class type: " + className);
    }
    return null;
  }

  private static final Map<String, Class<?>> NAME_TO_CLASS = new HashMap<>();

  private static void ensureClassMapping(Class<?> cls) {
    if (NAME_TO_CLASS.containsKey(cls.getName())) {
      return;
    }
    cacheMapping(cls.getSimpleName(), cls);
    cacheMapping(cls.getName(), cls);
  }

  private static Class<?> cacheMapping(String orig, Class<?> cls) {
    NAME_TO_CLASS.put(orig, cls);
    return cls;
  }

  /**
   * Root package where all drops exist.
   */
  private static String ROOT_DROP_PACKAGE = "com.surelogic.dropsea.ir.";

  private static final String DROP = ROOT_DROP_PACKAGE + "Drop";
  private static final String HINT = ROOT_DROP_PACKAGE + "HintDrop";
  private static final String PROBLEM = ROOT_DROP_PACKAGE + "ModelingProblemDrop";
  private static final String METRIC = ROOT_DROP_PACKAGE + "MetricDrop";
  private static final String PROPOSAL = ROOT_DROP_PACKAGE + "ProposedPromiseDrop";
  private static final String RESULT = ROOT_DROP_PACKAGE + "ResultDrop";
  private static final String RESULT_FOLDER = ROOT_DROP_PACKAGE + "ResultFolderDrop";
  private static final String ASSUME = ROOT_DROP_PACKAGE + "drops.AssumePromiseDrop";
  private static final String SCOPED_PROMISE = ROOT_DROP_PACKAGE + "drops.PromisePromiseDrop";
  private static final String PROMISE = "PromiseDrop";
  private static final String MODEL = "Model";

  /**
   * List of how of drop type names have changed. For backwards scan
   * compatibility.
   * 
   * The string is matched at the end of the fully qualified type and then the
   * class name replaces
   */
  private static final String[][] OLDSUFFIX_TO_NEWNAME = { { "PromiseWarningDrop", PROBLEM }, { "InfoDrop", HINT },
	  { "UniquenessControlFlowDrop", RESULT_FOLDER },
      { "WarningDrop", HINT }, { "AnalysisHintDrop", HINT }, { "ProjectsDrop", DROP } };

  /**
   * A list of types that use to be in drop-sea and are in persisted scans, but
   * no longer are used. This just helps to avoid lots of warnings. For
   * backwards scan compatibility.
   */
  private static String[] obsoleteTypes = { 
	  //"com.surelogic.dropsea.ir.drops.uniqueness.UniquenessControlFlowDrop",
	  "com.surelogic.analysis.AbstractWholeIRAnalysis$ResultsDepDrop",
  };

  /**
   * Sub-packages (appeneded to {@link #ROOT_DROP_PACKAGE} to form a package
   * name) where drops exist.
   */
  private static String[] SUB_DROP_PACKAGES = { "drops.", "drops.layers.", "drops.locks.", "drops.method.constraints.",
      "drops.nullable.", "drops.type.constraints.", "drops.uniqueness.", "drops.modules.", "drops.threadroles.", };

  private static Collection<String> getPossibleClassNames(String simpleClassName) {
    Collection<String> result = new ArrayList<>();
    result.add(ROOT_DROP_PACKAGE + simpleClassName);
    for (String subPkg : SUB_DROP_PACKAGES) {
      result.add(ROOT_DROP_PACKAGE + subPkg + simpleClassName);
    }
    return result;
  }

  private static String getSimpleName(String className) {
    int index = className.lastIndexOf(".");
    if (index == -1)
      return className;
    index++;
    if (index >= className.length())
      return "";
    final String simpleName = className.substring(index);
    return simpleName;
  }

  private static Class<?> forNameOrNull(String className) {
    /*
     * Try the cache
     */
    Class<?> result = NAME_TO_CLASS.get(className);
    if (result != null)
      return result;
    /*
     * Try the classpath
     */
    try {
      result = Class.forName(className);
      if (result != null) {
        ensureClassMapping(result);
        return result;
      }
    } catch (ClassNotFoundException ignore) {
      // Keep going
    }
    return null;
  }

  public static DropType computeDropType(String className) {
    if (className == null)
      return null;

    final String origName = className;
    /*
     * Try to find the full type name in our cache
     */
    DropType result = NAME_TO_TYPE.get(className);
    if (result != null)
      return result;

    final String simpleName = getSimpleName(className);
    if (className.startsWith(ROOT_DROP_PACKAGE) && (simpleName.endsWith(PROMISE) || simpleName.endsWith(MODEL))) {
      return cacheTypeMapping(origName, DropType.PROMISE);
    }

    /*
     * Handle classes we changed the names of
     */
    for (String[] old2new : OLDSUFFIX_TO_NEWNAME) {
      final String oldSuffix = old2new[0];
      final String newTypeName = old2new[1];
      if (className.endsWith(oldSuffix)) {
        className = newTypeName;
        // try lookup now
        result = NAME_TO_TYPE.get(className);
        if (result != null)
          return cacheTypeMapping(origName, result);
      }
    }

    /*
     * Check known packages using the simple name of the type.
     */
    for (String possibleClassName : getPossibleClassNames(simpleName)) {
      result = NAME_TO_TYPE.get(possibleClassName);
      if (result != null)
        return cacheTypeMapping(origName, result);
    }

    /*
     * Check if we know this type is no longer in the system.
     */
    if (!Arrays.asList(obsoleteTypes).contains(className)) {
      SLLogger.getLogger().warning("  Unknown class type: " + className);
    }
    return null;
  }

  private static final Map<String, DropType> NAME_TO_TYPE = new HashMap<>();
  static {
    NAME_TO_TYPE.put(DROP, DropType.OTHER);
    NAME_TO_TYPE.put(HINT, DropType.HINT);
    NAME_TO_TYPE.put(METRIC, DropType.METRIC);
    NAME_TO_TYPE.put(PROBLEM, DropType.MODELING_PROBLEM);
    NAME_TO_TYPE.put(PROPOSAL, DropType.PROPOSAL);
    NAME_TO_TYPE.put(RESULT, DropType.RESULT);
    NAME_TO_TYPE.put(RESULT_FOLDER, DropType.RESULT_FOLDER);
    NAME_TO_TYPE.put(ASSUME, DropType.SCOPED_PROMISE);
    NAME_TO_TYPE.put(SCOPED_PROMISE, DropType.SCOPED_PROMISE);
  }

  private static DropType cacheTypeMapping(String origName, DropType result) {
    NAME_TO_TYPE.put(origName, result);
    return result;
  }
  
  private static final Map<String, String> NAME_TO_NAME = new HashMap<>();
  
  public static String mapFullName(final String className) {
	  String rv = NAME_TO_NAME.get(className);
	  if (rv != null) {
		  return rv;
	  }
	  // Couldn't find a mapping cached	  
	  /*
	   * Handle classes we changed the names of
	   */
	  for (String[] old2new : OLDSUFFIX_TO_NEWNAME) {
		  final String oldSuffix = old2new[0];
		  final String newTypeName = old2new[1];
		  if (className.endsWith(oldSuffix)) {
			  rv = newTypeName;
		  }
	  }
	  if (rv == null) {
		  rv = className;
	  }
	  NAME_TO_NAME.put(className, rv);
	  return rv;
  }
}
