// $Header: /var/cvs/fluid/code/cspace/mvc/tree/SimpleForestApp.java,v 1.6
// 2003/02/06 14:34:48 chance Exp $
package edu.cmu.cs.fluid.mvc.examples;

import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.SimpleProxySupportingAttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.predicate.SimplePredicateViewFactory;
import edu.cmu.cs.fluid.mvc.tree.*;
import edu.cmu.cs.fluid.mvc.tree.syntax.*;
import edu.cmu.cs.fluid.mvc.version.*;
import edu.cmu.cs.fluid.mvc.version.tree.FixedVersionForestProjection;
import edu.cmu.cs.fluid.mvc.version.tree.FixedVersionForestProjectionFactory;
import edu
  .cmu
  .cs
  .fluid
  .mvc
  .version
  .tree
  .syntax
  .FixedVersionSyntaxForestProjection;
import edu
  .cmu
  .cs
  .fluid
  .mvc
  .version
  .tree
  .syntax
  .FixedVersionSyntaxForestProjectionFactory;
import edu.cmu.cs.fluid.mvc.visibility.PredicateBasedVisibilityViewFactory;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.parse.Ellipsis;
import edu.cmu.cs.fluid.render.StyleSetFactory;
import edu.cmu.cs.fluid.render.StyleSetModel;
import edu.cmu.cs.fluid.render.StyledPredicateViewFactory;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.version.Version;

public class SimpleForestApp extends edu.cmu.cs.fluid.util.SimpleApp {
  public static class VizModels {
    public void init(StyleSetModel palette, PredicateModel predModel, VisibilityModel visModel) {
      this.palette = palette;
      this.predModel = predModel;
      this.visModel = visModel;
    }
    public void init(VizModels vm) {
      init(vm.palette, vm.predModel, vm.visModel);
    }
    protected StyleSetModel palette;
    protected PredicateModel predModel;
    protected VisibilityModel visModel;
  }
  
  public interface VizModelsFactory {
    VizModels create(String name, Model source) throws SlotAlreadyRegisteredException;

    VizModelsFactory prototype = new VizModelsFactory() {
      @Override
      public VizModels create(String name, Model source) throws SlotAlreadyRegisteredException {
        // Set up the style palette
        StyleSetModel palette =
          StyleSetFactory.prototype.create(
            name + "style palette",
            SimpleExplicitSlotFactory.prototype);

        // Create the attribute model for the ForestModel
        PredicateModel predModel = SimplePredicateViewFactory.prototype.create(name + "fa", source);
        predModel =
          StyledPredicateViewFactory.prototype.configure(
            predModel,
            palette,
            SimpleSlotFactory.prototype);

        // Init Visibility Model for nodes in the Forest
        VisibilityModel visModel =
          PredicateBasedVisibilityViewFactory.prototype.create(
            name + "Viz Model",
            source,
            predModel);
        
        VizModels vm = new VizModels();
        vm.init(palette, predModel, visModel);
        return vm;
      }
      
    };
  }
  
  public abstract static class AbstractViewEnv extends VizModels
    implements SimpleForestModelChain {
    /**
		 * Logger for this class
		 */
    protected static final Logger LOG =
    	SLLogger.getLogger("CSPACE.simpleForestApp");

    protected final VersionTrackerModel tracker;

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getTracker()
		 */
    @Override
    public VersionTrackerModel getTracker() {
      return tracker;
    }
    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getPalette()
		 */
    @Override
    public StyleSetModel getPalette() {
      return palette;
    }
    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getPredModel()
		 */
    @Override
    public PredicateModel getPredModel() {
      return predModel;
    }
    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getVisibilityModel()
		 */
    @Override
    public VisibilityModel getVisibilityModel() {
      return visModel;
    }

    AbstractViewEnv(VersionTrackerModel vt) {
      tracker = vt;
    }
  }

  public static class ViewEnv extends AbstractViewEnv {
    public final ForestModel forest;
    public FixedVersionForestProjection fixProj;
    public ConfigurableForestView config;

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getBaseModel()
		 */
    @Override
    public ForestModel getBaseModel() {
      return forest;
    }
    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getFixedModel()
		 */
    @Override
    public ForestModel getFixedModel() {
      return fixProj;
    }

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getConfigurableView()
		 */
    @Override
    public ForestModel getConfigurableView() {
      return config;
    }
    
