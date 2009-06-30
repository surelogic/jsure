/*$Header: /cvs/fluid/fluid/src/com/surelogic/ast/IHasBinding.java,v 1.2 2007/10/29 05:28:37 chance Exp $*/
package com.surelogic.ast;

/**
 * Not for use with AASTs
 * @author chance
 */
public interface IHasBinding extends Resolvable {
  IBinding resolveBinding();
}
