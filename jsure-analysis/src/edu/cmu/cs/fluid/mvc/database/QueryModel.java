/*
 * Created on Oct 6, 2004
 *
 */
package edu.cmu.cs.fluid.mvc.database;

import java.sql.ResultSet;

import edu.cmu.cs.fluid.mvc.*;


/**
 * Model representing a query of some sort.
 * 
 * <P>An implementation must support the model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link #QUERY_STRING}
 * <li>{@link #QUERY_RESULT}
 * </ul>
 * 
 * @author chance
 */
public interface QueryModel extends Model {
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * Model attribute containing the current query for the model. The
   * value's type is {@link edu.cmu.cs.fluid.ir.IRStringType}, and is
   * immutable.
   */
  public static final String QUERY_STRING = "QueryModel.QUERY";

  /**
   * Model attribute containing the result of the current query. The value's type is
   * {@link edu.cmu.cs.fluid.ir.IRObjectType} (actually ResultSet), and is immutable.
   */
  public static final String QUERY_RESULT = "QueryModel.RESULT";
  
  
  /**
   * Convenience method for getting the {@link #QUERY_STRING} attribute value. 
   */
  public String getQuery();
  
  /**
   * Convenience method for setting the {@link #QUERY_STRING} attribute value. 
   */
  public String setQuery(String query);

  /**
   * Convenience method for getting the {@link #QUERY_RESULT} attribute value. 
   */
  public ResultSet getQueryResult();
}
