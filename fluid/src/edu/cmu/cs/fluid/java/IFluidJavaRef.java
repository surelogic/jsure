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
   * Gets the workspace relative path, or {@code null} if none is available.
   * <p>
   * The table below shows an example
   * 
   * for a Jar it is the path to the jar file.
   * 
   * @return a path relative to the workspace, or {@code null} if none is
   *         available.
   */
  @Nullable
  String getWorkspaceRelativePathOrNull();

  /**
   * Path into the jar (only non-null) if in a jar.
   * 
   * @return
   */
  @Nullable
  String getJarRelativePathOrNull();
}
