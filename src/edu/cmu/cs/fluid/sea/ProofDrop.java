package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.sea.xml.*;

/**
 * Represents a promise or a result used in the code/model consistency proof.
 * 
 * @subtypedBy
 *   edu.cmu.cs.fluid.sea.PromiseDrop,
 *   edu.cmu.cs.fluid.sea.ResultDrop
 */
public abstract class ProofDrop extends IRReferenceDrop {

  /**
   * Records if this element is able to be proved consistent with regards
   * to the whole-program.
   */
  boolean provedConsistent = false;

  /**
   * Returns if this element is able to be proved consistent (model/code
   * consistency) with regards to the whole-program.
   * 
   * @return <code>true</code> if consistent, <code>false</code> if inconsistent.
   */
  public boolean provedConsistent() {
    return provedConsistent;
  }

  /**
   * Records if the proof of this element depends upon a "red dot," or a
   * user vouching for or assuming something which may not be true, with
   * regards to the whole-program.
   */
  boolean proofUsesRedDot = true;

  /**
   * Returns if the proof of this element depends upon a "red dot," or a
   * user vouching for or assuming something which may not be true, with
   * regards to the whole-program.

   * @return<code>true</code> if red dot, <code>false</code> if no red dot.
   */
  public boolean proofUsesRedDot() {
    return proofUsesRedDot;
  }
  
  @Override
  public String getEntityName() {
	  return "proof-drop";
  }	
  
  @Override
  public void snapshotAttrs(AbstractSeaXmlCreator s) {
	  super.snapshotAttrs(s);
	  s.addAttribute("used-red-dot", proofUsesRedDot());
	  s.addAttribute("proved-consistent", provedConsistent());
  }
}