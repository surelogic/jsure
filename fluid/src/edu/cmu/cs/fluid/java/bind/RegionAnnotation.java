package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 */
@Deprecated
public class RegionAnnotation extends AbstractPromiseAnnotation {

  private static final String REGION = "Region";

  private static SlotInfo<IRSequence<IRNode>> classDeclsSI;

  private static SlotInfo<IRSequence<IRNode>> fieldMappingsSI;

  private static SlotInfo<IRNode> fieldDeclSI;

  private static SlotInfo<IRNode> fieldAggregateSI;

  private static SlotInfo<RegionModel> regionDrop = SimpleSlotFactory.prototype
      .newAttribute(null);

  private static SlotInfo<ExplicitUniqueInRegionPromiseDrop> aggregateDrop = SimpleSlotFactory.prototype
      .newAttribute(null);

  private static SlotInfo<InRegionPromiseDrop> inRegionDrop = SimpleSlotFactory.prototype
      .newAttribute(null);

  private static SlotInfo<MapFieldsPromiseDrop> mapFieldsDrop = SimpleSlotFactory.prototype
      .newAttribute(null);

  private RegionAnnotation() {
    // private constructor for singleton creation
  }

  private static final RegionAnnotation instance = new RegionAnnotation();

  public static final RegionAnnotation getInstance() {
    return instance;
  }

  /**
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] {
        new Region_ParseRule(REGION, typeDeclOps), //$NON-NLS-1$
        //new Region_ParseRule("MapRegion", noOps), //$NON-NLS-1$
        new MapFields_ParseRule(), new InRegion_ParseRule(),
        new Aggregate_ParseRule(), new RegionName_BindRule(),
        new QualifiedRegionName_BindRule(),

        new RegionsOnce_CompleteRegions() };
  }

  /**
   * Get the visibility of a region.
   * 
   * @param regDecl
   *          A NewRegionDeclaration or VariableDeclarator node
   */
  public static Visibility getRegionVisibility(final IRNode regDecl) {
    final Operator op = tree.getOperator(regDecl);
    if (NewRegionDeclaration.prototype.includes(op)) {
      return Visibility.getVisibilityOf(regDecl);
    } else if (RegionMapping.prototype.includes(op)) {
      return Visibility.DEFAULT;
    } else { // VariableDeclarator
      try {
        return Visibility.getVisibilityOf(
            tree.getParent(tree.getParent(regDecl)));
      } catch (NullPointerException e) {
//        System.out.println(DebugUnparser.toString(regDecl));
        return Visibility.DEFAULT;
      }
    }
  }

  /**
   * Accessors to promise storage
   */
  public static Iterator<IRNode> classRegions(IRNode classNode) {
    return getEnum_filtered(classDeclsSI, classNode);
  }

  /**
   * Add a region declaration node to the list of regions for this class
   * declaration node. It does not check to see this region declaration node is
   * already in the list.
   */
  public static void addClassRegion(IRNode classNode, IRNode regionNode) {
    addToSeq_mapped(classDeclsSI, classNode, regionNode);
    getRegionDrop(regionNode);
  }

  /**
   * Remove a region declaration node from the list of regions for this class
   * declaration node. It returns true if the region node was found (and
   * removed).
   */
  public static boolean removeClassRegion(IRNode classNode, IRNode regionNode) {
    return removeFromEnum_mapped(classDeclsSI, classNode, regionNode);
  }

  public static Iterator<IRNode> fieldMappings(IRNode classNode) {
    return getEnum_filtered(fieldMappingsSI, classNode);
  }

  public static void addFieldMappings(IRNode classNode, IRNode mappingNode) {
    addToSeq_mapped(fieldMappingsSI, classNode, mappingNode);
    getMapFieldsDrop(mappingNode);
  }

  public static boolean removeFieldMappings(IRNode classNode, IRNode mappingNode) {
    return removeFromEnum_mapped(fieldMappingsSI, classNode, mappingNode);
  }

  public static IRNode getFieldRegionOrNull(IRNode fieldNode) {
    return getXorNull_filtered(fieldDeclSI, fieldNode);
  }

  public static void setFieldRegion(IRNode fieldNode, IRNode region) {
    setX_mapped(fieldDeclSI, fieldNode, region);
    getInRegionDrop(fieldNode); // create the drop
  }

  public static IRNode getFieldAggregationOrNull(IRNode fieldNode) {
    return getXorNull_filtered(fieldAggregateSI, fieldNode);
  }

  public static void setFieldAggregation(IRNode fieldNode, IRNode region) {
    setX_mapped(fieldAggregateSI, fieldNode, region);
    getAggregateDrop(fieldNode); // create the drop
  }

  public static ExplicitUniqueInRegionPromiseDrop getAggregateDrop(IRNode fieldNode) {
    ExplicitUniqueInRegionPromiseDrop result = fieldNode.getSlotValue(aggregateDrop);
    if (result != null) {
      return result;
    } else {
      if (fieldNode.valueExists(fieldAggregateSI)) {
        result = new ExplicitUniqueInRegionPromiseDrop(null);
        result.setMessage(Messages.RegionAnnotation_prescrubbedAggregate); //$NON-NLS-1$
        // the rest is filled in by the promise scrubber
        fieldNode.setSlotValue(aggregateDrop, result);
        result.setAttachedTo(fieldNode, aggregateDrop);
        result.setNodeAndCompilationUnitDependency(fieldNode);
        return result;
      } else
        return null;
    }
  }

