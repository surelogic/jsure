/*
 * Created on Sep 9, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.IPrimitiveType;

import edu.cmu.cs.fluid.java.operator.PrimitiveType;

/**
 * A primitive Java type represented by the Java Operator.
 * @author chance
 */
public interface IJavaPrimitiveType extends IJavaType, IPrimitiveType {
  PrimitiveType getOp();
  String getCorrespondingTypeName();
}
