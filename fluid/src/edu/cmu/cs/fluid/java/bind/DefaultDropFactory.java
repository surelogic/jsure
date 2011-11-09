/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/DefaultDropFactory.java,v 1.3 2007/07/10 22:16:30 aarong Exp $*/
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.sea.PromiseDrop;

public abstract class DefaultDropFactory<D extends PromiseDrop> extends AbstractDropFactory<D,Object> {
  protected DefaultDropFactory(String tag) {
    super(tag);
  }
}
