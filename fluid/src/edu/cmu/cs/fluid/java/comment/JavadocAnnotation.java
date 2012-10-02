package edu.cmu.cs.fluid.java.comment;

import java.util.List;

import com.surelogic.Immutable;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Holds a raw annotation string and its offset in the file readn from an
 * annotation may placed in Javadoc. This feature can be useful for Java 1.4
 * code which does not include language support for annotations, via the
 * <code>&#064;annotate</code> tag.
 * 
 * <pre>
 * /**
 *  * @annotate RegionLock(&quot;SimpleLock is this protects Instance&quot;)
 *  &#42;/
 * class Simple { ... }
 * </pre>
 */
@Immutable
public final class JavadocAnnotation {

  /**
   * Fluid IR node that defines the type used by the {@link SlotInfo} for
   * holding a list of these annotations.
   */
  public static final IRObjectType<List<JavadocAnnotation>> FLUID_JAVADOC_REF_SLOT_TYPE = new IRObjectType<List<JavadocAnnotation>>();

  /**
   * Fluid IR name used the {@link SlotInfo} for holding a list of these
   * annotations.
   */
  public static final String JAVADOC_REF_SLOT_NAME = "JavaNode.JavadocAnnotations";

  private final String f_annotation;
  private final int f_offset;

  /**
   * Constructs an instance.
   * 
   * @param annotationPlusExtra
   *          the annotation text plus extra characters until the next Javadoc
   *          tag was encountered in the comment.
   * @param offset
   *          of the tag in characters from the beginning of the file.
   */
  public JavadocAnnotation(String annotationPlusExtra, int offset) {
    if (annotationPlusExtra == null)
      throw new IllegalArgumentException(I18N.err(44, "annotationPlusExtra"));
    f_annotation = annotationPlusExtra;
    f_offset = offset;
  }

  /**
   * Gets the annotation text plus extra characters until the next Javadoc tag
   * was encountered in the comment.
   * 
   * @return the annotation text plus extra characters until the next Javadoc
   *         tag was encountered in the comment.
   */
  public String getAnnotation() {
    return f_annotation;
  }

  /**
   * Gets the offset of the tag in characters from the beginning of the file.
   * 
   * @return the offset of the tag in characters from the beginning of the file.
   */
  public int getOffset() {
    return f_offset;
  }

  @Override
  public String toString() {
    return "JavadocAnnotation(value=\"" + f_annotation + "\", offset=" + f_offset + ")";
  }
}
