package edu.cmu.cs.fluid.java.template;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.Expression;
import edu.cmu.cs.fluid.template.*;
import edu.cmu.cs.fluid.template.TemplateEvent;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionTracker;

@Deprecated
@SuppressWarnings("all")
public class TupleTemplate
extends PartialTemplate
{
  public static final Operator[] exprs = { Expression.prototype };

  private StringVectorField names1, names2;
  private JavaNodeVectorField exprs1, exprs2;

  public Field[] useFields, remFields;

  public TupleTemplate( final VersionTracker vt )
  {
    super( "Tuple Template", vt );
    names1 = new StringVectorField( "Names", this );
    names2 = new StringVectorField( "Remember Names", this );
    exprs1 = new JavaNodeVectorField( "Exprs", this, null, exprs );
    exprs2 = new JavaNodeVectorField( "Remember Exprs", this, null, exprs );

    fields = new Field[] { names1, exprs1, names2, exprs2 };
    useFields = new Field[] { names1, exprs1 };
    remFields = new Field[] { names2, exprs2 };
  }

  @Override
  public boolean readyToRun()
  {
    return true;
  }
    
  @Override
  public TemplateEvent runImpl()
  {
    Version.saveVersion();
    try
    {
      Version.setVersion( getVersionTracker().getVersion() );

      System.out.println( "use" );
      for( int i = 0; i < names1.getSize(); i++ )
        System.out.println( names1.getValue( i ) + " with " +
                            DebugUnparser.toString( (IRNode)exprs1.getValue( i ) ) );

      System.out.println( "\nremember" );
      for( int i = 0; i < names2.getSize(); i++ )
        System.out.println( names1.getValue( i ) + " with " +
                            DebugUnparser.toString( (IRNode)exprs2.getValue( i ) ) );

      return new TemplateEvent.TemplateDoneEvent( this, true, "Completed Successfully." );
    }
    finally
    {
      Version.restoreVersion();
    }
  }
}
