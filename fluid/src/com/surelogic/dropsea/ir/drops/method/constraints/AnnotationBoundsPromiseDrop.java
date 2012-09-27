package com.surelogic.dropsea.ir.drops.method.constraints;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

public final class AnnotationBoundsPromiseDrop extends BooleanPromiseDrop<AnnotationBoundsNode> implements
    ValidatedDropCallback<AnnotationBoundsPromiseDrop> {

  public AnnotationBoundsPromiseDrop(AnnotationBoundsNode a) {
    super(a);
    setCategorizingString(JavaGlobals.ANNO_BOUNDS_CAT);
    final AnnotationBoundsNode ast = getAAST();
    final String[] attrs = new String[] { getNameList("containable", ast.getContainable()),
        getNameList("immutable", ast.getImmutable()), getNameList("referenceObject", ast.getReference()),
        getNameList("threadSafe", ast.getThreadSafe()), getNameList("valueObject", ast.getValue()) };
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (final String at : attrs) {
      if (at != null) {
        if (!first) {
          sb.append(", ");
        } else {
          first = false;
        }
        sb.append(at);
      }
    }

    setMessage(Messages.AnnotationBounds, sb.toString(), JavaNames.getTypeName(getNode()));
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