  public static MapFieldsPromiseDrop getMapFieldsDrop(IRNode typeNode) {
    MapFieldsPromiseDrop result = typeNode.getSlotValue(mapFieldsDrop);
    if (result != null) {
      return result;
    } else {
      if (typeNode.valueExists(fieldMappingsSI)) {
        result = new MapFieldsPromiseDrop(null);
        result.setMessage(Messages.RegionAnnotation_prescrubbedInRegion); //$NON-NLS-1$
        // the rest is filled in by the promise scrubber
        typeNode.setSlotValue(mapFieldsDrop, result);
        result.setAttachedTo(typeNode, mapFieldsDrop);
        result.setNodeAndCompilationUnitDependency(typeNode);
        return result;
      } else
        return null;
    }
  }

  public static InRegionPromiseDrop getInRegionDrop(IRNode fieldNode) {
    InRegionPromiseDrop result = fieldNode.getSlotValue(inRegionDrop);
    if (result != null) {
      return result;
    } else {
      if (fieldNode.valueExists(fieldDeclSI)) {
        result = new InRegionPromiseDrop(null);
        result.setMessage(Messages.RegionAnnotation_prescrubbedInRegion); //$NON-NLS-1$
        // the rest is filled in by the promise scrubber
        fieldNode.setSlotValue(inRegionDrop, result);
        result.setAttachedTo(fieldNode, inRegionDrop);
        result.setNodeAndCompilationUnitDependency(fieldNode);
        return result;
      } else
        return null;
    }
  }

  public static RegionModel getRegionDrop(IRNode regionNode) {
    if (regionNode == null) {
      return null;
    }
    RegionModel model = regionNode.getSlotValue(regionDrop);
    if (model != null) {
      return model;
    } else {
      // no region drop found, so create one
      final String regionName = getRegionName(regionNode);
      model = RegionModel.getInstance(regionName, "");
      model.setCategory(JavaGlobals.REGION_CAT);
      
      boolean predefined = regionName.equals("Instance") //$NON-NLS-1$
          || regionName.equals("All") || regionName.equals("[]"); //$NON-NLS-1$ //$NON-NLS-2$
      if (!predefined) {
        model.setNodeAndCompilationUnitDependency(regionNode);
      }
      String visibility;
      switch (getRegionVisibility(regionNode)) {
      case PRIVATE:
        visibility = " private "; //$NON-NLS-1$
        break;
      case PROTECTED:
        visibility = " protected "; //$NON-NLS-1$
        break;
      case PUBLIC:
        visibility = " public "; //$NON-NLS-1$
        break;
      default:
        visibility = " ";
        break;
      }
      model.setResultMessage(Messages.RegionAnnotation_regionDrop, visibility,
          getRegionName(regionNode), JavaNames.getTypeName(VisitUtil
              .getEnclosingType(regionNode)));
      regionNode.setSlotValue(regionDrop, model);
      model.setAttachedTo(regionNode, regionDrop);
      
      // TODO added temporarily
      IRNode type = JavaPromise.getPromisedFor(regionNode);
      PromiseFramework.getInstance().findSeqStorage(REGION).add(type, model);
      return model;
    }
  }

  public static String getRegionName(IRNode regionNode) {
    String result = "(REGION-UNKNOWN)"; //$NON-NLS-1$

    Operator lop = tree.getOperator(regionNode);
    if (NewRegionDeclaration.prototype.includes(lop)) {
      result = NewRegionDeclaration.getId(regionNode);
    } else if (VariableDeclarator.prototype.includes(lop)) {
      result = VariableDeclarator.getId(regionNode);
    } else if (RegionMapping.prototype.includes(lop)) {
      final IRNode regSpec = RegionMapping.getFrom(regionNode);
      result = RegionSpecification.getId(regSpec);
    } else {
      LOG.log(Level.SEVERE, "getRegionName() couldn't find a name on " //$NON-NLS-1$
          + DebugUnparser.toString(regionNode));
//      System.out.println("getRegionName() couldn't find a name on " //$NON-NLS-1$
//          + DebugUnparser.toString(regionNode));
    }
    return result;
  }

  /**
   * Get all the regions (fields and abstract regions) declared in the given
   * class
   * 
   * @param classDecl
   *          a ClassDeclaration node
   * @return A set of VariableDeclarators and NewRegionDeclaration nodes
   * @see getAllRegionsInClass
   */
  public static Set<IRNode> getDeclaredRegions(final IRNode classDecl) {
    final Set<IRNode> result = new HashSet<IRNode>();
    getDeclaredRegions(classDecl, result);
    return Collections.unmodifiableSet(result);
  }

