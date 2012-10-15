package edu.cmu.cs.fluid.java;

import com.surelogic.Nullable;
import com.surelogic.ThreadSafe;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef.Builder;

/**
 * Interface for code references within the JSure analysis and infrastructure.
 * This interface adds to the standard {@link IJavaRef} with items that <b>do
 * not</b> need to be transferred to the IR-free drop-sea and the Eclipse user
 * interface. *
 * <p>
 * Concrete instances or this interface are constructed using
 * {@link FluidJavaRef.Builder}&mdash;copy-and-modify is supported via
 * {@link Builder#Builder(IFluidJavaRef)}.
 */
@ThreadSafe
public interface IFluidJavaRef extends IJavaRef {

  /**
   * Gets the absolute path that this reference is within, or {@code null} if
   * none is available.
   * <p>
   * This resource may not exist on the filesystem anymore.
   * 
   * @return an absolute path that this reference is within, or {@code null} if
   *         none is available.
   */
  @Nullable
  String getAbsolutePathOrNull();

  /**
   * Gets the path within the <tt>.jar</tt> file returned by
   * {@link #getAbsolutePathOrNull()} that this reference is within. This method
   * returns {@code null} if this reference is not within a
   * {@link Within#JAR_FILE}.
   * 
   * @return the path that this reference is within inside the the <tt>.jar</tt>
   *         file returned by {@link #getAbsolutePathOrNull()}, or {@code null}
   *         if not within a <tt>.jar</tt> file.
   */
  @Nullable
  String getJarRelativePathOrNull();
}
