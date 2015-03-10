/*
 * Created on Oct 6, 2004
 *
 */
package edu.cmu.cs.fluid.mvc.database;

// import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.set.*;


/**
 * A IR-based realization of a database table
 * Typically a View of a DatabaseModel
 * 
 * Each row from a table / query is represented by a node.
 * Each column is represented by an attribute
 * 
 * This model needs to note 
 * -- what attributes there are
 * -- what labels to use for each (?)
 * 
 * -- anything else?
 * 
 * @author chance
 */
public interface TableModel extends SetModel {
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * Model attribute containing the name of the table in the database. 
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRStringType}, and is
   * immutable.
   */
  public static final String TABLE_NAME = "TableModel.TABLE_NAME";
  
  /**
   * Model attribute containing the names of the attributes from the table. 
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRSequenceType} (of Strings), and is
   * immutable.
   */
  public static final String COLUMN_NAMES = "TableModel.COLUMN_NAMES";
}
