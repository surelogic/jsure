package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.InRegionNode;
import com.surelogic.aast.promise.NewRegionDeclarationNode;
import com.surelogic.aast.promise.RegionSpecificationNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.regions.AbstractRegion;
import com.surelogic.analysis.regions.FieldRegion;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.NewRegionDeclaration;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Pair;

/**
 * Actual drop for "region" models.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 * 
 * @lock RegionModelLock is class protects nameToDrop
 */
public final class RegionModel extends ModelDrop<NewRegionDeclarationNode> implements IRegionBinding, IRegion {

  public final static String ALL = "java.lang.Object.All";
  public final static String INSTANCE = "java.lang.Object.Instance";

  /**
   * Map from (region name, project name) to drop instances.
   * <p>
   * Accesses must be protected by a lock on this class.
   */
  private static final HashMap<Pair<String, String>, RegionModel> REGIONNAME_PROJECT_TO_DROP = new HashMap<Pair<String, String>, RegionModel>();

  @Override
  public void clearNode() {
    SLLogger.getLogger().log(Level.WARNING, "Clearing node for " + f_project + '/' + this.f_regionName,
        new Throwable("For stack trace"));
    super.clearNode();
  }

  public static RegionModel getInstance(final String regionName, final String projectName) {
    if (regionName == null)
      throw new IllegalArgumentException(I18N.err(44, "regionName"));
    if (projectName == null)
      throw new IllegalArgumentException(I18N.err(44, "projectName"));
    final Pair<String, String> key = new Pair<String, String>(regionName, projectName);
    synchronized (RegionModel.class) {
      purgeUnusedRegions();
      RegionModel result = REGIONNAME_PROJECT_TO_DROP.get(key);
      if (result == null) {
        result = new RegionModel(regionName, projectName);

        REGIONNAME_PROJECT_TO_DROP.put(key, result);
      }
      return result;
    }
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
    if (n != null && n.identity() != IRNode.destroyedNode && !n.equals(region.getNode())) {
      throw new IllegalArgumentException("RegionModel doesn't match field decl: " + n);
    }
    /*
     * else if (n == null && "test.Test.counter".equals(qname)) {
     * System.out.println("Creating region model for test.Test.counter"); }
     */
    model.setNodeAndCompilationUnitDependency(region.getNode());
    setResultMessage(model, region.isStatic(), region.getVisibility(), qname);
    return model;
  }

  private static void setResultMessage(RegionModel model, final boolean isStatic, final Visibility viz, final String name) {
    final String stat = isStatic ? " static" : "";
    model.setResultMessage(Messages.RegionAnnotation_regionDrop, viz.getSourceText(), stat, name);
  }

  /**
   * The global region name this drop represents the declaration for.
   */
  private final String f_regionName;

  /**
   * Gets the global region name this drop represents the declaration for.
   * 
   * @return the global region name this drop represents the declaration for.
   */
  public String getRegionName() {
    return f_regionName;
  }

  /**
   * The simple (unqualified) name of the represented region.
   */
  private final String f_simpleName;

  /**
   * Gets the simple (unqualified) name of the represented region.
   * 
   * @return the simple (unqualified) name of the represented region.
   */
  public String getName() {
    return f_simpleName;
  }

  private final String f_project;

  /**
   * Gets the name of the enclosing project.
   * 
   * @return the name of the enclosing project.
   */
  public String getProject() {
    return f_project;
  }

  /**
   * private constructor invoked by {@link #getInstance(String)}.
   * 
   * @param name
   *          the region name
   */
  private RegionModel(String name, String proj) {
	super(null);
    f_regionName = name;
    f_simpleName = JavaNames.genSimpleName(name);
    f_project = proj;
    this.setMessage("region " + name);
    this.setCategory(JavaGlobals.REGION_CAT);

    if ("java.lang.Object.Instance".equals(name)) {
      System.out.println("Creating RegionModel " + name + " for " + proj);
    }
  }

  @Override
  protected boolean okAsNode(IRNode n) {
    Operator op = JJNode.tree.getOperator(n);
    return VariableDeclarator.prototype.includes(op) || NewRegionDeclaration.prototype.includes(n);
  }