    /*
    ViewEnv(
      ForestModel f,
      FixedVersionForestProjection fixed,
      ConfigurableForestView cfv,      
      VersionTrackerModel base) {
      super(base);
      forest = f;
      fixProj = fixed;
      config = cfv;
    }
    */

    ViewEnv(String name, final ForestModel forest, VizModelsFactory vmf, final VersionTrackerModel base) {
      super(base);
      this.forest = forest;
      try {

        // Create a projection of the ForestModel for the version given by the
        // VersionCursorModel
        fixProj =
          FixedVersionForestProjectionFactory.prototype.create(
            "fixed",
            forest,
            base);

        init(vmf.create(name, forest));

        /*
				 * Set up policy for synthesizing attributes on ellipses and collapsed
				 * nodes -- currently, just sets the operator to Ellipsis
				 */
        ForestProxyAttributePolicy fap = new ProxyPolicy();

        // Create the configurable view with all these policies/models
        // -- but no mutable attributes
        //	final String[] fMutableAttrs = new String[] { "" };

        config =
          ConfigurableForestViewFactory.prototype.create(
            "CFV",
            fixProj,
            visModel,
            SimpleProxySupportingAttributeInheritancePolicy.prototype,
            fap,
            NoEllipsisForestEllipsisPolicy.prototype,
            true,
            true);

        // Setup the initial ellipsis policy on configurable view
        ForestEllipsisPolicy policy = null;
        policy = new MultipleEllipsisForestEllipsisPolicy(config);

        config.setForestEllipsisPolicy(policy);
        config.setViewPathToRoot();
      } catch (SlotAlreadyRegisteredException e) {
        e.printStackTrace();
      }
    }
  }

  public static class SyntaxViewEnv
    extends AbstractViewEnv
    implements SimpleSyntaxForestModelChain {
    public SyntaxForestModel forest;
    public FixedVersionSyntaxForestProjection fixProj;
    public ConfigurableSyntaxForestView config;

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getBaseModel()
		 */
    @Override
    public ForestModel getBaseModel() {
      return forest;
    }
    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getFixedModel()
		 */
    @Override
    public ForestModel getFixedModel() {
      return fixProj;
    }

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.examples.SimpleForestModelChain#getConfigurableView()
		 */
    @Override
    public ForestModel getConfigurableView() {
      return config;
    }

    @Override
    public SyntaxForestModel getBaseSyntaxModel() {
      return forest;
    }

    @Override
    public SyntaxForestModel getFixedSyntaxModel() {
      return fixProj;
    }

    @Override
    public SyntaxForestModel getConfigurableSyntaxView() {
      return config;
    }

    SyntaxViewEnv(
      final String name,
      final SyntaxForestModel forest,
      final VizModelsFactory vmf,
      final VersionTrackerModel base) {
      super(base);
      this.forest = forest;
      int hash = forest.hashCode();
      try {
        LOG.info(
          hash
            + ": Creating a projection of the ForestModel for the version given by the VersionCursorModel");

        
        fixProj = (base == null) ? null :
          FixedVersionSyntaxForestProjectionFactory.prototype.create(
            name + " fixed",
            forest,
            base);
        
        final SyntaxForestModel model = (base == null) ? forest : fixProj;

        init(vmf.create(name, forest));        

        /*
				 * Set up policy for synthesizing attributes on ellipses and collapsed
				 * nodes -- currently, just sets the operator to Ellipsis
				 */
        ForestProxyAttributePolicy fap = new ProxyPolicy();

        /*
        AttributeInheritancePolicy aip = 
        	new SimpleProxySupportingAttributeInheritancePolicy() {
            protected HowToInherit filterNodeAttr( final Model from, final String attr )
				    {
            	if (SyntaxForestModel.OPERATOR.equals(attr)) {
				        return new HowToInherit( attr, attr, ProxyNodeSupport.IMMUTABLE_PROXY,
				                                 Model.STRUCTURAL );
				      } else {
				        return super.filterNodeAttr(from, attr);
				      }
				    }        
          };
        */
        
        LOG.info(
          hash
            + ": Create the configurable view with all these policies/models");
        // -- but no mutable attributes
        //        final String[] fMutableAttrs = new String[] { "" };
        config =
          ConfigurableSyntaxForestViewFactory.prototype.create(
            name + " CFV",
            model,
            visModel,
            new SimpleProxySupportingAttributeInheritancePolicy(),
            fap,
            NoEllipsisForestEllipsisPolicy.prototype,
            true,
            true);

        LOG.info(
          hash + ": Setup the initial ellipsis policy on configurable view");
        ForestEllipsisPolicy policy = null;
        policy = new MultipleEllipsisSynForestEllipsisPolicy(config);
        config.setForestEllipsisPolicy(policy);

        LOG.info(hash + ": showPathToRoot = " + config.isViewPathToRoot());
        // config.setShowPathToRoot(true);
      } catch (SlotAlreadyRegisteredException e) {
        e.printStackTrace();
      }
      LOG.info(hash + ": Done creating views");
    }
  }

