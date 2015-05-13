package edu.cmu.cs.fluid.promise;

import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;

public interface IPromiseFramework {
  public IAnnotationParseRule<?,?> getParseDropRule(String name);

  public boolean registerParseDropRule(IAnnotationParseRule<?,?> rule);

  public <D extends PromiseDrop<?>> boolean registerDropStorage(IPromiseDropStorage<D> stor);

  public IPromiseDropStorage<?> findStorage(String tag);
}
