package edu.cmu.cs.fluid.java;

import com.surelogic.Nullable;
import com.surelogic.ThreadSafe;
import com.surelogic.common.IJavaRef;
import com.surelogic.common.JavaRef.Builder;

import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.ir.SlotInfo;

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
   * Fluid IR node that defines the type used by the {@link SlotInfo} for
   * {@link IFluidJavaRef}.
   */
  static final IRObjectType<IFluidJavaRef> FLUID_JAVA_REF_SLOT_TYPE = new IRObjectType<IFluidJavaRef>();

  /**
   * Fluid IR name used the {@link SlotInfo} for {@link IFluidJavaRef} below.
   */
  static final String SRC_REF_SLOT_NAME = "JavaNode.IFluidJavaRef";

  /**
   * Gets the workspace relative path, or {@code null} if none is available.
   * 
   * @return a path relative to the workspace, or {@code null} if none is
   *         available.
   */
  @Nullable
  String getWorkspaceRelativePathOrNull();
}