  /**
   * Get all the regions present in a given class, that is all the regions
   * inherited by the class plus all the regions declared in the class.
   * 
   * @param classDecl
   *          a ClassDeclaration node <em>or</em> an ArrayDeclaration node
   * @return A set of VariableDeclarators and NewRegionDeclaration nodes
   * @see getDeclaredRegions
   */
  public Set<IRNode> getAllRegionsInClass(final IJavaType type) {
    final Set<IRNode> result = new HashSet<IRNode>();
    IJavaType currentType = type;
    while (currentType != null) {
      if (currentType instanceof IJavaDeclaredType) {
        IJavaDeclaredType ct = (IJavaDeclaredType) currentType;
        IRNode currentClass = ct.getDeclaration();
        getDeclaredRegions(currentClass, result);
      }
      // Get the annos from the super class
      currentType = currentType.getSuperclass(binder.getTypeEnvironment());
    }
    return Collections.unmodifiableSet(result);
  }

  /*
   * public Set<IRNode> getAllRegionsInClass(final IJavaType type) { if (type
   * instanceof IJavaDeclaredType) { return
   * getAllRegionsInClass(((IJavaDeclaredType) type).getDeclaration()); } else
   * if (type instanceof IJavaArrayType) { return
   * getAllRegionsInClass(IOldTypeEnvironment.arrayType); } return
   * Collections.emptySet(); }
   */

  /**
   * Helper method: gets all the region declarations of the given class and puts
   * them in the given set.
   * 
   * @param classDecl
   *          A ClassDeclaration
   * @param result
   *          A Set
   */
  private static void getDeclaredRegions(final IRNode classDecl,
      final Set<IRNode> result) {
    final Iterator<IRNode> regions = classRegions(classDecl);
    while (regions.hasNext()) {
      result.add(regions.next());
    }
    final Iterator<IRNode> fieldDecls = VisitUtil.getClassFieldDecls(classDecl);
    while (fieldDecls.hasNext()) {
      final IRNode fd = fieldDecls.next();
      final Iterator<IRNode> fields = VariableDeclarators
          .getVarIterator(FieldDeclaration.getVars(fd));
      while (fields.hasNext()) {
        result.add(fields.next());
      }
    }
  }

  /*
   * Private implementation
   */
  // private boolean checkRegionSpec(
  // String format, IPromiseCheckReport report, IRNode promisedFor,
  // IRNode promise, IRNode spec)
  // {
  // IRNode b = binder.getBinding(spec);
  // if (b != null) {
  // return true;
  // }
  // final String msg =
  // MessageFormat.format(format, new Object[] { DebugUnparser.toString(spec)
  // });
  //    
  // report.reportError(msg, promise);
  // return false;
  // }
  public static IRNode findRegionInType(IRNode type, String region) {
    return BindUtil.findRegionInType(type, region);
  }

  public IRNode findRegion(IRNode type, String region) {
    Operator op = tree.getOperator(type);
    if (LOG.isLoggable(Level.FINE)) {
      if (ArrayDeclaration.prototype.includes(op)) {
        LOG.fine("Searching for region " + region + " in array: " //$NON-NLS-1$ //$NON-NLS-2$
            + DebugUnparser.toString(ArrayDeclaration.getBase(type)) + ", " //$NON-NLS-1$
            + ArrayDeclaration.getDims(type));
      } else {
        LOG.fine("Searching for region " + region + ", starting with " //$NON-NLS-1$ //$NON-NLS-2$
            + DebugUnparser.toString(type));
      }
    }
    // Search in superclasses
    IRNode rv = (IRNode) binder.findClassBodyMembers(type,
        new FindRegionStrategy(binder, region), false);
    if (rv != null) {
      return rv;
    }
    // Search in outerclasses?
    return null;
  }

