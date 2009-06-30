/*
 * Created on Oct 6, 2004
 *
 */
package edu.cmu.cs.fluid.mvc.database;

import edu.cmu.cs.fluid.mvc.*;


/**
 * <p>The model represents a table in a (remote) database, while
 * its nodes represent columns in the table
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link #TABLE_NAME}
 * <li>{@link #TABLE_SIZE}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * <li>{@link #COLUMN_NAME}
 * <LI>{@link #COLUMN_TYPE}
 * </ul>

 * @author chance
 */
public interface DatabaseModel extends Model {
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * Model attribute containing the name of the table in the database. 
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRStringType}, and is
   * immutable.
   */
  public static final String TABLE_NAME = "DatabaseModel.TABLE_NAME";
  
  /**
   * Model attribute containing the number of records in the database. 
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRIntegerType}, and is
   * immutable.  The value of the attribute will change, however,
   * as the model's size changes (?).
   */
  public static final String TABLE_SIZE = "DatabaseModel.TABLE_SIZE";
  
  //===========================================================
  //== Names of standard node attributes
  //===========================================================  
 
  /**
   * Node attribute containing the name of the column. 
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRStringType}, and is
   * immutable.
   */
  public static final String COLUMN_NAME = "DatabaseModel.COLUMN_NAME";

  /**
   * Node attribute containing the type of the columns (from {@link java.sql.Types}). 
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRIntegerType}, and is
   * immutable.
   */
  public static final String COLUMN_TYPE = "DatabaseModel.TYPE";
}
