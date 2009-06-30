package edu.cmu.cs.fluid.promise;

import com.surelogic.annotation.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.tree.Operator;

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
  public boolean registerParseRule(String string, IPromiseParseRule rule);

  public boolean registerBindRule(Operator op, IPromiseBindRule rule);

  public boolean registerCheckRule(Operator operator, IPromiseCheckRule rule);

  /**
   * After this is called, there should be no more rules registered.
   */
  public void finishInit(IBinder binder, IPromiseParser parser);

	public boolean registerStorage(Operator operator, IPromiseStorage stor);

	public IPromiseParser getParser();
	
	public void setParser(IPromiseParser parser);
}
