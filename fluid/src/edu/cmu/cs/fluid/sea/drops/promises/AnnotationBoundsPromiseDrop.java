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
    setResultMessage(Messages.AnnotationBounds,
        getNameList(ast.getContainable()),
        getNameList(ast.getImmutable()),
        getNameList(ast.getThreadSafe()),
        JavaNames.getTypeName(getNode()));
  }

  private String getNameList(final NamedTypeNode[] list) {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (final NamedTypeNode namedType : list) {
      if (!first) {
        sb.append(", ");
      } else {
        first = false;
      }
      sb.append(namedType.getType());      
    }
    return sb.toString();
  }
  
  public void validated(final AnnotationBoundsPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}