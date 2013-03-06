/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/SimpleRenderer.java,v 1.10 2007/07/05 18:15:16 aarong Exp $
 *
 * SimpleRenderer.java
 * Created on March 22, 2002, 9:35 AM
 */

package edu.cmu.cs.fluid.mvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Bare-bones model renderer.  Displays attributes in tables.
 * Gives feedback of the most recently received event.
 *
 * @author Aaron Greenhouse
 */
public final class SimpleRenderer
extends JPanel
implements ModelListener
{
  /**
   * The model being rendered/observed.
   */
  private final Model model;
  
  /**
   * Table model for the model attributes.
   */
  private final TableModel compAttrModel;
  
  /**
   * Table model for the node attributes.
   */
  private final TableModel nodeAttrModel;
  
  
  
  /** Creates new form SimpleRenderer */
  public SimpleRenderer( final Model mod )
  {
    model = mod;
    compAttrModel = new CompAttributesModel( model );
    nodeAttrModel = new NodeAttributesModel( model );
    
    initComponents();
    compAttrTable.setModel( compAttrModel );
    nodeAttrTable.setModel( nodeAttrModel );
    model.addModelListener( this );
  }
  
  
  
  /**
   * Update the event feedback field when the model breaks.
   */
  @Override
  public void breakView( final ModelEvent e )
  {  
    if(e.shouldCauseRebuild()) {
      SwingUtilities.invokeLater( new Runnable() {
        @Override
        public void run() { 
          eventField.setText( e.toString() );
        }
      } );
    }
  }
  
  @Override
  public void addedToModel(final Model m) {
    // do nothing
  }

  @Override
  public void removedFromModel(final Model m) {
    // do nothing
  }
  
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  private void initComponents()//GEN-BEGIN:initComponents
  {
    jPanel1 = new javax.swing.JPanel();
    jLabel1 = new javax.swing.JLabel();
    eventField = new javax.swing.JTextField();
    jPanel3 = new javax.swing.JPanel();
    jSplitPane1 = new javax.swing.JSplitPane();
    jPanel4 = new javax.swing.JPanel();
    jScrollPane2 = new javax.swing.JScrollPane();
    nodeAttrTable = new javax.swing.JTable();
    jPanel2 = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    compAttrTable = new javax.swing.JTable();

    setLayout(new java.awt.BorderLayout(0, 5));

    setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), model.getName(), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 0, 12)));
    jPanel1.setLayout(new java.awt.BorderLayout());

    jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    jLabel1.setText("Most Recent Event:");
    jPanel1.add(jLabel1, java.awt.BorderLayout.WEST);

    eventField.setEditable(false);
    eventField.setText("Event Feedback goes here");
    jPanel1.add(eventField, java.awt.BorderLayout.CENTER);

    add(jPanel1, java.awt.BorderLayout.NORTH);

    jPanel3.setLayout(new java.awt.BorderLayout());

    jPanel3.setMaximumSize(new java.awt.Dimension(250, 250));
    jSplitPane1.setDividerSize(8);
    jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
    jSplitPane1.setResizeWeight(0.3);
    jPanel4.setLayout(new java.awt.BorderLayout());

    jPanel4.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), "Node Attributes", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 0, 11)));
    nodeAttrTable.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][]
      {
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null}
      },
      new String []
      {
        "Title 1", "Title 2", "Title 3", "Title 4"
      }
    ));
    jScrollPane2.setViewportView(nodeAttrTable);

    jPanel4.add(jScrollPane2, java.awt.BorderLayout.CENTER);

    jSplitPane1.setBottomComponent(jPanel4);

    jPanel2.setLayout(new java.awt.BorderLayout());

    jPanel2.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), "Model Attributes", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 0, 11)));
    compAttrTable.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][]
      {
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null}
      },
      new String []
      {
        "Title 1", "Title 2", "Title 3", "Title 4"
      }
    ));
    jScrollPane1.setViewportView(compAttrTable);

    jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

    jSplitPane1.setTopComponent(jPanel2);

    jPanel3.add(jSplitPane1, java.awt.BorderLayout.CENTER);

    add(jPanel3, java.awt.BorderLayout.CENTER);

  }//GEN-END:initComponents
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTable nodeAttrTable;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JTable compAttrTable;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JTextField eventField;
  // End of variables declaration//GEN-END:variables
  
}



/**
 * Table model for rendering model-level attributes.  Assumes that no
 * attributes are added after the model is created.  This is, in general,
 * a false assuption, but true enough for current purposes.
 */
