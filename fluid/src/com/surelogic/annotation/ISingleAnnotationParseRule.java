/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/ISingleAnnotationParseRule.java,v 1.2 2007/07/20 14:53:10 chance Exp $*/
package com.surelogic.annotation;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;


public interface ISingleAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<? super A>> 
extends IAnnotationParseRule<A,P>
{
  Class<A> getAASTType();
}
