/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/PartialTemplate.java,v 1.10 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.template;

import java.beans.PropertyChangeListener;

import edu.cmu.cs.fluid.version.VersionTracker;

/**
 * An implementation of Template that is supposed to make
 * creating Templates easier.  I don't know if this is true or not
 * but this seems like a good idea at the moment, and I'll have
 * to see what happens.
 */
@Deprecated
@SuppressWarnings("all")
public abstract class PartialTemplate extends Template
{
  /**
   * The fields of the template.  The template constructor
   * should initialize this as appropriate.
   */
  protected Field[] fields;

  /**
   * Create a new Template with the given fields.
   * @param n The template's name.
   * @param vc The version cursor to use.
   */
  public PartialTemplate( final String n, final VersionTracker vc )
  {
    super( n, vc );
    fields = null;
  }

  /**
   * Partial implementation.  Handles checking if the template has already
   * executed, or is not yet ready to run.  Handles sending start and
   * completed template events.  Execution is delegated to the
   * method {@link #runImpl}.
   * @exception UnsupportedOperationException Thrown if 
   * {@link #readyToRun()} is <code>false</code>, or if 
   * {@link #getCompleted()} is <code>true</code>.
   * @see Template#run
   */
  @Override
  public final void run()
  {
    if( !getCompleted() ) {
      if( readyToRun() ) {
        fireTemplateStartEvent( new TemplateEvent.TemplateStartEvent( this ) );
        final TemplateEvent result = runImpl();
        fireTemplateDoneEvent( result );
      } else {
        throw new UnsupportedOperationException( "Template \"" + getName() + "\" is not yet ready to run!" );
      }
    } else {
      throw new UnsupportedOperationException( "Template \"" + getName() + "\" has already completed successfully!" );
    }
  }

  /**
   * Subclasses of <code>PartialTemplate</code> should override this
   * method instead of {@link #run()} because <code>run()</code> is
   * overridden to force <code>UnsupportedOperationException</code>s
   * to be thrown if <code>readyToRun()</code> is <code>false</code>.
   * @return The event that should be send that indicates the 
   * completion status of the template.
   */
  protected abstract TemplateEvent runImpl();

  @Override
  public Field[] getFields()
  {
    return fields;
  }

  @Override
  public void addFieldListener( PropertyChangeListener l )
  {
    for( int i = 0; i < fields.length; i++ )
    {
      fields[i].addValueListener( l );
      fields[i].addEmptyListener( l );
    }
  }

  @Override
  public void removeFieldListener( PropertyChangeListener l )
  {
    for( int i = 0; i < fields.length; i++ )
    {
      fields[i].removeValueListener( l );
      fields[i].removeEmptyListener( l );
    }
  }
}
