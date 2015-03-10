/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/derived/AbstractDerivedInformation.java,v 1.7 2008/09/08 18:20:05 chance Exp $*/
package edu.cmu.cs.fluid.derived;

import java.util.concurrent.atomic.*;

import com.surelogic.*;

/**
 * A default implementation of IDerivedInformation
 * (Note that the synchronization below doesn't protect the derived state itself)
 * 
 * @author Edwin.Chan
 */
@PolicyLock("DerivingLock is this")
public abstract class AbstractDerivedInformation implements IDerivedInformation {  
  public enum Status {
	  NOT_DERIVED, IS_DERIVING, DERIVED, DESTROYED
  }

  private final AtomicReference<Status> status = 
	  new AtomicReference<Status>(Status.NOT_DERIVED);
    
  @Override
  public synchronized void clear() {
	// TODO what if destroyed?
	status.set(Status.NOT_DERIVED);
  }
  
  @Override
  public final boolean isDeriving() {
    return status.get() == Status.IS_DERIVING;
  }
  
  // This has to protect itself from other threads trying to 
  // use it, as well as notice if it's trying to re-derive
  @Override
  public synchronized final void ensureDerived() throws UnavailableException {
	final String label = getLabel();
	/*
	if ("AccessibleJFrame".equals(label)) {
		System.out.println("Got AccessibleJFrame");
	}
	*/
	final Status s = status.get();
	//boolean alreadyDeriving = isDeriving.getAndSet(true);
	//if (alreadyDeriving) {
	switch (s) {
	case IS_DERIVING:
		System.out.println("Already deriving "+label);
		throw new DerivationException(); 
	case DERIVED:
	case DESTROYED:
		// Nothing else to do
		return; 
	case NOT_DERIVED:
		/*
		if (label != null) {
			System.out.println("Deriving "+label);
		}
		*/
    	boolean derived = derive();
    	if (derived) {
    		boolean updated = status.compareAndSet(Status.NOT_DERIVED, Status.DERIVED);        	
        	if (!updated) {
        		//System.out.println("Somehow not deriving "+label);
        		ensureDerived();
        	}
        	/*
        	if (label != null) {
        		System.out.println("Done deriving "+label);
        	}
        	*/
    	}
	}
  }
    
  /**
   * @return true if successful
   */
  @RequiresLock("StateLock")
  protected abstract boolean derive();
  
  /**
   * For debugging
   */
  protected String getLabel() {
	  return null;
  }
  
  protected final Status getStatus() {
	  return status.get();
  }
  
  @MustInvokeOnOverride
  protected synchronized void destroy() {
	  status.set(Status.DESTROYED);
  }
}