  /// createVersionSpace
  /**
	 * Creates a VersionSpaceModel with the given name and with one
	 * VersionCursorModel, initialized to the given Version
	 */
  public static VersionSpaceModel createVersionSpace(
    String name,
    Version initVersion) {
    try {
      final VersionSpaceModel vsv =
        VersionSpaceFactory.prototype.create(
          "versionSpace",
          initVersion,
          new String[] { "cursor" });

      // Setup version cursor to the root version of the space, and to follow
      // upon changes to that version
      final VersionCursorModel base =
        (VersionCursorModel) vsv.getCursors().elementAt(0);
      base.setVersion(initVersion);
      base.setFollowing(true);

      return vsv;
    } catch (SlotAlreadyRegisteredException e) {
      e.printStackTrace();
      return null;
    }
  }

  /// createForest
  /**
	 * Creates a versioned ForestModel based on an existing Tree
	 */
  public static ForestModel createForest(Tree tree, String name) {
    try {
      // Create an ur-root node to be the parent for all the trees in the
			// forest
      final IRNode n = new PlainIRNode();
      tree.initNode(n, -1);

//      final SlotFactory sf = JJNode.treeSlotFactory;
      final SlotFactory sf = SimpleSlotFactory.prototype;      
      final IRSequence<IRNode> roots = sf.newSequence(1);
      roots.setElementAt(n, 0);

      return (new DelegatingPureForestFactory(tree, roots, true)).create(
        name,
        sf);
    } catch (SlotAlreadyRegisteredException e) {
      e.printStackTrace();
      return null;
    }
  }

  /// createConfigurableView
  /**
	 * Create a ConfigurableView for the ForestModel, along with
	 */
  public static SimpleForestModelChain createConfigurableView(
    String name, 
    ForestModel forest,
    VersionTrackerModel base) {
    return new ViewEnv(name, forest, VizModelsFactory.prototype, base);
  }

  /// createSyntaxForest
  /**
	 * Creates a versioned SyntaxForestModel based on an existing SyntaxTree
	 */
  public static SyntaxForestModel createSyntaxForest(
    SyntaxTreeInterface tree,
    String name) {
    try {
      // Create an ur-root node to be the parent for all the trees in the
			// forest
      IRNode n = new MarkedIRNode("Root for forest: "+name);
      tree.initNode(n, Ellipsis.prototype, -1);
      // tree.initNode(n, -1);

//      final SlotFactory sf = JJNode.treeSlotFactory;
      final SlotFactory sf = SimpleSlotFactory.prototype;
      final IRSequence<IRNode> roots = sf.newSequence(~1);
      roots.setElementAt(n, 0);
      System.err.println("Finished init of pure forest at "+Version.getVersion());
      
      return (new DelegatingPureSyntaxForestFactory(tree, roots, true)).create(
        name,
        sf);
    } catch (SlotAlreadyRegisteredException e) {
      e.printStackTrace();
      return null;
    }
  }

  /// createConfigurableView
  /**
	 * Create a ConfigurableView for the SyntaxForestModel, along with
	 */
  public static SimpleSyntaxForestModelChain createConfigurableView(
    String name,
    SyntaxForestModel forest,
    VizModelsFactory vmf, 
    VersionTrackerModel base) {
    return new SyntaxViewEnv(name, forest, vmf, base);
  }

  /// ProxyPolicy
  protected static class ProxyPolicy implements ForestProxyAttributePolicy {
    final AVPair[] pairs = new AVPair[0];
  	/*
    final AVPair[] pairs = new AVPair[1];
    {
      pairs[0] = new AVPair(SyntaxForestModel.OPERATOR, Ellipsis.prototype);
    }
    */
    @Override
    public AVPair[] attributesFor(Model model, Set skippedNodes) {
      return pairs;
    }
    @Override
    public AVPair[] attributesFor(ForestModel model, IRNode root) {
      return pairs;
    }
  }
}