  // for @region
  class RegionsOnce_CompleteRegions extends AbstractPromiseRule implements
      IPromiseCheckRule {

    public RegionsOnce_CompleteRegions() {
      super(typeDeclOps);
    }

    public boolean checkSanity(Operator op, IRNode promisedFor,
        IPromiseCheckReport report) {
      boolean result = true;
      final Set<String> uniqueRegionNames = new HashSet<String>();
      final Map<IRNode, TreeNode> declsToNodes = new HashMap<IRNode, TreeNode>();

      /*
       * Prime the set of regionNames with the fields declared in the class.
       */
      for (IRNode vdecl : VisitUtil.getClassFieldDeclarators(promisedFor)) {
        uniqueRegionNames.add(VariableDeclarator.getId(vdecl));
      }

      // Check class regions (NewRegionDecl)
      Iterator<IRNode> enm = classRegions(promisedFor);
      while (enm.hasNext()) {
        IRNode regionDecl = enm.next();
        Operator lop = tree.getOperator(regionDecl);
        if (NewRegionDeclaration.prototype.includes(lop)) {
          /*
           * >> RegionsOnce <<
           */
          final String name = NewRegionDeclaration.getId(regionDecl);
          if (uniqueRegionNames.contains(name)) {
            report.reportError("region \"" + name //$NON-NLS-1$
                + "\" already declared in class", regionDecl); //$NON-NLS-1$
          } else {
            uniqueRegionNames.add(name);
          }

          /* Check properties of the region hierarchy */
          final IRNode parentRegion = NewRegionDeclaration
              .getParent(regionDecl);
          if (parentRegion != null) {
            /*
             * >> CompleteRegions << (see also checker for @inRegion)
             */
            final IRNode parentDecl = binder.getBinding(parentRegion);
            if (parentDecl == null) {
              report.reportError("superregion \"" //$NON-NLS-1$
                  + DebugUnparser.toString(parentRegion) + "\" does not exist", //$NON-NLS-1$
                  regionDecl);
            } else {
              final Operator pop = tree.getOperator(parentDecl);
              if (!NewRegionDeclaration.prototype.includes(pop)) {
                report.reportError("superregion \"" //$NON-NLS-1$
                    + DebugUnparser.toString(parentRegion)
                    + "\" is not an abstract region", regionDecl); //$NON-NLS-1$
              } else {
                final String parentName = NewRegionDeclaration
                    .getId(parentDecl);

                /*
                 * >> Check Visibility of region << Cannot be more visible than
                 * parent (see also checker for @inRegion)
                 */
                if (!Visibility.atLeastAsVisibleAs(parentDecl, regionDecl)) {
                  report.reportError("region \"" + name //$NON-NLS-1$
                      + "\" is more visible than its superregion \"" //$NON-NLS-1$
                      + parentName + "\"", regionDecl); //$NON-NLS-1$
                }

                /*
                 * Check that static regions are not put into instance regions
                 */
                final boolean regionIsStatic = JavaNode.getModifier(regionDecl,
                    JavaNode.STATIC);
                final boolean parentIsStatic = JavaNode.getModifier(parentDecl,
                    JavaNode.STATIC);
                if (regionIsStatic && !parentIsStatic) {
                  report.reportError("static region \"" + name //$NON-NLS-1$
                      + " has non-static superregion " + parentName + "\"", //$NON-NLS-1$ //$NON-NLS-2$
                      regionDecl);
                }

                /*
                 * >> WFRegion << Check for cycles in the region hierarchy. (1)
                 * Induction hypothesis: No cycles in the inherited regions.
                 * (induction over the class hierarchy) (a) Cannot reparent an
                 * inherited region, so an inherited region cannot take part in
                 * a cycle (2) Check for cycles among the newly declared
                 * regions. (a) Only care about parents when they are a locally
                 * declared region, which is why we can ignore the case of the
                 * null (that is, implicit) superregion) [parentRegion != null
                 * check in outermost if-statement]
                 */
                // Get the node for the parent region --- if it doesn't exist,
                // create the node with a NULL parent.
                TreeNode parentNode = declsToNodes.get(parentDecl);
                if (parentNode == null) {
                  parentNode = new TreeNode();
                  declsToNodes.put(parentDecl, parentNode);
                }

                // Get the node for the region being declared. May already
                // exist if a reference to the region has already been made.
                // Make sure the parent pointer points to the parent node
                TreeNode declNode = declsToNodes.get(regionDecl);
                if (declNode == null)
                  declNode = new TreeNode(parentNode);
                else
                  declNode.parent = parentNode;

                // Check for cycle
                if (declNode.createsCycle()) {
                  report.reportError("region \"" + name //$NON-NLS-1$
                      + "\" creates a cycle in the regin hierarchy", //$NON-NLS-1$
                      regionDecl);
                } else {
                  declsToNodes.put(regionDecl, declNode);
                }
              }
            }
          }
        }
      }

      return result;
    }
  }

  private static class TreeNode {

    public TreeNode parent;

    public TreeNode() {
      parent = null;
    }

    public TreeNode(TreeNode p) {
      parent = p;
    }

    public boolean createsCycle() {
      TreeNode current = parent;
      while ((current != null) && (current != this)) {
        current = current.parent;
      }
      return current == this;
    }
  }

  // For @region and @mapRegion
  class Region_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {

    public Region_ParseRule(String name, Operator[] _ops) {
      super(name, SEQ, false, typeDeclOps, _ops);
    }

    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      classDeclsSI = si;
      return new TokenInfo<IRSequence<IRNode>>("Declared regions", si, "region"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      addClassRegion(n, result);

      final Operator op = tree.getOperator(result);
      if (op instanceof NewRegionDeclaration && !tree.hasChild(result, 0)) {
        LOG
            .fine("NewRegionDeclaration doesn't extend anything -- setting to null (default super-region)"); //$NON-NLS-1$
        tree.setChild(result, 0, null);
      }
      return true;
    }

