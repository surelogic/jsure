package edu.cmu.cs.fluid.java;

import com.surelogic.Nullable;
import com.surelogic.common.IJavaRef;

import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.comment.IJavadocElement;

public interface IFluidJavaRef extends IJavaRef {

  /**
   * Fluid IR node that defines the type used by the {@link SlotInfo} for
   * {@link IFluidJavaRef}.
   */
  public static final IRObjectType<IFluidJavaRef> FLUID_JAVA_REF_SLOT_TYPE = new IRObjectType<IFluidJavaRef>();

  /**
   * Fluid IR name used the {@link SlotInfo} for {@link IFluidJavaRef} below.
   */
  public static final String SRC_REF_SLOT_NAME = "JavaNode.IFluidJavaRef";

  /**
   * Gets the Javadoc for the declaration this node refers to, or {@code null}
   * if none.
   * 
   * @return the Javadoc for the declaration this node refers to, or
   *         {@code null} if none.
   */
  @Nullable
  IJavadocElement getJavadoc();
}
