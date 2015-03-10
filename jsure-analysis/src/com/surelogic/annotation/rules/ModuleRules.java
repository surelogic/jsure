/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ModuleRules.java,v 1.2 2007/10/28 18:17:07 dfsuther Exp $*/
package com.surelogic.annotation.rules;

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
}
