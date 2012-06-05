package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.logging.Level;

import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.regions.*;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.AbstractDropPredicate;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

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
	 * Map from region names to drop instances (Region, Project -> RegionDrop).
	 */
	private static Hashtable2<String, String, RegionModel> nameToDrop = new Hashtable2<String, String, RegionModel>();

	private Object colorInfo;

	@Override
	public void clearNode() {
		SLLogger.getLogger().log(Level.WARNING, "Clearing node for "+project+'/'+this.regionName, new Throwable("For stack trace"));
		super.clearNode();
	}
	
	public static synchronized RegionModel getInstance(String regionName,
			String projectName) {
		purgeUnusedRegions(); // cleanup the regions
		/*
		 * if (PromiseConstants.REGION_ELEMENT_NAME.equals(regionName)) {
		 * System.out.println("Getting region: "+regionName); }
		 */
		String key = regionName;
		RegionModel result = nameToDrop.get(key, projectName);
		if (result == null) {
			key = CommonStrings.intern(regionName);
			result = new RegionModel(key, projectName);

			nameToDrop.put(key, projectName, result);
			// System.out.println("Creating region "+key);
			/*
			 * if (key.equals(PromiseConstants.REGION_ELEMENT_NAME)) {
			 * System.out.println("Found []"); }
			 */
		}
		return result;
	}

	public static RegionModel getInstance(String region, IRNode context) {
		IIRProject p = JavaProjects.getEnclosingProject(context);
		final String project = p == null ? "" : p.getName();
		return getInstance(region, project);
	}

	public static IRegion getInstance(IRNode field) {
		return new FieldRegion(field);
	}

	/**
	 * Called by FieldRegion.getModel()
	 */
	public static RegionModel getInstance(FieldRegion region) {
		final String qname = region.toString();
		final IIRProject p = JavaProjects.getEnclosingProject(region.getNode());
		final String project = p == null ? "" : p.getName();
		RegionModel model = getInstance(qname, project);
		IRNode n = model.getNode();
		if (n != null && n.identity() != IRNode.destroyedNode
				&& !n.equals(region.getNode())) {
			throw new IllegalArgumentException(
					"RegionModel doesn't match field decl: " + n);
		}
		/*
		 * else if (n == null && "test.Test.counter".equals(qname)) {
		 * System.out.println("Creating region model for test.Test.counter"); }
		 */
		model.setNodeAndCompilationUnitDependency(region.getNode());
		setResultMessage(model, region.isStatic(), region.getVisibility(),
				qname);
		return model;
	}

	private static void setResultMessage(RegionModel model,
			final boolean isStatic, final Visibility viz, final String name) {
		final String stat = isStatic ? " static" : "";
		model.setResultMessage(Messages.RegionAnnotation_regionDrop,
				viz.getSourceText(), stat, name);
	}

	/**
	 * The global region name this drop represents the declaration for.
	 */
	public final String regionName;
	/**
	 * The simple (unqualified) name of the represented region.
	 */
	public final String simpleName;

	public final String project;

	/**
	 * private constructor invoked by {@link #getInstance(String)}.
	 * 
	 * @param name
	 *            the region name
	 */
	private RegionModel(String name, String proj) {
		regionName = name;
		simpleName = JavaNames.genSimpleName(name);
		project = proj;
		this.setMessage("region " + name);
		this.setCategory(JavaGlobals.REGION_CAT);

		if ("java.lang.Object.Instance".equals(name)) {
			System.out.println("Creating RegionModel "+name+" for "+proj);
		}				
	}

	@Override
  public String getProject() {
		return project;
	}

	@Override
	protected boolean okAsNode(IRNode n) {
		Operator op = JJNode.tree.getOperator(n);
		return VariableDeclarator.prototype.includes(op)
				|| NewRegionDeclaration.prototype.includes(n);
	}

	private static DropPredicate definingDropPred = new AbstractDropPredicate() {
		public boolean match(Drop d) {
			return d instanceof InRegionPromiseDrop
			// || d instanceof RegionDeclarationDrop
					|| d instanceof ExplicitUniqueInRegionPromiseDrop;
		}
	};

	public static void invalidate(String key, IRNode context) {
		final IIRProject p = JavaProjects.getEnclosingProject(context);
		final String project = p == null ? "" : p.getName();
		RegionModel drop = nameToDrop.get(key, project);
		if (drop != null) {
			drop.clearAST();
		}
	}

	@Override
	protected void invalidate_internal() {
		/*
		 * if ("[]".equals(regionName)) {
		 * System.out.println("Invalidating region "+regionName); }
		 */
		// new Throwable().printStackTrace();
		// System.out.println("\tin project "+project);
		super.invalidate_internal();
	}

	/**
	 * Removes regions that are not defined by any promise definitions.
	 */
	public static synchronized void purgeUnusedRegions() {
		Hashtable2<String, String, RegionModel> newMap = new Hashtable2<String, String, RegionModel>();
		// boolean invalidated = false;
		for (Pair<String, String> key : nameToDrop.keys()) {
			RegionModel drop = nameToDrop.get(key.first(), key.second());
			if (drop == null) {
				continue;
			}
			if (drop.getNode() == null) {
				SLLogger.getLogger().severe("Null node for "+drop.project+'/'+drop.regionName);
			}
			/*
			 * if (key.first().contains("[]")) {
			 * System.out.println("Found region []"); }
			 */
			boolean regionDefinedInCode = modelDefinedInCode(definingDropPred,
					drop) && isActiveProject(drop.project);
			boolean keepAnyways = false;
			if (!regionDefinedInCode) {
				keepAnyways = drop.isValid()
						&& isActiveProject(key.second())
						&& (drop.colorInfo != null || drop.getAST() != null
								|| key.first().equals(INSTANCE)
								|| key.first().endsWith(RegionRules.STATIC_SUFFIX)
								|| key.first().equals(ALL));
			}

			// System.out.println(key+" : "+regionDefinedInCode+", "+keepAnyways);
			if (regionDefinedInCode || keepAnyways) {
				newMap.put(key.first(), key.second(), drop);
			} else {
				// System.out.println("Purging "+drop.regionName);
				drop.invalidate();
				// invalidated = true;
			}
		}
		// swap out the static map to regions
		nameToDrop = newMap;
		/*
		 * if (invalidated) { System.out.println("Re-trying purge");
		 * purgeUnusedRegions(); }
		 */
	}

	private static boolean isActiveProject(String proj) {
		// System.out.println("Checking if "+proj+" is active");
		final ProjectsDrop pd = ProjectsDrop.getDrop();
		if (pd == null) {
			return true; // Assume we're in flux
		}
		for (String p : pd.getIIRProjects().getProjectNames()) {
			// System.out.println("\tComparing vs. "+p);
			if (p.equals(proj)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Region definitions are not checked by analysis (other than the promise
	 * scrubber).
	 */
	@Override
	public boolean isIntendedToBeCheckedByAnalysis() {
		// if (hasMatchingDependents(DropPredicateFactory
		// .matchType(AggregatePromiseDrop.class))) {
		// return true;
		// }
		// else {
		return false;
		// }
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
		/*
		 * IRNode decl = getNode(); if (!(JJNode.tree.getOperator(decl)
		 * instanceof VariableDeclarator)) {
		 * System.out.println("Got non-VarDecl for "+regionName); }
		 */
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
	 * Returns whether or not this RegionModel represents an abstract (i.e., not
	 * a field) region. An Abstract RegionModel has an AAST whereas a
	 * field-based does not. Rather, they are dynamically created if they are
	 * referenced.
	 * 
	 * @return true if this is not a field-based region, but rather a declared
	 *         region, i.e., @Region
	 */
	public boolean isAbstract() {
		return (this.getAST() != null);
	}

	public boolean isFinal() {
		final NewRegionDeclarationNode ast = getAST();
		if (ast != null) {
			return false;
		} else {
			return JavaNode.getModifier(VariableDeclarator.getMods(getNode()),
					JavaNode.FINAL);
		}
	}

	public boolean isVolatile() {
		final NewRegionDeclarationNode ast = getAST();
		if (ast != null) {
			return false;
		} else {
			return JavaNode.getModifier(VariableDeclarator.getMods(getNode()),
					JavaNode.VOLATILE);
		}
	}

	public boolean isStatic() {
		if (getAST() != null) {
			return getAST().isStatic();
		}
		// FIX From IR
		IRNode decl = getNode();
		if (decl == null) {
			return RegionRules.STATIC.equals(regionName) || PromiseConstants.REGION_ALL_NAME.equals(regionName);
			// throw new Error("decl is null");
		}
		if (!(JJNode.tree.getOperator(decl) instanceof VariableDeclarator)) {
			return false;
		}
		final int mods = VariableDeclarator.getMods(decl);
		return JavaNode.getModifier(mods, JavaNode.STATIC);
	}

	public String getName() {
		// return getAST().getId();
		return simpleName;
	}

	public Visibility getVisibility() {
		if (getAST() != null) {
			return getAST().getVisibility();
		}
		// FIX From IR
		IRNode decl = getNode();
		if (decl == null) {
			return Visibility.PUBLIC;
		} else {
			return Visibility.getVisibilityOf(decl);
		}
	}

	public boolean isAccessibleFromType(ITypeEnvironment tEnv, IRNode t) {

		if (getAST() != null) {
			return BindUtil.isAccessibleInsideType(tEnv, getAST()
					.getVisibility(), getAST().getPromisedFor(), t);
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
				} else {
					throw new RuntimeException("No binding exists for " + this);
				}
			}
			// return the default parent
			else {
				if (nrdn.isStatic()) {
					model = ALL.equals(regionName) ? null :
					  RegionModel.getStaticRegionForClass(nrdn.getPromisedFor());
				} else {
					model = INSTANCE.equals(regionName) ? RegionModel
							.getAllRegion(this.getNode()) : RegionModel.getInstanceRegion(this.getNode());
				}
			}
		}
		// This is a field-based region, look for a @InRegion and return that
		// region if it exists, otherwise return the defaults
		else {
			final InRegionNode min;
			final InRegionPromiseDrop mipd = RegionRules.getInRegion(this
					.getNode());

			if (mipd != null) {
				min = mipd.getAST();
				if (min != null) {
					rsn = min.getSpec();
					if (rsn != null) {
						binding = rsn.resolveBinding();
						if (binding != null) {
							model = binding.getModel();
						} else {
							throw new RuntimeException("No binding exists for "
									+ this);
						}
					}
					// No regionspecificationdrop
					else {
						// Error
						throw new RuntimeException(
								"No RegionSpecificationNode for " + min);
					}
				}
				// No InRegionNode - ERROR
				else {
					throw new RuntimeException("No InRegionNode for " + this);
				}
			}
			// No InRegionPromiseDrop for this field, return the default regions
			else {
				if (ALL.equals(regionName)) {
					return null;
				}
				if (this.getNode() == null) {
					SLLogger.getLogger().severe("Null node for region "+project+'/'+this.regionName);
				}
				if (JavaNode.getModifier(this.getNode(), JavaNode.STATIC)) {
				  return RegionModel.getStaticRegionForClass(
				      VisitUtil.getEnclosingType(this.getNode()));
				} else {
					return RegionModel.getInstanceRegion(this.getNode());
				}
			}
		}
		/*
		 * if (model == null) { return null; }
		 */
		return model;
	}

	public boolean isSameRegionAs(IRegion o) {
		if (o instanceof FieldRegion) {
			FieldRegion fr = (FieldRegion) o;
			/*
			 * if (!isAbstract() && getNode() == null) { System.out.println(); }
			 */
			return !isAbstract() && getNode().equals(fr.getNode());
		}
		if (o instanceof RegionModel) {
			RegionModel m = (RegionModel) o;
			if (!regionName.equals(m.regionName)) {
				return false;
			}
			if (!project.equals(m.project)) {
				return false;
			}
			if (this != m) {
				throw new Error("Same name, but different RegionModel object");
			}
			return true;
		}
		throw new Error("Unrecognized IRegion: " + o);
	}

	public boolean ancestorOf(final IRegion other) {
		return AbstractRegion.ancestorOf(this, other);
	}

	public boolean includes(final IRegion other) {
		return AbstractRegion.includes(this, other);
	}

	public boolean overlapsWith(final IRegion other) {
		return ancestorOf(other) || other.ancestorOf(this);
	}

	@Override
	public String toString() {
		return this.regionName;
	}

	private static String getJRE(IRNode context) {
		if (context != null) {
			final IIRProject thisProj   = JavaProjects.getEnclosingProject(context);
			return getJRE(thisProj);
		}
		return 
		IDE.getInstance().getStringPreference(IDEPreferences.DEFAULT_JRE);
	}
	
	private static String getJRE(IIRProject thisProj) {
		final IJavaDeclaredType jlo = thisProj.getTypeEnv().getObjectType();
		final IIRProject jloProj    = JavaProjects.getEnclosingProject(jlo);
		return jloProj.getName();
	}
	
	public static RegionModel getAllRegion(IIRProject p) {	
		return RegionModel.getInstance(ALL, getJRE(p)); 
	}
	
	public static RegionModel getAllRegion(IRNode context) {	
		return RegionModel.getInstance(ALL, getJRE(context)); 
	}

	public static RegionModel getInstanceRegion(IRNode context) {
		return RegionModel.getInstance(INSTANCE, getJRE(context)); 
	}

	/**
	 * Get the "Static" region for the given class or interface.
	 */
	public static RegionModel getStaticRegionForClass(final IRNode typeDecl) {
	  final IIRProject thisProj = JavaProjects.getEnclosingProject(typeDecl);
	  return RegionModel.getInstance(
	      JavaNames.getFullTypeName(typeDecl) + ".Static", thisProj.getName());
	}
	
	public static void printModels() {
		for (RegionModel m : nameToDrop.elements()) {
			final IRNode n = m.getNode();
			if (n == null || VariableDeclarator.prototype.includes(n)) {
				continue;
			}
			System.out.println("RegionModel: " + m.regionName);
			if (!findModel(m)) {
				System.out.println("\tCouldn't find model " + m.regionName);
			}
		}
	}

	private static boolean findModel(RegionModel m) {
		for (RegionModel m2 : RegionRules.getModels(m.getNode())) {
			if (m == m2) {
				return true;
			}
		}
		return false;
	}
}