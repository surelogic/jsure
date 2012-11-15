package com.surelogic.dropsea.irfree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.Utility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.ModelingProblemDrop;

@Utility
public final class DropTypeUtility {

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

  private static final Map<String, Class<?>> NAME_TO_CLASS = new HashMap<String, Class<?>>();

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
   * List of how of drop type names have changed. For backwards scan
   * compatibility.
   * 
   * The string is matched at the end of the fully qualified type and then the
   * class name replaces
   */
  private static final String[][] OLDSUFFIX_TO_NEWNAME = { { "PromiseWarningDrop", ModelingProblemDrop.class.getName() },
      { "InfoDrop", HintDrop.class.getName() }, { "WarningDrop", HintDrop.class.getName() },
      { "AnalysisHintDrop", HintDrop.class.getName() },
      { "ProjectsDrop", Drop.class.getName() }};

  /**
   * A list of types that use to be in drop-sea and are in persisted scans, but
   * no longer are used. This just helps to avoid lots of warnings. For
   * backwards scan compatibility.
   */
  private static String[] obsoleteTypes = { "com.surelogic.analysis.AbstractWholeIRAnalysis$ResultsDepDrop" };

  /**
   * Root package where all drops exist.
   */
  private static String ROOT_DROP_PACKAGE = "com.surelogic.dropsea.ir.";
  /**
   * Sub-packages (appeneded to {@link #ROOT_DROP_PACKAGE} to form a package
   * name) where drops exist.
   */
  private static String[] SUB_DROP_PACKAGES = { "drops.", "drops.layers.", "drops.locks.", "drops.method.constraints.",
      "drops.nullable.", "drops.type.constraints.", "drops.uniqueness.", "drops.modules.", "drops.threadroles.", };

  private static Collection<String> getPossibleClassNames(String simpleClassName) {
    Collection<String> result = new ArrayList<String>();
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

}
