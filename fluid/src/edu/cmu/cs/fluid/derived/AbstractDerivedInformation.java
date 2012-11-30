/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/derived/AbstractDerivedInformation.java,v 1.7 2008/09/08 18:20:05 chance Exp $*/
package edu.cmu.cs.fluid.derived;

import java.util.concurrent.atomic.AtomicBoolean;

import com.surelogic.*;

/**
 * A default implementation of IDerivedInformation
 * 
 * @author Edwin.Chan
 */
@Region("protected Status")
@RegionLock("StatusLock is this protects Status")
public abstract class AbstractDerivedInformation implements IDerivedInformation {  
  /**
   * @mapInto Info
   */
  private final AtomicBoolean derived = new AtomicBoolean(false);
  /**
   * @mapInto Status
   */
  private final AtomicBoolean isDeriving = new AtomicBoolean(false);
  
  public void clear() {
	derived.set(false);
  }

  public final boolean isDeriving() {
    return isDeriving.get();
  }
  
  // This has to protect itself from other threads trying to 
  // use it, as well as notice if it's trying to re-derive
  public synchronized final void ensureDerived() throws UnavailableException {
	//final String label = getLabel();
	/*
	if ("AccessibleJFrame".equals(label)) {
		System.out.println("Got AccessibleJFrame");
	}
	*/
	boolean alreadyDeriving = isDeriving.getAndSet(true);
	if (alreadyDeriving) {
		//System.out.println("Already deriving "+label);
		throw new DerivationException(); 
	}
	/*
	if (label != null) {
		System.out.println("Deriving "+label);
	}
	*/
    try {
    	if (!derived.get()) {
    		boolean rv = derive();
    		derived.set(rv);
    	}
    } finally {
    	//boolean deriving = 
    	isDeriving.getAndSet(false);
    	/*
    	if (deriving == false) {
    		System.out.println("Somehow not deriving "+label);
    	}
    	if (label != null) {
    		System.out.println("Done deriving "+label);
    	}
    	*/
    }
  }
    
  /**
   * @return true if successful
   */
  protected abstract boolean derive();
  
  protected String getLabel() {
	  return null;
  }
}
