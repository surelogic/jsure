package edu.cmu.cs.fluid.promise.parse;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * This calls the appropriate method in PromiseParser
 */
public abstract class AbstractPromiseParserRule extends AbstractParseRule {
  /**
   * A flag indicating whether the rule can return multiple IRNode results, 
   * each of which requires processing
   */
  final boolean multipleResultsPossible;
  
  public AbstractPromiseParserRule(String name, boolean multi, Operator[] ops) {
    super(name, ops);
    multipleResultsPossible = multi;
    checkIfParseable(this.name());
  }
	public AbstractPromiseParserRule(String name, boolean multi, Operator op) {
		super(name, op);
    multipleResultsPossible = multi;
		checkIfParseable(name);
	}
	
  private void checkIfParseable(String name) {
		if (!PromiseParser.isParseable(name)) {
			LOG.log(
				Level.SEVERE,
				"@"+name+" requires Start_"+name+" to be implemented in PromiseX.jjt (aka PromiseParser)",
				new Throwable("For stack trace"));
		}
  } 
  
  /**
	 * @see edu.cmu.cs.fluid.eclipse.promise.IPromiseParseRule#parse(IRNode,
	 *      String)
	 */
  public boolean parse(IRNode n, String contents, IPromiseParsedCallback cb) {
    if (contents == null) {
      cb.noteProblem("contents for @" + name + " is null");
      return false;
    }
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Parsing @" + name + " - '" + contents + "'");
    }
		IRNode promisedFor = getPromisedFor(n, contents, cb);    
    if (promisedFor == null) {
      return false;
    }
    IRNode result = null;
    try {
      result = PromiseParser.parsePromise(name, contents);
    } catch (ParseException e) {
      cb.noteProblem("Failed to parse for @" + name + " - " + contents + " with exception: "+e.getMessage());
			LOG.log(Level.WARNING, "Failed to parse for @" + name + " - " + contents, e); 
      return false;
    } catch (IOException e) {
      cb.noteProblem("Failed to parse for @" + name + " - " + contents + " with exception: "+e.getMessage());
			LOG.log(Level.WARNING, "Failed to parse for @" + name + " - " + contents, e);       
      return false;
    }
    if (result != null) {
      if (filterParsedResult(promisedFor, result)) {
        final String msg = "Filtered out promise referencing elided code: @" + name + " " + contents;
        cb.noteWarning(msg);    
        LOG.log(Level.WARNING, msg);       
        return false;
      }
      if (multipleResultsPossible) {
        final Collection<IRNode> results = new ArrayList<IRNode>();
        boolean rv = processResult(promisedFor, result, cb, results);
        if (rv) {
          if (results.isEmpty()) {      
            cb.parsed(result);     
          } else {
            for (IRNode rn : results) {
              cb.parsed(rn);
            }
          }
        }
        return rv;
      } else {
        // Only one result possible
        boolean rv = processResult(promisedFor, result, cb);
        if (rv) {   
          cb.parsed(result);     
        }
        return rv;
      }
    } 
    cb.noteProblem("Null result for @"+name+" "+contents);
    return false;
  }

  protected boolean filterParsedResult(IRNode promisedFor, IRNode result) {
    //IBinder b = IDE.getInstance().getTypeEnv().getBinder();
    //JavaPromise.attachPromiseNode(promisedFor, result, false);
    
    //try {
      for (IRNode n : tree.bottomUp(result)) {
        if (checkIfElided(promisedFor, n)) {
          //JavaPromise.detachPromiseNode(promisedFor, result);
          return true;
        }
      }
    //} catch (SlotUndefinedException e) {
      // ignore
    //}
    //JavaPromise.detachPromiseNode(promisedFor, result);
    return false;    
  }
  
  // HACK
  protected boolean checkIfElided(IRNode promisedFor, IRNode n) {
    Operator op = tree.getOperator(n);
    if (FieldRef.prototype.includes(op)) {
      // Check if this
      IRNode base = FieldRef.getObject(n);
      if (!ThisExpression.prototype.includes(base)) {
        return false;
      }
      // Check if elided
      IRNode cu  = VisitUtil.getEnclosingCompilationUnit(promisedFor);
      CUDrop cud = CUDrop.queryCU(cu);
      if (cud.wasElided(name)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * @param field
   * @return The first VariableDeclarator in the FieldDeclaration
   */
  protected static IRNode getFirstVarForField(IRNode field) {
    return VariableDeclarators.getVar(FieldDeclaration.getVars(field), 0);
  }

  /**
   * @param field
   * @return An enumeration of the VariableDeclarators for the FieldDeclaration
   */
  protected static Iterator<IRNode> getVarsForField(IRNode field) {
    return VariableDeclarators.getVarIterator(
      FieldDeclaration.getVars(field));
  }

  /**
   * Takes the result and annotates n (in the appropriate way).
   * 
	 * Designed to be overridden.
	 * If returning false, it should call cb.noteProblem.
   *
	 * @param n
	 *          The node being annotated
	 * @param result
	 *          The promise that was just parsed
	 */
  protected boolean processResult(final IRNode n, final IRNode result, IPromiseParsedCallback cb, Collection<IRNode> results)
  {        
    return processResult(n, result, cb);
  }
  
  protected boolean processResult(final IRNode n, final IRNode result,
                                  IPromiseParsedCallback cb) {
    final Operator op = tree.getOperator(result);

    cb.noteProblem("Did nothing with @" + name + ", op = " + op + ": " + DebugUnparser.toString(result));
    return false;
  }
}
