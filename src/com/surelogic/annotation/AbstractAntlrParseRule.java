/*
 * $Header:
 * /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultSLAnnotationParseRule.java,v
 * 1.12 2007/07/09 18:40:14 chance Exp $
 */
/*
 * $Header:
 * /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultSLAnnotationParseRule.java,v
 * 1.13 2007/07/18 14:30:51 chance Exp $
 */
package com.surelogic.annotation;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractNodeAdaptor;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Abstract class for annotations parsed using an Antlr parser
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractAntlrParseRule<A extends IAASTRootNode, 
                                             D extends PromiseDrop<? super A>,
                                             P extends Parser>
	extends AbstractAnnotationParseRule<A, D> {
  protected static final Object USE_RAW_STRING = new Object();
  
	AnnotationLocation relativeLocation;

	protected AbstractAntlrParseRule(String name, Operator[] ops,
		Class<A> dt, AnnotationLocation loc) {
		super(name, ops, dt);
		relativeLocation = loc;
	}

	protected AbstractAntlrParseRule(String name, Operator op,
		Class<A> dt, AnnotationLocation loc) {
		super(name, op, dt);
		relativeLocation = loc;
	}

	protected AbstractAntlrParseRule(String name, Operator[] ops,
		Class<A> dt) {
		this(name, ops, dt, AnnotationLocation.DECL);
	}

	protected AbstractAntlrParseRule(String name, Operator op, Class<A> dt) {
		this(name, op, dt, AnnotationLocation.DECL);
	}

	/**
	 * Parses a string containing the 'meat' of the annotation into an AAST
	 * 
	 * @param context
	 *            The object used to report errors
	 * @param contents
	 *            The information defining the annotation
	 */
	public final ParseResult parse(IAnnotationParsingContext context, String contents) {
		if (!declaredOnValidOp(context.getOp())) {
			context.reportError(IAnnotationParsingContext.UNKNOWN,
				"@"+name()+" declared on invalid operator: "+context.getOp().name());
			return ParseResult.FAIL;
		}
		try {
		  Object result       = parse(context, initParser(contents));
		  if (result == ParseResult.IGNORE) {
//		    System.out.println("Ignoring: @"+name()+' '+contents);
		    return ParseResult.IGNORE;
		  }
		  AASTNode an;
		  if (result instanceof AASTNode) {
			  an = (AASTNode) result;
		  } else {
			  AbstractNodeAdaptor.Node tn =	(AbstractNodeAdaptor.Node) result;
			  if (tn == null || tn.isNil()) {
				  context.reportError(0, "Nothing parsed: " + contents);
				  return ParseResult.FAIL;
			  }
			  an = finalizeAST(context, tn);
		  }
		  A n;
		  if (an == null) {
			  return ParseResult.FAIL;  
		  }
		  else if (getAASTType().isInstance(an)) {
			  @SuppressWarnings("unchecked")
			  A temp = (A) an;
			  n = temp;
		  }
		  else {
			  n = makeRoot(an);
		  }

		  if (n != null) {
			  context.reportAAST(n.getOffset(), relativeLocation, n);
		  }
		}		
		catch (IndexOutOfBoundsException e) {
			//context.reportException(IAnnotationParsingContext.UNKNOWN, e);
			context.reportError(IAnnotationParsingContext.UNKNOWN, "Unable to create promise: @"+this.name+' '+contents);
			return ParseResult.FAIL;
		}		
		catch (RecognitionException e) {
		  handleRecognitionException(context, contents, e);
		  return ParseResult.FAIL;
		}
		catch (Exception e) {
			context.reportException(IAnnotationParsingContext.UNKNOWN, e);
			return ParseResult.FAIL;
		}
		return ParseResult.OK;
	}

	protected abstract P initParser(String contents) throws Exception;
	
  protected AASTNode finalizeAST(IAnnotationParsingContext context,
      AbstractNodeAdaptor.Node tn) {
		return tn.finalizeAST(context);
	}

	/**
	 * Calls the appropriate method on the parser
	 * 
	 * @return the created tree
	 */
	protected abstract Object parse(IAnnotationParsingContext context, P parser) 
	throws Exception, RecognitionException;

	/**
	 * Called if the parser doesn't return the type of node that we expect to
	 * report. Usually used to add a root node (e.g. an InRegionNode) on top of
	 * the existing tree (e.g. a RegionSpecificationNode)
	 */
	protected A makeRoot(AASTNode an) {
		throw new UnsupportedOperationException();
	}
}
