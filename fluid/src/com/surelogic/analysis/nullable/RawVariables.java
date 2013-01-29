package com.surelogic.analysis.nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.surelogic.analysis.nullable.RawLattice.Element;
import com.surelogic.util.IRNodeIndexedExtraElementArrayLattice;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Associative array from all the receiver declarations in scope or found 
 * along the flow of control, and any non-array
 * object-typed local variable and parameter declarations to the raw state of
 * the referenced object. This array always contains an extra element indexed by
 * "null" whose value is initialized to IMPOSSIBLE. If the value of this last
 * element is ever not IMPOSSIBLE, then the value of the array is non-normative.
 */
public final class RawVariables extends IRNodeIndexedExtraElementArrayLattice<RawLattice, RawLattice.Element> {
  private final Element[] empty;
  
  /**
   * When the lattice is for a constructor declaration of class C, this is the 
   * set of all the qualified this uses, "C.this", that appear along the flow 
   * of control within an anonymous class.  For example, in
   * 
   * <pre>
   * public class C {
   *   private final Object f = new Object() {
   *     final Object g = C.this; // (*)
   *     
   *     {
   *       doStuff(C.this) // (*)
   *       
   *       new Object() {
   *         final Object h = C.this; // (*)
   *       }
   *     }
   *     
   *     private void m() {
   *       moreStuff(C.this);
   *     }
   *   }
   *   
   *   public C() {
   *     super();
   *   }
   * }
   * </pre>
   * 
   * <p>the starred uses of "C.this" would be in this list, but the use in 
   * method <code>m()</code> would not be.  The uses in this set are those 
   * where the qualified receiver is RAW(X) where X is the superclass of C.
   * All other uses of qualified receivers are NOT_RAW.  
   */
  private final Set<IRNode> qualifiedThis;
  
  private RawVariables(final IRNode[] modifiedKeys, final RawLattice lat,
      final Set<IRNode> qt) {
    super(lat, modifiedKeys);
    qualifiedThis = qt;
    
    // Create a unique reference to the empty value
    empty = createEmptyValue();
  }

  public static RawVariables create(
      final List<IRNode> keys, final RawLattice lattice,
      final Set<IRNode> qualifiedThis) {
    return new RawVariables(modifyKeys(keys), lattice, qualifiedThis);
  }

  @Override
  public Element[] getEmptyValue() {
    return empty;
  }

  @Override
  protected Element[] newArray() {
    return new Element[size];
  }
  
  @Override
  protected Element getEmptyElementValue() { return RawLattice.NOT_RAW; }
  
  @Override
  protected Element getNormalFlagValue() { return RawLattice.IMPOSSIBLE; }
  
  public boolean isInterestingQualifiedThis(final IRNode qt) {
    return qualifiedThis.contains(qt);
  }
  
  @Override
  protected void indexToString(final StringBuilder sb, final IRNode index) {
    if (index == null) {
      sb.append("BOGUS");
    } else {
      final Operator op = JJNode.tree.getOperator(index);
      if (ReceiverDeclaration.prototype.includes(op)) {
        sb.append("this");
        final IRNode from = JavaPromise.getPromisedFor(index);
        if (InitDeclaration.prototype.includes(from)) {
          sb.append(" from <init> of ");
          final IRNode clazz = JavaPromise.getPromisedFor(from);
          sb.append(JavaNames.getRelativeTypeName(clazz));
          sb.append('@');
          sb.append(JavaNode.getJavaRef(clazz).getLineNumber());
        } else {
          sb.append(" from ");
          sb.append(JavaNames.genSimpleMethodConstructorName(from));
          sb.append('@');
          sb.append(JavaNode.getJavaRef(from).getLineNumber());
        }
      } else if (ParameterDeclaration.prototype.includes(op)) {
        sb.append(ParameterDeclaration.getId(index));
      } else { // VariableDeclarator
        sb.append(VariableDeclarator.getId(index));
      }
    }
  }
  
  // for debugging
  public String qualifiedThisToString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('{');
    final Iterator<IRNode> i = qualifiedThis.iterator();
    while (i.hasNext()) {
      final IRNode use = i.next();
      sb.append("use@");
      sb.append(JavaNode.getJavaRef(use).getLineNumber());
      if (i.hasNext()) sb.append(", ");
    }
    sb.append('}');
    return sb.toString();
  }
}
