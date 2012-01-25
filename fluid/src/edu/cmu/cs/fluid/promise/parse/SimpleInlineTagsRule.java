package edu.cmu.cs.fluid.promise.parse;

import java.util.Iterator;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.SimpleRemovelessIterator;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/**
 * Scans the tag contents for inline tags and applies its own rule Mostly to
 * check for @unique, @borrowed within @param,
 * 
 * @return
 */
public abstract class SimpleInlineTagsRule extends AbstractParseRule {
  private final IParseRuleSet rules = new ParseRuleMap();
  /*
	private SimpleInlineTagsRule(String s, Operator op) {
		super(s, op);
	}
  */
  private SimpleInlineTagsRule(String s, Operator[] ops) {
    super(s, ops);
  }

  @Override
  protected String ensureCapitalizedTag(String tag) {
    return tag;
  }
  
  /**
	 * @see IPromiseParseRule#parse(IRNode, String)
	 */
  public final boolean parse(
    IRNode n,
    String contents,
    IPromiseParsedCallback cb) {
    IRNode p = null;
    try {
      p = getPromisedFor(n, contents, cb);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Got exception", e);
      cb.noteProblem("Got exception in getPromisedFor("+contents+"): "+e);
      return false;
    }
    if (p == null) {
      //LOG.warning("Couldn't find promisedFor node in: " + contents);
      //LOG.warning(DebugUnparser.toString(n));
      if (name.equals("param")) {
        LOG.warning("Non-existent parameter for @param: "+contents);        
        return false;
      }
      else if (name.equals("return")) {
        Operator op = tree.getOperator(n);
        
        if (!MethodDeclaration.prototype.includes(op)) {
          LOG.warning("@return should not appear on a "+op);
        }        
        else if (JavaNode.getModifier(n, JavaNode.STATIC)) {        
          LOG.warning("@return should not appear on static method "+DebugUnparser.toString(n));                  
        } 
        else { 
          IRNode retType = MethodDeclaration.getReturnType(n);
          Operator top   = tree.getOperator(retType);
          if (VoidType.prototype.includes(top)) {
            LOG.warning("@return should not appear on a void method "+DebugUnparser.toString(n));
          } else {
            // non-static, non-void method
            IRNode t = VisitUtil.getEnclosingType(n);
            LOG.severe("No return node for "+DebugUnparser.toString(n)+" within "+JavaNames.getQualifiedTypeName(t));
          }
        }
        return false;
      }
      cb.noteWarning("Couldn't find promisedFor ("+contents+") in "+DebugUnparser.toString(n));
      return false;
    }
    Iterator<String> it = getInlineTags(contents);

    final boolean debug = LOG.isLoggable(Level.FINE);
    boolean rv = true;
    while (it.hasNext()) {
      final String promise = it.next();
      if (!rules.useRule(p, promise, IPromiseParsedCallback.defaultPrototype)) {
        // FIX for nested promises
        // LOG.warning(" Parse failed : " + promise);
        cb.noteProblem("Failed to parse inline promise: "+promise);
        rv = false;
      } else if (debug) {
        LOG.fine("Parse success: " + promise);
      }
    }
    return rv;
  }

  public IPromiseParseRule addRule(IPromiseParseRule r) {
    return rules.addRule(r);
  }

  /**
	 * @return Returns an iterator of Strings (e.g. "unique foo" for <tt>@unique foo</tt>)
	 */
  Iterator<String> getInlineTags(String contents) {
    int start = contents.indexOf("{@");
    if (start < 0) {
      LOG.fine("No inline tags in: " + contents);
      return new EmptyIterator<String>();
    }
    return new InlineTagIterator(contents.substring(start));
  }

  private static class InlineTagIterator extends SimpleRemovelessIterator<String> {
    private String contents;

    InlineTagIterator(String contents) {
      this.contents = contents;
    }

    @Override
    protected Object computeNext() {
      final int start = contents.indexOf("{@");
      if (start < 0) {
        return noElement;
      }

      final int end = contents.indexOf('}', start + 2);
      if (end < 0) {
        return noElement;
      }

      final String promise = contents.substring(start + 2, end);
      contents = contents.substring(end);
      return promise;
    }
  }

  private static boolean setupDone = false;

  /**
	 *  
	 */
  public static synchronized void setupTagRules(IPromiseParser parser) {
    if (setupDone) {
      return;
    }

    SimpleInlineTagsRule returnRule = new SimpleInlineTagsRule("return", methodDeclOp) {
      @Override
      protected IRNode getPromisedFor_raw(IRNode n, String contents, IPromiseParsedCallback cb) {
        return JavaPromise.getReturnNodeOrNull(n);
      }
    };

    Operator[] ops = {
      ReturnValueDeclaration.prototype, ParameterDeclaration.prototype
    };
    SimpleInlineTagsRule[] rules = { returnRule, new ParamRule()};
    for (int i = 0; i < rules.length; i++) {
      IPromiseParseRule[] inlineRules = createInlineRules(ops[i]);
           
      for (int j = 0; j < inlineRules.length; j++) {
        rules[i].addRule(inlineRules[j]);
      }
      parser.addRule(rules[i]);
    }

    setupDone = true;
  }

  private static IPromiseParseRule[] createInlineRules(final Operator matchOp) {
  	final Operator[] matchOps = new Operator[] { matchOp };

  	IPromiseParseRule[] inlineRules =
    {
      new BooleanTagRule("Unique", matchOps, UniquenessAnnotation.getIsUniqueSlotInfo()),
      new BooleanTagRule("Borrowed", matchOps, UniquenessAnnotation.getIsBorrowedSlotInfo()),
      new BooleanTagRule("NotNull", matchOps, NotNullAnnotation.getIsNotNullSlotInfo()),
      new StandardTagRule("link"),
    };
    return inlineRules;
  }

  private static class ParamRule extends SimpleInlineTagsRule {
    ParamRule() {
      super("param", methodOrClassDeclOps);
    }

    @Override
    protected IRNode getPromisedFor_raw(IRNode n, String contents, IPromiseParsedCallback cb) {
      final String pname = super.rules.getFirstToken(contents);
      if (pname == null) {
        return null;
      }
      final IRNode param = BindUtil.findLV(n, pname);
      return param;
    }
  }
}