    @Override
    public boolean checkSanity(Operator pop, IRNode promisedFor,
        IPromiseCheckReport report) {
      boolean result = true;

      // Check class regions (NewRegionDecl|RegionMapping)
      Iterator<IRNode> enm = getIterator(classDeclsSI, promisedFor);
      while (enm.hasNext()) {
        IRNode regionDecl = enm.next();
        Operator op = tree.getOperator(regionDecl);
        if (op instanceof NewRegionDeclaration) {
          IRNode parentRegion = binder.getRegionParent(regionDecl);
          if (parentRegion != null) {
            RegionModel regionModel = getRegionDrop(regionDecl);
            if (!regionModel.regionName.equals("All")) { //$NON-NLS-1$
              RegionModel parentRegionModel = getRegionDrop(parentRegion);
              regionModel.addSupportingInformation("parent region: " //$NON-NLS-1$
                  + parentRegionModel.regionName, parentRegionModel.getNode());
              // ResultDrop link = new ResultDrop();
              // link.addCheckedPromise(regionModel);
              // link.addTrustedPromise(parentRegionModel);
              // link.setMessage("parent region:");
              // link.setConsistent();
              // link.setNodeAndCompilationUnitDependency(regionModel.getNode());
            }
            // result = result && checkRegionSpec(report, promisedFor, parent);
          }
        } else {
          // Assumed to be a RegionMapping
          // result = result && checkRegionSpec("Couldn't bind \"{0}\"", report,
          // promisedFor, n, RegionMapping.getFrom(n));
          // result = result && checkRegionSpec("Couldn't bind \"{0}\"", report,
          // promisedFor, n, RegionMapping.getTo(n));
        }
      }
      return result;
    }
  }

  class MapFields_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {
    public MapFields_ParseRule() {
      super("MapFields", SEQ, true, fieldOrTypeOp, typeDeclOps); //$NON-NLS-1$
    }

    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      fieldMappingsSI = si;
      return new TokenInfo<IRSequence<IRNode>>("Field mappings", si, "mapFields"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb, Collection<IRNode> results) {
      boolean rv = true;
      IRNode temp = VisitUtil.getEnclosingType(n);
      final IRNode type = (temp != null) ? temp : n;
      final IRNode body = VisitUtil.getClassBody(type);
      addFieldMappings(type, result);
      results.add(result);

      /*
      // to link up the various drops
      ScopedPromises.getInstance().setCurrentDrop(getMapFieldsDrop(result));
      */
      try {
        // XXX hack to get src ref produced early
        cb.parsed(result);
        final ISrcRef ref = JavaNode.getSrcRef(result);

        final IRNode mappings = FieldMappings.getFields(result);
        final IRNode region = FieldMappings.getTo(result);
        Iterator<IRNode> specs = RegionSpecifications
            .getSpecsIterator(mappings);
        while (specs.hasNext()) {
          final IRNode name = specs.next();
          final String field = RegionName.getId(name);
          final IRNode vdecl = BindUtil.findFieldInBody(body, field);

          // LOG.debug("2 Mapping field "+VariableDeclarator.getId(vdecl)+" to
          // "+JavaNode.getInfo(reg));
          if (vdecl != null) {
            final IRNode region2 = copyRegion(region);
            if (ref != null) {
              JavaNode.setSrcRef(region2, ref);
            }
            setFieldRegion(vdecl, region2);
            results.add(region2);
            continue;
          }
          cb
              .noteProblem("Couldn't find field '" + field + "' to map to " + DebugUnparser.toString(region)); //$NON-NLS-1$ //$NON-NLS-2$
          rv = false;
        }
      } finally {
    	//ScopedPromises.getInstance().clearCurrentDrop();
      }
      return rv;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.promise.IPromiseCheckRule#checkSanity(edu.cmu.cs.fluid.tree.Operator,
     *      edu.cmu.cs.fluid.ir.IRNode,
     *      edu.cmu.cs.fluid.promise.IPromiseCheckReport)
     */
    @Override
    public boolean checkSanity(Operator op, IRNode promisedFor,
        IPromiseCheckReport report) {
      final Iterator<IRNode> enm = getIterator(fieldMappingsSI, promisedFor);
      while (enm.hasNext()) {
        final IRNode mappings = enm.next();
        final IRNode parentRegion = FieldMappings.getTo(mappings);
        MapFieldsPromiseDrop mfp = getMapFieldsDrop(mappings);
        mfp.setNode(mappings);
        mfp.dependUponCompilationUnitOf(promisedFor);

        final IRNode maps = FieldMappings.getFields(mappings);
        final Iterator<IRNode> fields = RegionSpecifications
            .getSpecsIterator(maps);
        final StringBuilder buf = new StringBuilder();
        IRNode field = fields.next();
        buf.append(RegionName.getId(field));
        while (fields.hasNext()) {
          field = fields.next();
          buf.append(", "); //$NON-NLS-1$
          buf.append(RegionName.getId(field));
        }
        final String fieldNames = buf.toString();

        if (parentRegion != null) {
          final IRNode parentDecl = binder.getBinding(parentRegion);

          // check the target region?

          RegionModel model = getRegionDrop(parentDecl);
          mfp.setResultMessage(Messages.RegionAnnotation_mapFieldsDrop, fieldNames,
              model.regionName); //$NON-NLS-1$
          model.addDependent(mfp);
        } else {
          mfp.setMessage(Messages.RegionAnnotation_parentRegionDrop,
              DebugUnparser.toString(parentRegion), fieldNames); //$NON-NLS-1$
        }
      }
      return true;
    }
  }

  /**
   * @param region
   * @return
   */
  private static IRNode copyRegion(IRNode region) {
    // JavaNode.copyTree(region);
    Operator op = tree.getOperator(region);
    if (RegionName.prototype.includes(op)) {
      return RegionName.createNode(RegionName.getId(region));
    } else {
      final IRNode nt = QualifiedRegionName.getType(region);
      final String type = NamedType.getType(nt);
      final IRNode nt2 = NamedType.createNode(type);
      return QualifiedRegionName.createNode(nt2, QualifiedRegionName
          .getId(region));
    }
  }

