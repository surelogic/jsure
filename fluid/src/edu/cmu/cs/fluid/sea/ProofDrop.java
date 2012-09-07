package edu.cmu.cs.fluid.sea;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.TypeUtil;

/**
 * Represents a promise or a result used in the code/model consistency proof.
 * 
 * @subtypedBy edu.cmu.cs.fluid.sea.PromiseDrop, edu.cmu.cs.fluid.sea.ResultDrop
 */
public abstract class ProofDrop extends IRReferenceDrop implements IProofDrop {

  /**
   * Records if this element is able to be proved consistent with regards to the
   * whole-program.
   */
  boolean provedConsistent = false;

  /**
   * Returns if this element is able to be proved consistent (model/code
   * consistency) with regards to the whole-program.
   * 
   * @return <code>true</code> if consistent, <code>false</code> if
   *         inconsistent.
   */
  public boolean provedConsistent() {
    return provedConsistent;
  }

  /**
   * Records whether this result depends on something from source code.
   */
  boolean derivedFromSrc = false;

  /**
   * Checks is this result depends upon something from source code.
   * 
   * @return {@code true} if this result depends on something from source code,
   *         {@code false} otherwise.
   */
  public boolean derivedFromSrc() {
    return derivedFromSrc;
  }

  /**
   * Records if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   */
  boolean proofUsesRedDot = true;

  /**
   * Returns if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   * 
   * @return<code>true</code> if red dot, <code>false</code> if no red dot.
   */
  public boolean proofUsesRedDot() {
    return proofUsesRedDot;
  }

  public boolean isFromSrc() {
    // throw new UnsupportedOperationException("Not a PromiseDrop");
    IRNode n = getNode();
    if (n != null) {
      return !TypeUtil.isBinary(n);
    }
    return false;
  }

  @Override
  public String getXMLElementName() {
    return "proof-drop";
  }

  @Override
  @MustInvokeOnOverride
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(AbstractXMLReader.USES_RED_DOT_ATTR, proofUsesRedDot());
    s.addAttribute(AbstractXMLReader.PROVED_ATTR, provedConsistent());
    // System.out.println(getMessage()+" proved consistent: "+provedConsistent());
    s.addAttribute(AbstractXMLReader.DERIVED_FROM_SRC_ATTR, derivedFromSrc());
  }
}