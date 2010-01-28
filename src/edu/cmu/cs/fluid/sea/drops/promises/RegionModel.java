package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.*;

import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.regions.*;
import com.surelogic.annotation.rules.RegionRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Actual drop for "region" models.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 * @see edu.cmu.cs.fluid.java.bind.RegionAnnotation
 * 
 * @lock RegionModelLock is class protects nameToDrop
 */
public class RegionModel extends ModelDrop<NewRegionDeclarationNode> implements
		IRegionBinding, IRegion {
	
  public final static String ALL = "java.lang.Object.All";
  public final static String INSTANCE = "java.lang.Object.Instance";
  
	/**
	 * Map from region names to drop instances (String -> RegionDrop).
	 */
	private static Map<String, RegionModel> nameToDrop = new HashMap<String, RegionModel>();
  
	private Object colorInfo;

	public static synchronized RegionModel getInstance(String regionName) {
		purgeUnusedRegions(); // cleanup the regions

		String key = regionName;
		RegionModel result = nameToDrop.get(key);
		if (result == null) {
			key = CommonStrings.intern(regionName);
			result = new RegionModel(key);

			nameToDrop.put(key, result);
			//System.out.println("Creating region "+key);
		}
		return result;
	}

  public static IRegion getInstance(IRNode field) {
    return new FieldRegion(field);
  }
  
  /**
   * Called by FieldRegion.getModel()
   */
  public static RegionModel getInstance(FieldRegion region) {
    String qname      = region.toString();
    RegionModel model = getInstance(qname);
    IRNode n = model.getNode();
    if (n != null && n.identity() != IRNode.destroyedNode &&
        !n.equals(region.getNode())) {
      throw new IllegalArgumentException("RegionModel doesn't match field decl: "+n);
    }
    model.setNodeAndCompilationUnitDependency(region.getNode());
    setResultMessage(model, region.isStatic(), region.getVisibility(), qname);
    return model;
  }
  
  private static void setResultMessage(RegionModel model, 
      final boolean isStatic, final int viz, final String name) {
    final String stat = isStatic ? " static" : "";
    
    String visibility = " ";
    switch (viz) {
    case JavaNode.PRIVATE:
      visibility = "private"; //$NON-NLS-1$
      break;
    case JavaNode.PROTECTED:
      visibility = "protected"; //$NON-NLS-1$
      break;
    case JavaNode.PUBLIC:
      visibility = "public"; //$NON-NLS-1$
      break;
    }
    
    model.setResultMessage(
        Messages.RegionAnnotation_regionDrop, visibility,  stat, name);
  }
  
	/**
	 * The global region name this drop represents the declaration for.
	 */
	public final String regionName;
	/**
	 * The simple (unqualified) name of the represented region.
	 */
	public final String simpleName;
	
	/**
	 * private constructor invoked by {@link #getInstance(String)}.
	 * 
	 * @param name
	 *            the region name
	 */
	private RegionModel(String name) {
		regionName = name;
		simpleName = JavaNames.genSimpleName(name);
		this.setMessage("region " + name);
		this.setCategory(JavaGlobals.REGION_CAT);
	}

	@Override
	protected boolean okAsNode(IRNode n) {
		Operator op = JJNode.tree.getOperator(n);
		return VariableDeclarator.prototype.includes(op)
				|| NewRegionDeclaration.prototype.includes(n);
	}

	private static DropPredicate definingDropPred = new DropPredicate() {
		public boolean match(Drop d) {
			return d instanceof InRegionPromiseDrop
			// || d instanceof RegionDeclarationDrop
					|| d instanceof AggregatePromiseDrop;
		}
	};
	
	public static void invalidate(String key) {
		RegionModel drop = nameToDrop.get(key);
		if (drop != null) {
			drop.clearAST();
		}
	}
	
	@Override
	protected void invalidate_internal() {
		if ("[]".equals(regionName)) {
			System.out.println("Invalidating region "+regionName);
		}
		super.invalidate_internal();
	}
	
	/**
	 * Removes regions that are not defined by any promise definitions.
	 */
	public static synchronized void purgeUnusedRegions() {
		Map<String, RegionModel> newMap = new HashMap<String, RegionModel>();
		//boolean invalidated = false;
		for (String key : nameToDrop.keySet()) {
			RegionModel drop = nameToDrop.get(key);

			boolean regionDefinedInCode = modelDefinedInCode(definingDropPred,
					drop);			
			boolean keepAnyways = false;
			if (!regionDefinedInCode) {
				keepAnyways = drop.isValid() && 
        	                 (drop.colorInfo != null || 
                              drop.getAST() != null  || 
                              key.equals(INSTANCE) || 
                              key.equals(ALL)) || 
                              key.equals(PromiseConstants.REGION_ELEMENT_NAME);
			}
			 
			//System.out.println(key+" : "+regionDefinedInCode+", "+keepAnyways);
			if (regionDefinedInCode || keepAnyways) {
				newMap.put(key, drop);
			}
			else {
				//System.out.println("Purging "+drop.regionName);
				drop.invalidate();
				//invalidated = true;
			}
		}		
		// swap out the static map to regions
		nameToDrop = newMap;
		/*
		if (invalidated) {
			System.out.println("Re-trying purge");
			purgeUnusedRegions();	
		}
		*/
	}

	/**
	 * Region definitions are not checked by analysis (other than the promise
	 * scrubber).
	 */
	@Override
  public boolean isIntendedToBeCheckedByAnalysis() {
//		if (hasMatchingDependents(DropPredicateFactory
//				.matchType(AggregatePromiseDrop.class))) {
//			return true;
//		}
//		else {
			return false;
//		}
	}

	/**
	 * @return Returns the colorInfo.
	 */
	public Object getColorInfo() {
		return colorInfo;
	}

	/**
	 * @param colorInfo
	 *            The colorInfo to set.
	 */
	public void setColorInfo(Object colorInfo) {
		this.colorInfo = colorInfo;
	}

	@Override
	protected void computeBasedOnAST() {
		final NewRegionDeclarationNode ast = getAST();
    if (ast == null) {
			return;
		}
        setResultMessage(this, ast.isStatic(), ast.getVisibility(), regionName);
	}

	public RegionModel getModel() {
		return this;
	}
  
  public IRegion getRegion() {
    return this;
  }
	
	/**
	 * Returns whether or not this RegionModel represents an abstract (i.e., not a field) region.
	 * An Abstract RegionModel has an AAST whereas a field-based does not. Rather, they are dynamically
	 * created if they are referenced.
	 * 
	 * @return true if this is not a field-based region, but rather a declared region, i.e., @Region
	 */
	public boolean isAbstract(){
		return (this.getAST() != null);
	}
	
	public boolean isFinal() {
	  final NewRegionDeclarationNode ast = getAST();
	  if (ast != null) {
	    return false;
	  } else {
	    return JavaNode.getModifier(
	        VariableDeclarator.getMods(getNode()), JavaNode.FINAL);
	  }	  
	}

  public boolean isVolatile() {
    final NewRegionDeclarationNode ast = getAST();
    if (ast != null) {
      return false;
    } else {
      return JavaNode.getModifier(
          VariableDeclarator.getMods(getNode()), JavaNode.VOLATILE);
    }   
  }

  public boolean isStatic() {
    if (getAST() != null) {
      return getAST().isStatic();
    }
    // FIX From IR
    IRNode decl = getNode();
    if (decl == null) {
      // XXX: Shouldn't his be "All"???
      return "Static".equals(regionName);
      //throw new Error("decl is null");
    }
    final int mods = VariableDeclarator.getMods(decl);
    return JavaNode.getModifier(mods, JavaNode.STATIC);
  }

  public String getName() {
//    return getAST().getId();
    return simpleName;
  }
  
  public int getVisibility() {
    if (getAST() != null) {
      return getAST().getVisibility();
    }
    // FIX From IR
    IRNode decl = getNode();
    if (decl == null) {
      return JavaNode.PUBLIC;
    }
    int modifiers = VariableDeclarator.getMods(decl);
    for(int viz : legalVisibilities) {
      if (JavaNode.isSet(modifiers, viz)) {
        return viz;
      }
    }
    return 0;
  }
  
  private static int[] legalVisibilities = {
    JavaNode.PRIVATE, JavaNode.PROTECTED, JavaNode.PUBLIC
  };
  
  public boolean isAccessibleFromType(ITypeEnvironment tEnv, IRNode t) {
    
    if (getAST() != null) {
      return BindUtil.isAccessibleInsideType(tEnv, getAST().getVisibility(), 
                                             getAST().getPromisedFor(), t);
    }
    return BindUtil.isAccessibleInsideType(tEnv, getNode(), t);
  }
  
	/**
	 * Returns the parent, direct ancestor, of this RegionModel or null if this
	 * Region is All
	 * 
	 * @return This RegionModel's parent RegionModel
	 * @throws Exception
	 *             If a IRegionBinding doesn't exist
	 */
	public RegionModel getParentRegion() {
		final NewRegionDeclarationNode nrdn = this.getAST();
		RegionModel model = null;
		final RegionSpecificationNode rsn;
		final IRegionBinding binding;

		if (nrdn != null) {
			rsn = nrdn.getRegionParent();
			if (rsn != null) {
				binding = rsn.resolveBinding();
				if (binding != null) {
					model = binding.getModel();
				}
				else {
					throw new RuntimeException("No binding exists for " + this);
				}
			}
			// return the default STATIC or INSTANCE
			else {
				if(nrdn.isStatic()){
    			model =	ALL.equals(regionName) ? null : RegionModel.getInstance(ALL);
				}
				else{
    			model =	INSTANCE.equals(regionName) ? RegionModel.getInstance(ALL) : RegionModel.getInstance(INSTANCE);
				}
			}
		}
		// This is a field-based region, look for a @InRegion and return that region if it exists, otherwise return the defaults
		else {
			final InRegionNode min;
			final InRegionPromiseDrop mipd = RegionRules.getInRegion(this.getNode());
			
			if(mipd != null){
				min = mipd.getAST();
				if(min != null){
					rsn = min.getSpec();
					if(rsn != null){
						binding = rsn.resolveBinding();
						if(binding != null){
							model = binding.getModel();
						}
						else {
        					throw new RuntimeException("No binding exists for " + this);
						}
					}
					//No regionspecificationdrop
					else{
						//Error
    					throw new RuntimeException("No RegionSpecificationNode for " + min);
					}
				}
				//No InRegionNode - ERROR
				else{
					throw new RuntimeException("No InRegionNode for " + this);
				}
			}
			//No InRegionPromiseDrop for this field, return the default regions
			else{
				if(JavaNode.getModifier(this.getNode(), JavaNode.STATIC)){
					return RegionModel.getInstance(ALL);
				}
				else{
					return RegionModel.getInstance(INSTANCE);
				}
			}
		}
    /*
    if (model == null) {
      return null;
    }
    */
		return model;
	}

  public boolean isSameRegionAs(IRegion o) {
    if (o instanceof FieldRegion) {
      FieldRegion fr = (FieldRegion) o;
      /*
      if (!isAbstract() && getNode() == null) {
        System.out.println();
      }
      */
      return !isAbstract() &&
             getNode().equals(fr.getNode());
    }
    if (o instanceof RegionModel) {
      RegionModel m = (RegionModel) o;
      if (!regionName.equals(m.regionName)) {
        return false;
      }
      if (this != m) {
        throw new Error("Same name, but different RegionModel object");
      }
      return true;
    }
    throw new Error("Unrecognized IRegion: "+o);
  }
  
	public boolean ancestorOf(final IRegion other) {
    return AbstractRegion.ancestorOf(this, other);
	}

	public boolean includes(final IRegion other) {
		return AbstractRegion.includes(this, other);
	}
	
	public boolean overlapsWith(final IRegion other){
		return ancestorOf(other) || other.ancestorOf(this);
	}

  @Override
  public String toString() {
    return this.regionName;
  }
}