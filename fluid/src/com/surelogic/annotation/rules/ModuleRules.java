/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ModuleRules.java,v 1.2 2007/10/28 18:17:07 dfsuther Exp $*/
package com.surelogic.annotation.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.ExportNode;
import com.surelogic.aast.promise.ModuleChoiceNode;
import com.surelogic.aast.promise.NoVisClauseNode;
import com.surelogic.aast.promise.VisClauseNode;
import com.surelogic.annotation.DefaultSLThreadRoleAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.SimpleBooleanAnnotationParseRule;
import com.surelogic.annotation.parse.SLThreadRoleAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.modules.ExportDrop;
import com.surelogic.dropsea.ir.drops.modules.ModulePromiseDrop;
import com.surelogic.dropsea.ir.drops.modules.NoVisPromiseDrop;
import com.surelogic.dropsea.ir.drops.modules.VisDrop;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ICodeFile;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.tree.Operator;

public class ModuleRules extends AnnotationRules {

  public static final String MODULE = "Module";
  public static final String MODULEWRAPPER = "Module";
  public static final String VIS = "Vis";
  public static final String NOVIS = "NoVis";
  public static final String EXPORT = "Export";
  public static final String EXPORTTO = "Export";
  public static final String BLOCKIMPORT = "BlockImport";
  
  private static final AnnotationRules instance = new ModuleRules();
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  /*
  private static final NoVis_ParseRule noVisRule = new NoVis_ParseRule();
  private static final Vis_ParseRule visRule = new Vis_ParseRule();
  private static final Module_ParseRule moduleRule = new Module_ParseRule();
  private static final Export_ParseRule exportRule = new Export_ParseRule();
  */

