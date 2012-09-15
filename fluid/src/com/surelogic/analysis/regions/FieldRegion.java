/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/regions/FieldRegion.java,v 1.12 2008/02/25 20:52:42 aarong Exp $*/
package com.surelogic.analysis.regions;

import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.InRegionNode;
import com.surelogic.aast.promise.RegionSpecificationNode;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.dropsea.ir.drops.InRegionPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

public class FieldRegion extends AbstractRegion {
  private final IRNode field;

  public FieldRegion(IRNode ref) {
    if (ref == null) {
      throw new IllegalArgumentException("null field decl");
    }
    field = ref;
  }
  
  public boolean isStatic() {
    return TypeUtil.isStatic(field);
  }
  
  public boolean isAbstract() {
    return false;
  }
  
  public boolean isFinal() {
    return TypeUtil.isFinal(field);
  }
  
  public boolean isVolatile() {
    return TypeUtil.isVolatile(field);
  }
  
  public IRNode getNode() {
    return field;
  }
  
  public IRegion getRegion() {
    return this;
  }
  
  public String getName() {
    if (EnumConstantDeclaration.prototype.includes(field)) {
      return EnumConstantDeclaration.getId(field);
    } else {
      return VariableDeclarator.getId(field);
    }
  }
  
  public Visibility getVisibility() {
    return Visibility.getVisibilityOf(
        JJNode.tree.getParent(JJNode.tree.getParent(field)));
  }
  
  public boolean isAccessibleFromType(ITypeEnvironment tEnv, IRNode t) {
    return BindUtil.isAccessibleInsideType(tEnv, field, t);
  }
  
  public RegionModel getModel() {
    return RegionModel.getInstance(this);
  }
  
  public IRegion getParentRegion() {
    // This is a field-based region, look for a @InRegion
    // and return that region if it exists, otherwise return the defaults
    final InRegionNode min;
    final InRegionPromiseDrop mipd = RegionRules.getInRegion(field);

    if (mipd != null) {
      min = mipd.getAAST();
      if (min != null) {
        RegionSpecificationNode rsn = min.getSpec();
        if (rsn != null) {
          IRegionBinding binding = rsn.resolveBinding();
          if (binding != null) {
            return binding.getModel();
          } else {
            throw new RuntimeException("No binding exists for " + this);
          }
        }
        // No regionspecificationdrop
        else {
          // Error
          throw new RuntimeException("No RegionSpecificationNode for " + min);
        }
      }
      // No InRegionNode - ERROR
      else {
        throw new RuntimeException("No InRegionNode for " + this);
      }
    }
    // No InRegionPromiseDrop for this field, return the default regions
    else {
      if (isStatic()) {
        return RegionModel.getStaticRegionForClass(
            VisitUtil.getEnclosingType(field));
      } else {
        return RegionModel.getInstanceRegion(field);
      }
    }
  }

  public boolean isSameRegionAs(IRegion o) {
    if (o instanceof FieldRegion) {
      FieldRegion fr = (FieldRegion) o;
      return field.equals(fr.field);
    }
    if (o instanceof RegionModel) {
      RegionModel m = (RegionModel) o;
      return !m.isAbstract() &&
             field.equals(m.getNode());
    }
    throw new Error("Unrecognized IRegion: "+o);
  }

  @Override
  public String toString() {
    return JavaNames.getQualifiedTypeName(VisitUtil.getEnclosingType(field))+'.'+VariableDeclarator.getId(field);
  }
  
  @Override
  public int hashCode() {
    return field.hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof FieldRegion) {
      FieldRegion r = (FieldRegion) o;
      return field.equals(r.field);
    }
    else if (o instanceof RegionModel) {
      RegionModel r = (RegionModel) o;
      return field.equals(r.getNode());
    }
    return false;
  }
}