  class InRegion_ParseRule extends AbstractPromiseParserCheckRule<IRNode> {

    Logger LOG = AbstractPromiseAnnotation.LOG;

    public InRegion_ParseRule() {
      super("MapInto", NODE, false, FieldDeclaration.prototype, //$NON-NLS-1$
          VariableDeclarator.prototype);
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      final boolean debug = LOG.isLoggable(Level.FINE);
      final Iterator<IRNode> vars = getVarsForField(n);
      while (vars.hasNext()) {
        final IRNode vdecl = vars.next();
        if (debug) {
          LOG.fine("1 Mapping field " + VariableDeclarator.getId(vdecl)); //$NON-NLS-1$
        }
        final IRNode region = copyRegion(result);
        setFieldRegion(vdecl, region);
      }
      return true;
    }

    public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
      fieldDeclSI = si;
      return new TokenInfo<IRNode>("Field mapping", si, "inRegion"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public boolean checkSanity(Operator op, IRNode promisedFor,
        IPromiseCheckReport report) {
      boolean result = true;

      /*
       * >> RegionsOnce << This check is taken care of by the Java compiler and
       * by the check on NewRegionDeclarations.
       */

      final String name = VariableDeclarator.getId(promisedFor);

      /* Get the parent region */
      final IRNode parentRegion = getXorNull(fieldDeclSI, promisedFor);
      if (parentRegion != null) {
        /*
         * >> CompleteRegions << (see also checker for @region)
         */
        final IRNode parentDecl = binder.getBinding(parentRegion);
        if (parentDecl == null) {
          report.reportError("superregion \"" //$NON-NLS-1$
              + DebugUnparser.toString(parentRegion) + "\" does not exist", //$NON-NLS-1$
              parentRegion);
          result = false;
        } else {
          final Operator pop = tree.getOperator(parentDecl);
          if (!NewRegionDeclaration.prototype.includes(pop)) {
            report.reportError("superregion \"" //$NON-NLS-1$
                + DebugUnparser.toString(parentRegion)
                + "\" is not an abstract region", parentRegion); //$NON-NLS-1$
            result = false;
          } else {
            final String parentName = NewRegionDeclaration.getId(parentDecl);

            /*
             * >> Check Visibility of region << Cannot be more visible than
             * parent (see also checker for @inRegion)
             */
            final IRNode fieldDecl = tree
                .getParent(tree.getParent(promisedFor));
            if (!Visibility.atLeastAsVisibleAs(parentDecl, fieldDecl)) {
              report.reportError("region \"" + name //$NON-NLS-1$
                  + "\" is more visible than its superregion \"" + parentName //$NON-NLS-1$
                  + "\"", parentRegion); //$NON-NLS-1$
              result = false;
            }

            /*
             * Check that static regions are not put into instance regions
             */
            final boolean regionIsStatic = JavaNode.getModifier(fieldDecl,
                JavaNode.STATIC);
            final boolean parentIsStatic = JavaNode.getModifier(parentDecl,
                JavaNode.STATIC);
            if (regionIsStatic && !parentIsStatic) {
              report.reportError("static region \"" + name //$NON-NLS-1$
                  + "\" has non-static superregion \"" + parentName + "\"", //$NON-NLS-1$ //$NON-NLS-2$
                  parentRegion);
              result = false;
            }

            /*
             * Check for cycles in the hierarchy --- Nothing to do because
             * @inRegion cannot cause a cycle because fields are not allowed to
             * be used as superregions.
             */
          }
        }
        RegionModel model = getRegionDrop(parentDecl);
        InRegionPromiseDrop mip = getInRegionDrop(promisedFor);
        mip.setResultMessage(Messages.RegionAnnotation_inRegionDrop, model.regionName,
            name); //$NON-NLS-1$
        mip.setNode(parentRegion);
        mip.dependUponCompilationUnitOf(promisedFor);
        model.addDependent(mip);
      }

      return result;
    }
  }

  class Aggregate_ParseRule extends AbstractPromiseParserCheckRule<IRNode> {

    Logger LOG = AbstractPromiseAnnotation.LOG;

    public Aggregate_ParseRule() {
      super("Aggregate", NODE, false, FieldDeclaration.prototype, //$NON-NLS-1$
          VariableDeclarator.prototype);
    }

    public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
      fieldAggregateSI = si;
      return new TokenInfo<IRNode>("Region aggregations", si, "aggregate"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      final IRNode vdecl = getFirstVarForField(n);
      LOG.fine("Aggregating field " + VariableDeclarator.getId(vdecl)); //$NON-NLS-1$
      setFieldAggregation(vdecl, result);
      return true;
    }

