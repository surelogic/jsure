package com.surelogic.annotation;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

public interface ISingleAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<? super A>>
    extends IAnnotationParseRule<A, P> {
  Class<A> getAASTType();
}