  public static class NoVis_ParseRule 
  extends SimpleBooleanAnnotationParseRule<NoVisClauseNode,NoVisPromiseDrop> {
    public NoVis_ParseRule() {
      super(NOVIS, declOps, NoVisClauseNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new NoVisClauseNode(offset);
    }
    @Override
    protected IPromiseDropStorage<NoVisPromiseDrop> makeStorage() {
    	return SinglePromiseDropStorage.create(name(), NoVisPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<NoVisClauseNode, NoVisPromiseDrop>(this, ScrubberType.UNORDERED) {
        @Override
        protected PromiseDrop<NoVisClauseNode> makePromiseDrop(NoVisClauseNode a) {
          NoVisPromiseDrop d = NoVisPromiseDrop.buildNoVisPromiseDrop(a);
          return storeDropIfNotNull(a, d);          
        }
      };
    }    
  } 
  
  public static class Vis_ParseRule extends DefaultSLThreadRoleAnnotationParseRule<VisClauseNode, VisDrop> {
    public Vis_ParseRule() {
      super(VIS, declOps, VisClauseNode.class);
    }
 
    
    @Override
    protected Object parseTRoleAnno(IAnnotationParsingContext context,
                           SLThreadRoleAnnotationsParser parser) throws Exception,
        RecognitionException {
      return parser.vis().getTree();
    }


    @Override
    protected IPromiseDropStorage<VisDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), VisDrop.class);
    }
    
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<VisClauseNode, VisDrop>(this, ScrubberType.UNORDERED) {
        @Override
        protected PromiseDrop<VisClauseNode> makePromiseDrop(VisClauseNode a) {
          VisDrop d = VisDrop.buildVisDrop(a);
          return storeDropIfNotNull(a, d);          
        }
      };
    }    
    
  }
  
  public static class Module_ParseRule extends DefaultSLThreadRoleAnnotationParseRule<ModuleChoiceNode, ModulePromiseDrop> {
    public Module_ParseRule() {
      super(MODULE, packageTypeDeclOps, ModuleChoiceNode.class);
    }
 
    
    @Override
    protected Object parseTRoleAnno(IAnnotationParsingContext context,
                           SLThreadRoleAnnotationsParser parser) throws Exception,
        RecognitionException {
      return parser.module().getTree();
    }


    @Override
    protected IPromiseDropStorage<ModulePromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), ModulePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<ModuleChoiceNode, ModulePromiseDrop>(this, ScrubberType.UNORDERED) {
        @Override
        protected PromiseDrop<ModuleChoiceNode> makePromiseDrop(ModuleChoiceNode a) {
          ModulePromiseDrop d = ModulePromiseDrop.buildModulePromiseDrop(a);
          return storeDropIfNotNull(a, d);          
        }
      };
    }    
    
  }
  
  public static class Export_ParseRule extends DefaultSLThreadRoleAnnotationParseRule<ExportNode, ExportDrop> {
    public Export_ParseRule() {
      super(EXPORT, declOps, ExportNode.class);
    }
 
    
    @Override
    protected Object parseTRoleAnno(IAnnotationParsingContext context,
                           SLThreadRoleAnnotationsParser parser) throws Exception,
        RecognitionException {
      return parser.module().getTree();
    }


    @Override
    protected IPromiseDropStorage<ExportDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), ExportDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<ExportNode, ExportDrop>(this, ScrubberType.UNORDERED) {
        @Override
        protected PromiseDrop<ExportNode> makePromiseDrop(ExportNode a) {
          ExportDrop d = ExportDrop.buildExportDrop(a);
          return storeDropIfNotNull(a, d);          
        }
      };
    }    
    
  }
  @Override
  public void register(PromiseFramework fw) {
	/*
    registerParseRuleStorage(fw, noVisRule);
    registerParseRuleStorage(fw, visRule);
    registerParseRuleStorage(fw, moduleRule);
    registerParseRuleStorage(fw, exportRule);
    */
  }
  
  public static ModulePromiseDrop getModule(IRNode where) {
	  //return getDrop(moduleRule.getStorage(), where); 
	  return null;
  }
  /**
   * @return The representation of the module name, or
   *         null if there is no module declaration 
   */
  public static ModulePromiseDrop getModuleDecl(IRNode here) {
    IRNode cu = VisitUtil.getEnclosingCompilationUnit(here);
    if (cu == null) {
      Operator op = tree.getOperator(here);
      if (CompilationUnit.prototype.includes(op)) {
        cu = here;
      } else {
        LOG.severe("Didn't find enclosing CU for the Module Decl: "+DebugUnparser.toString(here));
      }
    }
    IRNode pd = CompilationUnit.getPkg(cu); 
    return getModule(pd);
//    IRNode mayHaveModuleDecl = VisitUtil.computeOutermostEnclosingTypeOrCU(here);
//    return getModule(mayHaveModuleDecl);
  }
  
  private static boolean defaultAsSource = true;
  private static boolean defaultAsNeeded = false;
  
  private static List<ModulePattern> yes_AsNeeded = new ArrayList<ModulePattern>();
  private static List<ModulePattern> no_AsNeeded  = new ArrayList<ModulePattern>();
  private static List<ModulePattern> yes_AsSource = new ArrayList<ModulePattern>();
  private static List<ModulePattern> no_AsSource  = new ArrayList<ModulePattern>();
  
  private static Map<String,ModulePattern> patternCache = new HashMap<String,ModulePattern>();
  
  private static ModulePattern findPattern(String pattern) {
    ModulePattern p = patternCache.get(pattern);
    if (p == null) {
      if (pattern.indexOf('*') < 0) {
        p = new NoWildcards(pattern);
      } else {
        p = new Wildcards(pattern);
      }
      patternCache.put(pattern, p);
    }
    return p;
  }
  
  public static void clearSettings() {
	  defaultAsSource = true;
	  defaultAsNeeded = false;
	  yes_AsNeeded.clear();
	  no_AsNeeded.clear();
	  yes_AsSource.clear();
	  no_AsSource.clear();
  }
  
  public static void defaultAsSource(boolean b) {
	  LOG.fine(b ? "Defaulting to load as source" : "Defaulting to load as class");
	  defaultAsSource = b;
  }

  public static void defaultAsNeeded(boolean b) {
	  if (b) {
		  LOG.fine("Defaulting to load as needed");
	  }
	  defaultAsNeeded = b;
  } 

  public static boolean getDefaultAsSource() {
	  return defaultAsSource;
  }

  public static boolean getDefaultAsNeeded() {
	  return defaultAsNeeded;
  }

  public static void setAsNeeded(String modulePattern, boolean b) {
	  final String msg = b ? "Loading as needed: " : "Loading required: ";
	  LOG.fine(msg+modulePattern);
	  List<ModulePattern> l = b ? yes_AsNeeded : no_AsNeeded; 
	  ModulePattern p       = findPattern(modulePattern);
	  l.add(p);
  }

  public static void setAsSource(String modulePattern, boolean b) {
	  final String msg = b ? "Loading as source: " : "Loading as class: ";
	  LOG.fine(msg+modulePattern);
	  List<ModulePattern> l = b ? yes_AsSource : no_AsSource; 
	  ModulePattern p       = findPattern(modulePattern);
	  l.add(p);
  }

  private static boolean processPatterns(final String mod, List<ModulePattern> yes, List<ModulePattern> no, boolean defaultVal) {
	  for (ModulePattern p : yes) {
		  if (p.match(mod)) {
			  return true;
		  }
	  }
	  for (ModulePattern p : no) {
		  if (p.match(mod)) {
			  return false;
		  }
	  }
	  return defaultVal;
  }

  public static boolean loadedAsNeeded(String module) {
	  boolean rv = processPatterns(module, yes_AsNeeded, no_AsNeeded, defaultAsNeeded);
	  //System.out.println(module+" as needed? "+rv);
	  return rv;
  }
  public static boolean treatedAsSource(String module) {
	  boolean rv = processPatterns(module, yes_AsSource, no_AsSource, defaultAsSource);
	  //System.out.println(module+" as source? "+rv);
	  return rv;
  }
  
  private interface ModulePattern {    
	  boolean match(String s);
  }

  private static class NoWildcards implements ModulePattern {
	  final String match;
	  NoWildcards(String pattern) {
		  this.match = pattern;
	  }
	  @Override
    public boolean match(String s) {
		  return match.equals(s);
	  }    
  }

  private static class Wildcards implements ModulePattern {
	  final Pattern compiledPattern;
	  Wildcards(String pattern) {      
		  final String noDots    = pattern.replaceAll("\\.", "\\.");
		  final String wildcards = noDots.replaceAll("\\*", ".*");
		  compiledPattern = Pattern.compile(wildcards);
	  }
	  @Override
    public boolean match(String s) {
		  Matcher m = compiledPattern.matcher(s);
		  return m.matches();
	  }    
  }
  
  private static final Map<CompUnitPattern, Boolean> asSourcePatterns = new HashMap<CompUnitPattern, Boolean>();
  private static final Map<CompUnitPattern, Boolean> asNeededPatterns = new HashMap<CompUnitPattern, Boolean>();
  private static final Map<CompUnitPattern, String> modulePatterns = new HashMap<CompUnitPattern, String>();

  public static void clearAsSourcePatterns() {
	  asSourcePatterns.clear();
  }

  public static void clearAsNeededPatterns() {
	  asNeededPatterns.clear();
	  modulePatterns.clear();
  }

  public static void setAsSource(CompUnitPattern pattern, boolean asSource) {
	  // System.out.println("Setting pattern "+pattern+" asSource =
	  // "+asSource);
	  asSourcePatterns.put(pattern, asSource ? Boolean.TRUE : Boolean.FALSE);
  }

  public static void setAsNeeded(CompUnitPattern pattern, boolean asSource) {
	  // System.out.println("Setting pattern "+pattern+" asSource =
	  // "+asSource);
	  asNeededPatterns.put(pattern, asSource ? Boolean.TRUE : Boolean.FALSE);
  }

  /**
   * @param f_cu
   * @return
   */
  public static boolean treatedAsSource(ICodeFile cf) {
	  //ICodeFile cf = new EclipseCodeFile(cu);
	  return matchingPattern(asSourcePatterns, ModuleRules
			  .getDefaultAsSource(), cf);
  }

  public static boolean loadedAsNeeded(ICodeFile cf) {
	  //ICodeFile cf = new EclipseCodeFile(cu);
	  return matchingPattern(asNeededPatterns, ModuleRules
			  .getDefaultAsNeeded(), cf);
  }

  private static boolean matchingPattern(Map<CompUnitPattern, ?> patterns,
		  boolean flag, ICodeFile cu) {
	  Boolean b = (Boolean) matchingPattern(patterns, cu, flag ? Boolean.TRUE
			  : Boolean.FALSE);
	  return b.booleanValue();
  }

  private static Object matchingPattern(Map<CompUnitPattern, ?> patterns,
		  ICodeFile cu, Object rv) {
	  if (cu == null) {
		  return null;
	  }
	  CompUnitPattern last = null;
	  String pkg = null;

	  for (Map.Entry<CompUnitPattern, ?> e : patterns.entrySet()) {
		  CompUnitPattern pat = e.getKey();
		  Object val = e.getValue();

		  // optimization
		  if (pkg == null) {
			  pkg = cu.getPackage(); // All assumed to be in same project
		  }

		  if (pat.matches(pkg, null)) {
			  if (last != null) {
				  // multiple matches
				  if (!rv.equals(val)) {
					  LOG.severe("Multiple CU patterns match and disagree: "
							  + last + ", " + pat);
				  } else {
					  LOG.warning("Multiple CU patterns match: " + last
							  + ", " + pat);
				  }
			  }
			  last = pat;
			  rv = val;
		  }
	  }
	  return rv;
  }

  private static Object matchingPattern(Map<CompUnitPattern, ?> patterns,
		  final String pkg, final String path, Object rv) {
	  if (pkg == null) {
		  return null;
	  }
	  CompUnitPattern last = null;

	  for (Map.Entry<CompUnitPattern, ?> e : patterns.entrySet()) {
		  CompUnitPattern pat = e.getKey();
		  Object val = e.getValue();

		  if (pat.matches(pkg, path)) {
			  if (last != null) {
				  // multiple matches
				  if (!rv.equals(val)) {
					  LOG.severe("Multiple CU patterns match and disagree: "
							  + last + ", " + pat);
				  } else {
					  LOG.warning("Multiple CU patterns match: " + last
							  + ", " + pat);
				  }
			  }
			  last = pat;
			  rv = val;
		  }
	  }
	  return rv;
  }

  /**
   * @param name
   *            The module to be created
   * @param patterns
   *            A comma-separated list of patterns
   */
  public static void createModule(String proj, String name, String patterns) {
	  StringTokenizer st = new StringTokenizer(patterns, ",");
	  while (st.hasMoreTokens()) {
		  String pat = st.nextToken().trim();
		  Object old = modulePatterns.put(CompUnitPattern.create(proj, pat),
				  name);
		  if (old != null) {
			  LOG.severe("Somehow displaced an existing module mapping for "
					  + old);
		  }
	  }
  }

  public static String getModule(ICodeFile cu) {
	  return (String) matchingPattern(modulePatterns, cu, REST_OF_THE_WORLD);
  }

  public static final String REST_OF_THE_WORLD = "Rest of the world";

  /**
   * @param pkg
   *            The fully qualified name of a package
   * @return
   */
  public static String mapToModule(String pkg, String path) {
	  return (String) matchingPattern(modulePatterns, pkg, path, REST_OF_THE_WORLD);
  }
}