    @Override
    public boolean checkSanity(Operator op, IRNode promisedFor,
        IPromiseCheckReport report) {
      boolean result = true;

      final IRNode aggPromise = getXorNull(fieldAggregateSI, promisedFor);
      if (aggPromise != null) {
        final IRNode fieldDecl = tree.getParent(tree.getParent(promisedFor));
        /*
         * final IRNode typeDecl = binder.getBinding(FieldDeclaration
         * .getType(fieldDecl));
         */
        final IJavaType type = binder.getJavaType(promisedFor);

        // map from field/region to aggregating field/region (or null)
        final Map<IRNode, MapRecord> regions = new HashMap<IRNode, MapRecord>();

        // Init region map
        final Iterator<IRNode> iter = getAllRegionsInClass(type).iterator();
        while (iter.hasNext()) {
          regions.put(iter.next(), new MapRecord());
        }
        /*
        // Check that the field is also declared to be unique
        if (!UniquenessAnnotation.isUnique(promisedFor)) {
          report
              .reportError(
                  "field is not declared \"@unique\" --- cannot aggregate its regions", //$NON-NLS-1$
                  aggPromise);
          result = false;
        }
        */
        ExplicitUniqueInRegionPromiseDrop ap = getAggregateDrop(promisedFor);
        ap.setNode(aggPromise);
        ap.dependUponCompilationUnitOf(promisedFor);
        PromiseDrop unique = null;//UniquenessAnnotation.getUniqueDrop(promisedFor);
        if (unique != null) {
          ResultDrop link = new ResultDrop();
          link.addCheckedPromise(ap);
          link.addTrustedPromise(unique);
          link.setNodeAndCompilationUnitDependency(unique.getNode());
          link.setConsistent();
          link.setMessage(Messages.RegionAnnotation_aggregationAllowedDrop); //$NON-NLS-1$
        } else {
          ResultDrop link = new ResultDrop();
          link.addCheckedPromise(ap);
          link.setInconsistent();
          link.setMessage(Messages.RegionAnnotation_aggregationDisallowedDrop); //$NON-NLS-1$
        }
        ap.setMessage(Messages.RegionAnnotation_aggregateDrop); //$NON-NLS-1$

        final Iterator<IRNode> maps = MappedRegionSpecification
            .getMappingIterator(aggPromise);
        while (maps.hasNext()) {
          final IRNode mapping = maps.next();
          final IRNode fromName = RegionMapping.getFrom(mapping);
          final IRNode fromDecl = binder.getBinding(fromName);
          final String fromId = RegionName.getId(fromName);
          final IRNode toName = RegionMapping.getTo(mapping);
          final IRNode toDecl = binder.getBinding(toName);
          final String toId = RegionSpecification.getId(toName);

          /*
           * >> AggregationOK << --- make sure the regions exist
           */
          if (fromDecl == null) {
            report.reportError("source region \"" + fromId //$NON-NLS-1$
                + "\" is not declared in aggregated class", aggPromise); //$NON-NLS-1$
            result = false;
          }
          if (toDecl == null) {
            report.reportError("destination region \"" + toId //$NON-NLS-1$
                + "\" is not declared", aggPromise); //$NON-NLS-1$
            result = false;
          }
          if ((fromDecl != null) && (toDecl != null)) {
            RegionModel model = RegionAnnotation.getRegionDrop(toDecl);
            // add to global region definition (binding to other defining
            // promises)
            model.addDependent(ap);

            //****MIGHT NEED CHANGE****//
            // sr.addDependent(ap);
            ap.setMessage(ap.getMessage() + " "
                + RegionAnnotation.getRegionName(fromDecl) + " into " //$NON-NLS-1$
                + RegionAnnotation.getRegionName(toDecl));
            /*
             * Check that the dest region is an abstract region
             */
            final boolean destIsAbstract = NewRegionDeclaration.prototype
                .includes(tree.getOperator(toDecl));
            if (!destIsAbstract) {
              report.reportError("destination region \"" + toId //$NON-NLS-1$
                  + "\" is not abstract", aggPromise); //$NON-NLS-1$
              result = false;
            }

            /*
             * Check that the source region is not static
             */
            boolean isSrcStatic;
            if (NewRegionDeclaration.prototype.includes(tree
                .getOperator(fromDecl))) {
              isSrcStatic = JavaNode.getModifier(fromDecl, JavaNode.STATIC);
            } else {
              isSrcStatic = JavaNode.getModifier(tree.getParent(tree
                  .getParent(fromDecl)), JavaNode.STATIC);
            }
            if (isSrcStatic) {
              report.reportError("source region \"" + fromId //$NON-NLS-1$
                  + "\" is static --- cannot aggregate static regions", //$NON-NLS-1$
                  aggPromise);
              result = false;
            }

            /*
             * If the annotated field is static, then all the dest regions MUST
             * be static.
             */
            if (JavaNode.getModifier(fieldDecl, JavaNode.STATIC)) {
              if (destIsAbstract
                  && !JavaNode.getModifier(toDecl, JavaNode.STATIC)) {
                report
                    .reportError(
                        "destination region \"" //$NON-NLS-1$
                            + toId
                            + "\" is not static --- destintation region must be static when the field is static", //$NON-NLS-1$
                        aggPromise);
                result = false;
              }
            }

            /*
             * What about visibility? Should only be able to reference regions
             * in the delegate class that are VISIBLE to the current class. Not
             * sure we have simple mechanisms for verifying this---e.g., can we
             * handle visibility issues w.r.t. inner classes?
             */

            /*
             * >> MapOk << --- make sure the region hierarchy is respected (1)
             * All regions of delegate object are mapped (2) Related regions
             * must be mapped to related regions (3) Region cannot be mapped
             * more than once (except by (2))
             * 
             * TODO: Implement this!
             */
          }
        }
      }
      return result;
    }
  }