  private static final DropPredicate definingDropPred = new DropPredicate() {
    public boolean match(IDrop d) {
      return d.instanceOf(InRegionPromiseDrop.class) || d.instanceOf(ExplicitUniqueInRegionPromiseDrop.class);
    }
  };

  public static void invalidate(String lockName, IRNode context) {
    final IIRProject p = JavaProjects.getEnclosingProject(context);
    final String project = p == null ? "" : p.getName();
    RegionModel drop = REGIONNAME_PROJECT_TO_DROP.get(Pair.getInstance(lockName, project));
    if (drop != null) {
      drop.clearAAST();
    }
  }

  /**
   * Removes regions that are not defined by any promise definitions.
   */
  public static synchronized void purgeUnusedRegions() {
    synchronized (RegionModel.class) {
      final Set<String> activeProjects = computeActiveProjects();

      for (Iterator<Entry<Pair<String, String>, RegionModel>> iterator = REGIONNAME_PROJECT_TO_DROP.entrySet().iterator(); iterator
          .hasNext();) {
        Entry<Pair<String, String>, RegionModel> entry = iterator.next();
        final Pair<String, String> key = entry.getKey();
        final RegionModel drop = entry.getValue();

        if (drop.getNode() == null) {
          SLLogger.getLogger().severe("Null node for " + drop.f_project + '/' + drop.f_regionName);
        }

        boolean regionDefinedInCode = modelDefinedInCode(definingDropPred, drop) && activeProjects.contains(drop.f_project);
        boolean keepAnyways = false;
        if (!regionDefinedInCode) {
          keepAnyways = drop.isValid()
              && activeProjects.contains(key.second())
              && (drop.getAAST() != null || key.first().equals(INSTANCE) || key.first().endsWith(RegionRules.STATIC_SUFFIX) || key
                  .first().equals(ALL));
        }

        // System.out.println(key+" : "+regionDefinedInCode+", "+keepAnyways);
        if (!(regionDefinedInCode || keepAnyways)) {
          // System.out.println("Purging "+drop.regionName);
          drop.invalidate();
          iterator.remove();
        }
      }
    }
  }

