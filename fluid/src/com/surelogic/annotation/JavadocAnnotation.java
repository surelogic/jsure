package com.surelogic.annotation;

import java.util.List;

import com.surelogic.Immutable;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Holds a raw annotation string and its offset in the file read from an
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
 * 
 * This class defines the <i>annotation</i> to be <tt>RegionLock</tt>
 * (obtainable via {@link #getAnnotation()}) and the <i>argument</i> to be
 * <tt>&quot;SimpleLock is this protects Instance&quot;</tt> (obtainable via
 * {@link #getArgument()}&mdash;{@link #hasArgument()} flags if an argument
 * exists at all).
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

  /**
   * {@code null} if !{@link #f_isValid}.
   */
  @Nullable
  private final String f_annotation;
  /**
   * {@code null} if !{@link #f_isValid} or no argument on the annotation.
   */
  @Nullable
  private final String f_argument;
  @NonNull
  private final String f_raw;
  private final int f_offset;
  private final boolean f_isValid;

  /**
   * Constructs an instance from the Javadoc comment text.
   * 
   * @param annotationPlusExtra
   *          the annotation text plus extra characters until the next Javadoc
   *          tag was encountered in the comment. This string should strip off
   *          the <code>&#064;annotate</code> Javadoc tag.
   * @param offset
   *          of the tag in characters from the beginning of the file.
   */
  public JavadocAnnotation(String annotationPlusExtra, int offset) {
    if (annotationPlusExtra == null)
      throw new IllegalArgumentException(I18N.err(44, "annotationPlusExtra"));

    f_offset = offset;
    f_raw = annotationPlusExtra.trim();

    String annotation = null;
    String argument = null;

    final StringBuilder raw = new StringBuilder(f_raw);

    boolean handled = false;
    final int openP = raw.indexOf("(");
    annotation = raw.substring(0, openP);
    if (openP != -1 && SLUtility.isValidJavaIdentifier(annotation)) {
      /*
       * Seems to be an annotation with arguments. Find the closing parenthesis,
       * while handling string literals (so we don't id a closing parenthesis in
       * a string literal).
       */
      char last = '(';
      boolean inStringLiteral = false;
      int closeP = -1;
      for (int i = openP; i < raw.length(); i++) {
        char now = raw.charAt(i);
        if (!inStringLiteral && now == '\"')
          inStringLiteral = true;
        else if (inStringLiteral && now == '\"') {
          if (last != '\\')
            inStringLiteral = false;
        } else if (!inStringLiteral && now == ')') {
          closeP = i;
          break;
        }
        last = now;
      }
      if (closeP != -1) {
        argument = raw.substring(openP + 1, closeP);
        handled = true;
      }
    }
    if (!handled) {
      annotation = null;
      /*
       * Seems to be a simple annotation with no arguments -- pull off the
       * longest valid Java identifier we find.
       */
      int valid = -1;
      for (int i = 1; i < raw.length(); i++) {
        final String now = raw.substring(0, i);
        if (SLUtility.isValidJavaIdentifier(now))
          valid = i;
        else
          break;
      }
      if (valid != -1) {
        annotation = raw.substring(0, valid);
        handled = true;
      }
    }
    f_annotation = annotation;
    f_argument = argument;
    f_isValid = handled;
  }

  public static void main(String[] args) {
    JavadocAnnotation j = new JavadocAnnotation(".RegionEffects(\"none\")sdfsd fdsfsd", 5);
    System.out.println(j.getOffset());
    System.out.println(j.isValid());
    System.out.println(j.getRawCommentText());
    System.out.println(j.getAnnotation());
    System.out.println(j.hasArgument());
    System.out.println(j.getArgument());
    System.out.println(j.toString());
  }

  /**
   * Gets the Javadoc comment text passed to construct this instance.
   * 
   * @return the Javadoc comment text passed to construct this instance.
   */
  @NonNull
  public String getRawCommentText() {
    return f_raw;
  }

  /**
   * The annotation, or <tt>""</tt> if !{@link #isValid()}.
   * <p>
   * For example:
   * 
   * <pre>
   * /**
   *  * @annotate RegionLock(&quot;SimpleLock is this protects Instance&quot;)
   *  &#42;/
   * class Simple { ... }
   * </pre>
   * 
   * This class defines the <i>annotation</i> to be <tt>RegionLock</tt>.
   * 
   * @return the annotation, or <tt>""</tt> if !{@link #isValid()}.
   */
  @Nullable
  public String getAnnotation() {
    return f_annotation == null ? "" : f_annotation;
  }

  /**
   * The argument, or <tt>""</tt> if !{@link #isValid()} or there is no
   * argument. To check if there is an argument use {@link #hasArgument()}.
   * <p>
   * For example:
   * 
   * <pre>
   * /**
   *  * @annotate RegionLock(&quot;SimpleLock is this protects Instance&quot;)
   *  &#42;/
   * class Simple { ... }
   * </pre>
   * 
   * This class defines the the <i>argument</i> to be
   * <tt>&quot;SimpleLock is this protects Instance&quot;</tt> (the quotes are
   * included in the returned string).
   * 
   * @return the argument, or <tt>""</tt> if !{@link #isValid()} or there is no
   *         argument.
   */
  @Nullable
  public String getArgument() {
    return f_argument == null ? "" : f_argument;
  }

  /**
   * Indicates that {@link #getAnnotation()} will return a valid Java identifier
   * and {@link #getArgument()} will return the complete argument text (if any).
   * 
   * @return {@code true} if the annotation and optional arguement were
   *         processed okay (may still not parse, but that will be checked
   *         later), {@code false} otherwise.
   */
  public boolean isValid() {
    return f_isValid;
  }

  /**
   * Checks if this annotation has an argument.
   * 
   * @return {@code} if this annotation has an argument that will be returned
   *         from {@link #getArgument()}.
   */
  public boolean hasArgument() {
    return f_argument != null;
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
    final StringBuilder b = new StringBuilder("JavadocAnnotation(");
    if (f_isValid) {
      b.append(f_annotation);
      if (f_argument != null) {
        b.append('(');
        b.append(f_argument);
        b.append(')');
      }
    } else {
      b.append("NONSENSE: ");
      b.append(f_raw);
    }
    b.append(')');
    return b.toString();
  }
}
