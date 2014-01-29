package com.surelogic.analysis.granules;

import com.surelogic.analysis.visitors.SuperVisitor.SubVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;

public final class FlowUnitGranule extends GranuleInType {
  private enum Kind {
    METHOD {
      @Override
      public void execute(final SubVisitor<?> visitor, final IRNode flowUnit) {
        visitor.visitMethodDeclaration(flowUnit);
      }
    },
    CONSTRUCTOR {
      @Override
      public void execute(final SubVisitor<?> visitor, final IRNode flowUnit) {
        visitor.visitConstructorDeclaration(flowUnit);
      }
    },
    CLASS_INIT {
      @Override
      public void execute(final SubVisitor<?> visitor, final IRNode flowUnit) {
        visitor.visitClassInitDeclaration(flowUnit);
      }
    };
    
    public abstract void execute(SubVisitor<?> visitor, IRNode flowUnit);
  }
  
  
  
  private final IRNode flowUnit;
  private final Kind kind;
  
  
  
  private FlowUnitGranule(final IRNode type, final Kind k, final IRNode fu) {
    super(type);
    flowUnit = fu;
    kind = k;
  }
  
  public static FlowUnitGranule newMethod(final IRNode type, final IRNode methodDecl) {
    return new FlowUnitGranule(type, Kind.METHOD, methodDecl);
  }
  
  public static FlowUnitGranule newConstructor(final IRNode type, final IRNode constructorDecl) {
    return new FlowUnitGranule(type, Kind.CONSTRUCTOR, constructorDecl);
  }
  
  public static FlowUnitGranule newClassInitializer(final IRNode type, final IRNode classInit) {
    return new FlowUnitGranule(type, Kind.CLASS_INIT, classInit);
  }

  
  
  @Override
  public String getLabel() {
    return JavaNames.genQualifiedMethodConstructorName(flowUnit);
  }

  @Override
  public IRNode getNode() {
    return flowUnit;
  }

  
  
  public void execute(final SubVisitor<?> visitor) {
    kind.execute(visitor, flowUnit);
  }
  
  
  
  @Override
  public boolean equals(final Object other) {
    /* Kind only describes the flowUnit, it doesn't actually contribute
     * to the state of this object. 
     */
    if (other instanceof FlowUnitGranule) {
      final FlowUnitGranule o = (FlowUnitGranule) other;
      return typeDecl == o.typeDecl && flowUnit == o.flowUnit;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    /* Kind only describes the flowUnit, it doesn't actually contribute
     * to the state of this object. 
     */
    int result = 17;
    result = 31 * result + typeDecl.hashCode();
    result = 31 * result + flowUnit.hashCode();
    return result;
  }
}