  private static HashSet<String> computeActiveProjects() {
    final ProjectsDrop pd = ProjectsDrop.getDrop();
    if (pd == null) {
      return null; // Assume we're in flux
    }
    HashSet<String> active = new HashSet<String>();
    for (String p : pd.getIIRProjects().getProjectNames()) {
      // System.out.println("\tComparing vs. "+p);
      active.add(p);
    }
    return active;
  }

  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return false;
  }

  @Override
  protected void computeBasedOnAST() {
    /*
     * IRNode decl = getNode(); if (!(JJNode.tree.getOperator(decl) instanceof
     * VariableDeclarator)) {
     * System.out.println("Got non-VarDecl for "+regionName); }
     */
    final NewRegionDeclarationNode ast = getAAST();
    if (ast == null) {
      return;
    }
    setResultMessage(this, ast.isStatic(), ast.getVisibility(), f_regionName);
  }

  public RegionModel getModel() {
    return this;
  }

  public IRegion getRegion() {
    return this;
  }

  /**
   * Returns whether or not this RegionModel represents an abstract (i.e., not a
   * field) region. An Abstract RegionModel has an AAST whereas a field-based
   * does not. Rather, they are dynamically created if they are referenced.
   * 
   * @return true if this is not a field-based region, but rather a declared
   *         region, i.e., @Region
   */
  public boolean isAbstract() {
    return (this.getAAST() != null);
  }

  public boolean isFinal() {
    final NewRegionDeclarationNode ast = getAAST();
    if (ast != null) {
      return false;
    } else {
      return JavaNode.getModifier(VariableDeclarator.getMods(getNode()), JavaNode.FINAL);
    }
  }

  public boolean isVolatile() {
    final NewRegionDeclarationNode ast = getAAST();
    if (ast != null) {
      return false;
    } else {
      return JavaNode.getModifier(VariableDeclarator.getMods(getNode()), JavaNode.VOLATILE);
    }
  }

  public boolean isStatic() {
    if (getAAST() != null) {
      return getAAST().isStatic();
    }
    // FIX From IR
    IRNode decl = getNode();
    if (decl == null) {
      return RegionRules.STATIC.equals(f_regionName) || PromiseConstants.REGION_ALL_NAME.equals(f_regionName);
      // throw new Error("decl is null");
    }
    if (!(JJNode.tree.getOperator(decl) instanceof VariableDeclarator)) {
      return false;
    }
    final int mods = VariableDeclarator.getMods(decl);
    return JavaNode.getModifier(mods, JavaNode.STATIC);
  }

  public Visibility getVisibility() {
    if (getAAST() != null) {
      return getAAST().getVisibility();
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

    if (getAAST() != null) {
      return BindUtil.isAccessibleInsideType(tEnv, getAAST().getVisibility(), getAAST().getPromisedFor(), t);
    }
    return BindUtil.isAccessibleInsideType(tEnv, getNode(), t);
  }

  /**
   * Returns the parent, direct ancestor, of this RegionModel or null if this
   * Region is All
   * 
   * @return This RegionModel's parent RegionModel
   * @throws Exception
   *           If a IRegionBinding doesn't exist
   */
  public RegionModel getParentRegion() {
    final NewRegionDeclarationNode nrdn = this.getAAST();
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
          model = ALL.equals(f_regionName) ? null : RegionModel.getStaticRegionForClass(nrdn.getPromisedFor());
        } else {
          model = INSTANCE.equals(f_regionName) ? RegionModel.getAllRegion(this.getNode()) : RegionModel.getInstanceRegion(this
              .getNode());
        }
      }
    }
    // This is a field-based region, look for a @InRegion and return that
    // region if it exists, otherwise return the defaults
    else {
      final InRegionNode min;
      final InRegionPromiseDrop mipd = RegionRules.getInRegion(this.getNode());

      if (mipd != null) {
        min = mipd.getAAST();
        if (min != null) {
          rsn = min.getSpec();
          if (rsn != null) {
            binding = rsn.resolveBinding();
            if (binding != null) {
              model = binding.getModel();
            } else {
              throw new RuntimeException("No binding exists for " + this);
            }
          }
          // No regionspecificationdrop
          else {
            // Error
            throw new RuntimeException("No RegionSpecificationNode for " + min);
          }
        }
        // No InRegionNode - ERROR
        else {
          throw new RuntimeException("No InRegionNode for " + this);
        }
      }
      // No InRegionPromiseDrop for this field, return the default regions
      else {
        if (ALL.equals(f_regionName)) {
          return null;
        }
        if (this.getNode() == null) {
          SLLogger.getLogger().severe("Null node for region " + f_project + '/' + this.f_regionName);
        }
        if (JavaNode.getModifier(this.getNode(), JavaNode.STATIC)) {
          return RegionModel.getStaticRegionForClass(VisitUtil.getEnclosingType(this.getNode()));
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
      if (!f_regionName.equals(m.f_regionName)) {
        return false;
      }
      if (!f_project.equals(m.f_project)) {
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
    return f_regionName;
  }

  private static String getJRE(IRNode context) {
    if (context != null) {
      final IIRProject thisProj = JavaProjects.getEnclosingProject(context);
      return getJRE(thisProj);
    }
    return IDE.getInstance().getStringPreference(IDEPreferences.DEFAULT_JRE);
  }

  private static String getJRE(IIRProject thisProj) {
    final IJavaDeclaredType jlo = thisProj.getTypeEnv().getObjectType();
    final IIRProject jloProj = JavaProjects.getEnclosingProject(jlo);
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
    return RegionModel.getInstance(JavaNames.getFullTypeName(typeDecl) + ".Static", thisProj.getName());
  }

  public static void printModels() {
    for (RegionModel m : REGIONNAME_PROJECT_TO_DROP.values()) {
      final IRNode n = m.getNode();
      if (n == null || VariableDeclarator.prototype.includes(n)) {
        continue;
      }
      System.out.println("RegionModel: " + m.f_regionName);
      if (!findModel(m)) {
        System.out.println("\tCouldn't find model " + m.f_regionName);
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
