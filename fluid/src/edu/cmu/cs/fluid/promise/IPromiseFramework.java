package edu.cmu.cs.fluid.promise;

import com.surelogic.annotation.*;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.sea.*;

/**
 * @author chance
 */
public interface IPromiseFramework {
  public IAnnotationParseRule getParseDropRule(String name);
  
  public boolean registerParseDropRule(IAnnotationParseRule rule);  
  public <D extends PromiseDrop>
  boolean registerDropStorage(IPromiseDropStorage<D> stor);
  
  public IPromiseDropStorage findStorage(String tag);
  /*
  public <D extends IAnnotationDrop>
  SlotInfo<D> findNodeStorage(String tag, Class<D> cl);
  
  public <D extends IAnnotationDrop>
  SlotInfo<IRSequence<D>> findSeqStorage(String tag, Class<D> cl);
  
  public <D extends IBooleanAnnotationDrop>
  SlotInfo<D> findBooleanStorage(String tag, Class<D> cl);
  */
}
