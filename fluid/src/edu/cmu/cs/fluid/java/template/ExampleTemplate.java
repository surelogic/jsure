/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/ExampleTemplate.java,v 1.15 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.template;

import edu.cmu.cs.fluid.java.operator.FloatLiteral;
import edu.cmu.cs.fluid.java.operator.ForStatement;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.template.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.*;

@Deprecated
@SuppressWarnings("all")
public class ExampleTemplate
extends PartialTemplate
{
  private static final Operator[] ops1 = { IntLiteral.prototype };
  private static final Operator[] ops2 = { ForStatement.prototype };
  private static final Operator[] ops3 = { IntLiteral.prototype, FloatLiteral.prototype };
  private static final Operator[][] oplist = { ops1, ops1 };

  private final Field intExp;
  private final Field forStmt;
  private final Field intOrFloat;
  private final Field string;
  private final Field stringVector;
  private final Field nodeVector;

  public ExampleTemplate( String name )
  {
    super( name, new VersionCursor( Version.getVersion() ) );

    intExp = new JavaNodeField( "Field 1 (Int)", this, ops1 );
    forStmt = new JavaNodeField( "Field 2 (For)", this, ops2 );
    intOrFloat = new JavaNodeField( "Field 3 (Int/Float)", this, ops3 );
    string = new StringField( "Field 4 (User String)", this );
    stringVector = new StringVectorField( "Field 5 (String Vector)", this );
    nodeVector = new JavaNodeVectorField( "Field 6 (i,i,(i|f)*)", this,
                                         Field.USE_TEMPLATES_TRACKER,
                                         Field.DEFAULT_CONSULTANT, 5,
                                         oplist, ops3 );
    fields = new Field[] { intExp, forStmt, intOrFloat, string, stringVector, nodeVector };
  }

  @Override
  public boolean readyToRun()
  {
    boolean ready = true;
    for( int i = 0; i < fields.length && ready; i++ )
      if( fields[i].isEmpty() ) ready = false;

    // Check to see that the string sequence has at least 3 elements
    if( ready )
    {
      final Object[] v = fields[4].getValue();
      if( v.length < 3 ) ready = false;
    }
    return ready;
  }

  @Override
  protected TemplateEvent runImpl() {
    return new TemplateEvent.TemplateDoneEvent( this, true, "Completed Successfully." );
  }
}