  private static class MapRecord {

    public boolean mapped = false;

    public IRNode dest = null;
  }

  class QualifiedRegionName_BindRule extends AbstractPromiseBindRule {

    public QualifiedRegionName_BindRule() {
      super(QualifiedRegionName.prototype);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseBindRule#bind(edu.cmu.cs.fluid.tree.Operator,
     *      edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    protected IRNode bind(Operator op, IRNode n) {
      IRNode typeE = QualifiedRegionName.getType(n);
      IRNode type = findNamedType(binder.getTypeEnvironment(), n, NamedType
          .getType(typeE));

      return findRegion(type, QualifiedRegionName.getId(n));
    }
  }

  class RegionName_BindRule extends AbstractPromiseBindRule {

    public RegionName_BindRule() {
      super(RegionName.prototype);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseBindRule#bind(edu.cmu.cs.fluid.tree.Operator,
     *      edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    protected IRNode bind(Operator op, IRNode n) {
      // within a class
      final IRNode parent = tree.getParentOrNull(n);
      final Operator pop = (parent != null) ? tree.getOperator(parent) : null;
      final IRNode gparent = tree.getParentOrNull(parent);
      final Operator gpop = (gparent != null) ? tree.getOperator(gparent)
          : null;

      IRNode type = null;
      if (parent == null) {
        // @inRegion
        type = VisitUtil.getEnclosingType(n);
      } else if (pop instanceof EffectSpecification) {
        final IRNode context = EffectSpecification.getContext(parent);
        final Operator cop = tree.getOperator(context);
        if (cop instanceof VariableUseExpression) {
          String id = VariableUseExpression.getId(context);
          IRNode pd = BindUtil.findLV(getPromisedFor(parent), id);
          if (pd != null) {
            type = getIRType(pd);
          } else {
            LOG.severe("Couldn't find variable " + id); //$NON-NLS-1$
            return null;
          }
        } else if (cop instanceof ThisExpression) {
          type = VisitUtil.getEnclosingType(n);
        } else if (cop instanceof AnyInstanceExpression) {
          IRNode typeN = AnyInstanceExpression.getType(context);
          type = findNamedType(binder.getTypeEnvironment(), n, NamedType
              .getType(typeN));
        } else if (cop instanceof TypeExpression) {
          IRNode typeN = TypeExpression.getType(context);
          type = findNamedType(binder.getTypeEnvironment(), n, NamedType
              .getType(typeN));
        } else if (cop instanceof QualifiedThisExpression) {
          IRNode typeN = QualifiedThisExpression.getType(context);
          type = findNamedType(binder.getTypeEnvironment(), n, NamedType
              .getType(typeN));
        } else {
          LOG.severe("Unexpected context expr: " + cop); //$NON-NLS-1$
          return null;
        }
      } else if (pop instanceof RegionMapping) {
        final IRNode from = RegionMapping.getFrom(parent);
        if (n.equals(from)) {
          final IRNode field = VisitUtil.getEnclosingClassBodyDecl(n);
          if (field != null) {
            // type = binder.getBinding(FieldDeclaration.getType(field));
            final IRNode vars = FieldDeclaration.getVars(field);
            final IRNode vd = VariableDeclarators.getVar(vars, 0);
            type = getIRType(vd);
          } else { // Assume it's on a type
            type = VisitUtil.getEnclosingType(parent);
          }
        } else {
          type = VisitUtil.getEnclosingType(n);
        }
      } else if (pop instanceof NewRegionDeclaration
          || pop instanceof FieldMappings) {
        type = VisitUtil.getEnclosingType(n);
      } else if (RegionSpecifications.prototype.includes(pop) &&
                 (FieldMappings.prototype.includes(gpop) ||
                  ColorizedRegion.prototype.includes(gpop) || 
                  ColorConstrainedRegions.prototype.includes(gpop))) {
        type = VisitUtil.getEnclosingType(n);
      } else {
        LOG.severe("Unexpected parent expr: " + pop); //$NON-NLS-1$
        return null;
      }

      if (type == null) {
        LOG
            .severe("No context type available for " + DebugUnparser.toString(parent)); //$NON-NLS-1$
        return null;
      }
      return findRegion(type, RegionName.getId(n));
    }
  }

  private IRNode getIRType(IRNode e) {
    IJavaType jt = binder.getJavaType(e);
    if (jt instanceof IJavaDeclaredType) {
      IJavaDeclaredType jdt = (IJavaDeclaredType) jt;
      return jdt.getDeclaration();
    } else if (jt instanceof IJavaArrayType) {
      //return IOldTypeEnvironment.arrayType;
    	throw new UnsupportedOperationException();
    }
    return null;
  }
}