/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/SimpleTemplate.java,v 1.15 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.template;

import edu.cmu.cs.fluid.java.operator.FloatLiteral;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.template.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.VersionTracker;

@Deprecated
@SuppressWarnings("all")
public class SimpleTemplate
extends PartialTemplate
implements FieldConsultant
{
  private static final Operator[] ops3 = { IntLiteral.prototype, FloatLiteral.prototype };

  private final Field string1;
  private final Field string2;
  private final Field bool;
  private final Field integer;
  private final Field stringVector;
  private final Field intOrFloat;

  public SimpleTemplate( final String name, final VersionTracker vt )
  {
    super( name, vt );
    
    string1 = new StringField( "String1", this );
    string2 = new StringField( "String2", this, this );
    bool = new BooleanField( "Boolean", this );
    integer = new IntegerField( "Integer", this );
    stringVector = new StringVectorField( "String Vector", this, this );
    intOrFloat = new JavaNodeField( "int/float", this, ops3 );

    fields = new Field[] { string1, string2, bool, integer, stringVector, intOrFloat };
  }

  public boolean isObjectAcceptable( final Field f, final int pos, final Object o )
  {
    final String s = (String)o;
    return ((s.length() & 0x1) == 0);
  }

  public boolean isObjectAcceptable( final Field f, final Object[] o )
  {
    for( int i = 0; i < o.length; i++ )
      if( (((String)o[i]).length() & 0x1) != 0 )
        return false;
    return true;
  }

  @Override
  public boolean readyToRun()
  {
    boolean ready = true;
    for( int i = 0; i < fields.length && ready; i++ )
      if( fields[i].isEmpty() ) ready = false;
    return ready;
  }

  @Override
  protected TemplateEvent runImpl() {
    return new TemplateEvent.TemplateDoneEvent( this, true, "Completed Successfully." );
  }
}