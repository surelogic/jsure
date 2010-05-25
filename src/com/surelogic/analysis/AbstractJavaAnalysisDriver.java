package com.surelogic.analysis;

import java.util.LinkedList;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;

/**
 * Visitor that ensures that a new "query" record is created whenever a 
 * method/constructor/instance initializer is visited.  In particular, it makes
 * sure to remember the previous query when entering an anonymous class 
 * instance initializer, to get the "query" of an anonymous class instance
 * initializer as a sub query of the existing query, and to generate queries
 * for instance initializers when "super(...)" calls are found.
 *
 * <p>In its most basic form, a query is meant to be a {@link AnalysisQuery} object,
 * although there is nothing in this class that cares about the exact nature of 
 * the query.  This means that if an assurance uses several different analysis,
 * the query can be a record of many {@link AnalysisQuery} objects.
 */
public abstract class AbstractJavaAnalysisDriver<Q> extends JavaSemanticsVisitor {
  /**
   * The current query record.  Keep this private to that subclasses cannot change it.
   * use {@link #currentQuery()} to get the value.  
   */
  private Q currentQuery = null;
  private final LinkedList<Q> oldQueries = new LinkedList<Q>();

  
  
  protected AbstractJavaAnalysisDriver() {
    super(true);
  }
  
  
  
  protected final Q currentQuery() {
    return currentQuery;
  }
  
  private void pushQuery(final Q q) {
    oldQueries.addFirst(currentQuery);
    currentQuery = q;
  }
  
  private void popQuery() {
    currentQuery = oldQueries.removeFirst();
  }

  
  
  /**
   * Return a new query suitable for code contained in the given
   * method/constructor/static initializer declaration.
   * 
   * @param decl
   *          A MethodDeclaration, ConstructorDeclaration, or ClassInitializer
   *          node. If a ClassInitializer node, it is for a <code>static</code>
   *          initializer block.
   * @return A new query record.
   */
  protected abstract Q createNewQuery(IRNode decl);

  /**
   * Return a new query suitable for code contained in the instance initializer
   * block associated with the given caller. If the caller is a ConstructorCall,
   * then the current query is for a ConstructorDeclaration, the caller is the
   * "super(...)" call at the start of that constructor, and the new query
   * should be for the instance initializer associated with the containing
   * class. If the caller is an AnonymousClassExpression then the current query
   * is for a method/constructor/initializer, and the new query should be for
   * the instance initializer of the anonymous class expression.
   * 
   * @param caller
   *          A ConstructorCall or AnonClassExpression node.
   * @return A new query record as described above.
   */
  protected abstract Q createSubQuery(IRNode caller);

  
  
  /**
   * Ensure that we create a new query when we enter a new
   * method/constructor/initializer declaration. Calls
   * {@link #enteringEnclosingDeclPrefix} before creating the query and
   * {@link #enteringEnclosingDeclPostfix} after creating the query.
   */
  @Override
  protected final void enteringEnclosingDecl(
      final IRNode newDecl, final IRNode anonClassDecl) {
    enteringEnclosingDeclPrefix(newDecl, anonClassDecl);
    final Q query;
    if (anonClassDecl == null) {
      query = createNewQuery(newDecl);
    } else {
      query = createSubQuery(anonClassDecl);
    }
    pushQuery(query);
    enteringEnclosingDeclPostfix(newDecl, anonClassDecl);
  }
  
  /**
   * Really only intended to update labels and status messages before the 
   * new query is created.
   */
  protected void enteringEnclosingDeclPrefix(
      final IRNode newDecl, final IRNode anonClassDecl) {
    // do nothing
  }

  /**
   * Not sure yet why you would want this, but being comprehensive.
   */
  protected void enteringEnclosingDeclPostfix(
      final IRNode newDecl, final IRNode anonClassDecl) {
    // do nothing
  }
  
  /**
   * Ensure that we restore the previous query when we leave a
   * method/constructor/initializer declaration. Calls
   * {@link #leavingEnclosingDeclPrefix} before restoring the query and
   * {@link #leavingEnclosingDeclPostfix} after restoring the query.
   */
  @Override
  protected final void leavingEnclosingDecl(final IRNode oldDecl) {
    leavingEnclosingDeclPrefix(oldDecl);
    popQuery();
    leavingEnclosingDeclPostfix(oldDecl);
  }

  /**
   * Not sure yet why you would want this, but being comprehensive.
   */
  protected final void leavingEnclosingDeclPrefix(final IRNode oldDecl) {
    // do nothing
  }

  /**
   * Really only intended to update labels and status messages.
   */
  protected final void leavingEnclosingDeclPostfix(final IRNode oldDecl) {
    // do nothing
  }
  
  
  
  /**
   * Overridden to ensure that we create queries for instance initializers 
   * when we encounter a constructor call, i.e., "super(...)".  Delegates
   * to {@link #getConstructorCallInitAction2(IRNode)} and wraps the returned
   * action with an action that call pushes the new query before calling {@link InstanceInitAction#tryBefore},
   * calls {@link InstanceInitAction#finallyAfter} before popping the new query,
   * and delegates directly to {@link InstanceInitAction#afterVisit()}.  
   */
  @Override
  protected final InstanceInitAction getConstructorCallInitAction(final IRNode ccall) {
    final InstanceInitAction a = getConstructorCallInitAction2(ccall);
    return new InstanceInitAction() {
      public void tryBefore() {
        final Q subAnalysisQuery = createSubQuery(ccall);
        pushQuery(subAnalysisQuery);
        a.tryBefore();
      }
      
      public void finallyAfter() {
        a.finallyAfter();
        popQuery();
      }
      
      public void afterVisit() {
        a.afterVisit();
      }
    };
  }

  /**
   * The real method to use if you need to have specials actions around
   * constructor calls.  The default implementation returns
   * {@link InstanceInitAction#NULL_ACTION}.
   * @see AbstractJavaAnalysisDriver#getConstructorCallInitAction(IRNode)
   * @see JavaSemanticsVisitor#getConstructorCallInitAction(IRNode)
   */
  protected InstanceInitAction getConstructorCallInitAction2(final IRNode ccall) {
    return InstanceInitAction.NULL_ACTION;
  }
}