final class CompAttributesModel
extends AbstractTableModel
implements ModelListener
{
  /** The number of columns. */
  private static final int COLUMN_COUNT = 2;
  
  /** The index of the attribute name column */
  private static final int ATTR_NAME_COL = 0;
  
  /** The index of the attribute value column */
  private static final int ATTR_VAL_COL = 1;
  
  /** String for undefined attributes. */
  private static final String UNDEFINED = "<Undefined>";
  
  
  
  /** The column headers */
  private static final String[] colHeaders =
    new String[] { "Attribute", "Value" };
  
  /** The column types */
  private static final Class[] colClasses =
    new Class[] { String.class, String.class };
  
  
  
  /** The model whose attributes are being tabled. */
  private final Model model;
  
  /** Cache of the attribute names */
  private final String[] attrNames;
  
  /** The attribute value storage for the different attributes. */
  private final ComponentSlot[] values;

  
  
  public CompAttributesModel( final Model mod )
  {
    model = mod;
    
    /* Cache the attributes */
    final List<String> names = new ArrayList<String>( 10 );
    final List<ComponentSlot> attrs = new ArrayList<ComponentSlot>( 10 );
    final Iterator i = model.getComponentAttributes();
    while( i.hasNext() ) {
      final String attr = (String) i.next();
      names.add( attr );
      attrs.add( model.getCompAttribute( attr ) );
    }
    attrNames = names.toArray( new String[0] );
    values = attrs.toArray( new ComponentSlot[0] );
    
    model.addModelListener( this );
  }
  
  
  
  @Override
  public int getColumnCount()
  {
    return COLUMN_COUNT;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public Class getColumnClass( final int idx )
  {
    return colClasses[idx];
  }
  
  @Override
  public String getColumnName( final int idx )
  {
    return colHeaders[idx];
  }
  
  @Override
  public int getRowCount()
  {
    return attrNames.length;
  }
  
  @Override
  public Object getValueAt( final int attrIdx, final int colIdx )
  {
    if( colIdx == ATTR_NAME_COL ) {
      return attrNames[attrIdx];
    } else if( colIdx == ATTR_VAL_COL ) {
      if( values[attrIdx].isValid() ) {
        return model.compValueToString( attrNames[attrIdx] );
      } else {
        return UNDEFINED;
      }
    } else {
      return null;
    }
  }  

  @Override
  public void breakView( final ModelEvent e )
  {  
    if( !(e instanceof RebuildEvent) ) fireTableDataChanged();
  }
  
  @Override
  public void addedToModel(final Model m) {
    // do nothing
  }
  
  @Override
  public void removedFromModel(final Model m) {
    // do nothing
  }
}



/**
 * Table model for rendering node-level attributes.  Assumes that no
 * attributes are added after the model is created.  This is, in general,
 * a false assuption, but true enough for current purposes.
 */
final class NodeAttributesModel
extends AbstractTableModel
implements ModelListener
{
  /** String for undefined attributes. */
  private static final String UNDEFINED = "<Undefined>";

  /** Column name for the IRNode column. */
  private static final String IRNODE_HEADER = "Node";
 
  /** Column name for the IRNode column. */
  private static final int IRNODE_IDX = 0;
 
  
  
  /** The model whose attributes are being tabled. */
  private final Model model;
  
  /** Cache of the attribute names */
  private final String[] attrNames;
  
  /** The attribute value storage for the different attributes. */
  private final SlotInfo[] values;

  /** Ordered list of the nodes in the model (Cache). */
  private final List<IRNode> nodes;
  
  
  
  
  public NodeAttributesModel( final Model mod )
  {
    model = mod;
    
    /* Cache the attributes */
    final List<String> names = new ArrayList<String>( 10 );
    final List<SlotInfo> attrs = new ArrayList<SlotInfo>( 10 );
    final Iterator i = model.getNodeAttributes();
    while( i.hasNext() ) {
      final String attr = (String) i.next();
      names.add( attr );
      attrs.add( model.getNodeAttribute( attr ) );
    }
    attrNames = names.toArray( new String[0] );
    values = attrs.toArray( new SlotInfo[0] );
    nodes = new ArrayList<IRNode>();
    updateNodes();
    
    model.addModelListener( this );
  }
  
  
  
  private void updateNodes()
  {
    nodes.clear();
    final Iterator<IRNode> i = model.getNodes();
    while( i.hasNext() ) {
      nodes.add( i.next() );
    }
  }
  
  
  @Override
  public int getColumnCount()
  {
    return 1 + attrNames.length;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public Class getColumnClass( final int idx )
  {
    return String.class;
  }
  
  @Override
  public String getColumnName( final int idx )
  {
    if( idx == IRNODE_IDX ) return IRNODE_HEADER;
    else return attrNames[idx-1];
  }
  
  @Override
  public int getRowCount()
  {
    return nodes.size();
  }
  
  @Override
  public Object getValueAt( final int rowIdx, final int colIdx )
  {
    try {
      final IRNode node = nodes.get( rowIdx );
      if( colIdx == IRNODE_IDX ) {
        return model.idNode( node );
      } else if( colIdx <= attrNames.length ) {
        final int attrIdx = colIdx - 1;
        if( node.valueExists( values[attrIdx] ) ) {
          return model.nodeValueToString( node, attrNames[attrIdx] );
        } else {
          return UNDEFINED;
        }
      } else {
        return null;
      }
    } catch( IndexOutOfBoundsException e ) {
      return null;
    }
  }  

  @Override
  public void breakView( final ModelEvent e )
  {  
    if(e.shouldCauseRebuild()) {
      updateNodes();
      fireTableDataChanged();
    }
  }
  
  @Override
  public void addedToModel(final Model m) {
    // do nothing
  }
  
  @Override
  public void removedFromModel(final Model m) {
    // do nothing
  }
}
