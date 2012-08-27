package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class AnnotationBoundsPromiseDrop extends
    BooleanPromiseDrop<AnnotationBoundsNode> implements
    ValidatedDropCallback<AnnotationBoundsPromiseDrop> {
  public AnnotationBoundsPromiseDrop(AnnotationBoundsNode a) {
    super(a); 
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    final AnnotationBoundsNode ast = getAST();
    final String[] attrs = new String[] {
        getNameList("containable", ast.getContainable()),
        getNameList("immutable", ast.getImmutable()),
        getNameList("threadSafe", ast.getThreadSafe()) };
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (final String a : attrs) {
      if (a != null) {
        if (!first) {
          sb.append(", ");
        } else {
          first = false;
        }
        sb.append(a);
      }
    }
    
    setResultMessage(Messages.AnnotationBounds, sb.toString(),
        JavaNames.getTypeName(getNode()));
  }

  private String getNameList(final String attribute, final NamedTypeNode[] list) {
    if (list.length > 0) {
      final StringBuilder sb = new StringBuilder();
      sb.append(attribute);
      sb.append(" = {");
      boolean first = true;
      for (final NamedTypeNode namedType : list) {
        if (!first) {
          sb.append("\", \"");
        } else {
          first = false;
          sb.append('\"');
        }
        sb.append(namedType.getType());      
      }
      sb.append("\"}");
      return sb.toString();
    } else {
      return null;
    }
  }
  
  public void validated(final AnnotationBoundsPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}