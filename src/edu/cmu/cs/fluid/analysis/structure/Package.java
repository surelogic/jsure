package edu.cmu.cs.fluid.analysis.structure;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An abstraction of a Java package.
 * 
 * @author T.J. Halloran
 */
public final class Package {

  static private final Package ROOT = new Package(".", null);

  /**
   * The package name.
   */
  private final String f_name;

  /**
   * Reference to this package's enclosing package. May be <code>null</code>
   * if the enclosing package is the default package.
   */
  private final Package f_parent;

  /**
   * Map to this package's subpackages using the subpackage name as its key.
   */
  private Map<String, Package> f_nameToSubpackage = new HashMap<String, Package>();

  private Package(final String name, final Package parent) {
    assert (name != null);
    f_name = name;
    f_parent = parent;
  }

  /**
   * Returns the {@link Package} instance corresponding to the specified fully
   * qualified package name.
   * 
   * @param fullyQualifiedPackageName
   *          the fully qualified package name, for example,
   *          <code>java.util</code>.
   * @return the object instance representing the specified fully qualified
   *         package.
   */
  public Package getInstance(String fullyQualifiedPackageName) {
    return null;
  }

  public static Package getDefaultPackage() {
    return ROOT;
  }

  /**
   * Returns the {@link Package} instance corresponding to the specified
   * subpackage of this package.
   * 
   * @param simpleName
   *          the innermost simple name of the subpackage.
   * @return the object instance representing the subpackage.
   */
  public Package getSubpackage(String simpleName) {
    assert (simpleName != null);
    Package result = f_nameToSubpackage.get(simpleName);
    if (result == null) {
      result = new Package(simpleName, this);
      f_nameToSubpackage.put(simpleName, result);
    }
    return result;
  }

  /**
   * Returns the innermost simple name of this package. For example, if this
   * object represented the <code>java.util</code> package, then the result
   * would be <code>util</code>.
   * 
   * @return the innermost simple name of this package.
   */
  public String getSimpleName() {
    return f_name;
  }

  /**
   * Returns, from outer to inner, the packages that enclose this package. For
   * example, if this object represented the <code>java.util.logging</code>
   * package, then the result would be the list containing the {@link Package}
   * instances corresponding to <code>.</code>, <code>java</code>,
   * <code>java.util</code>, and <code>java.util.logging</code>.
   * 
   * @return An ordered list package objects, inner to outer, representing this
   *         package's fully qualified name.
   */
  public List<Package> getSimpleNameList() {
    LinkedList<Package> result = new LinkedList<Package>();
    for (Package p = this; p != null; p = p.f_parent) {
      result.addFirst(p);
    }
    return result;
  }

  /**
   * Returns the fully qualified name of this package. For example,
   * <code>java.util</code>.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    boolean first = true;
    for (Package p : getSimpleNameList()) {
      if (!first)
        b.append(".");
      else
        first = false;
      b.append(p.getSimpleName());
    }
    return b.toString();
  }
}
