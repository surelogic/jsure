package edu.cmu.cs.fluid.java;

import com.surelogic.Nullable;
import com.surelogic.common.IJavaRef;

import edu.cmu.cs.fluid.java.comment.IJavadocElement;

public interface IFluidJavaRef extends IJavaRef {

  @Nullable
  IJavadocElement getJavadoc();

}
