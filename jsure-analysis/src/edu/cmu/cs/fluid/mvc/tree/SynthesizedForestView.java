/*
 * Created on May 14, 2003
 *
 */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.Comparator;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.attr.SortedAttributeView;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * @author Edwin Chan
 */
public interface SynthesizedForestView extends ModelToForestStatefulView {
  /**
   * Actually an attribute of the attribute view
   */
  static final String IS_CRITERIA = "SynthesizedForestView.isCriteria";
  
  void addComparator(String attr, Comparator<Object> c);

	/**
	 * @author Administrator
	 *
	 */
	public interface Factory {
    public SynthesizedForestView create(final String name, final Model src, 
                                        final SortedAttributeView sav,
                                        final String labelAttr,
                                        final AttributeInheritancePolicy aip)
    throws SlotAlreadyRegisteredException;
  }
}
