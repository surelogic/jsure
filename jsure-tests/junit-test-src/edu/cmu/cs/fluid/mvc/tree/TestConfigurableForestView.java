// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/TestConfigurableForestView.java,v 1.24 2007/01/12 18:53:30 chance Exp $

package edu.cmu.cs.fluid.mvc.tree;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ConfigurableView;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelUtils;
import edu.cmu.cs.fluid.mvc.SimpleProxySupportingAttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.predicate.SimplePredicateViewFactory;
import edu.cmu.cs.fluid.mvc.visibility.PredicateBasedVisibilityViewFactory;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;

public class TestConfigurableForestView
{
  private static class proxyPolicy
  implements ForestProxyAttributePolicy
  {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public AVPair[] attributesFor( Model model, Set skippedNodes )
    {
      final StringBuilder buf = new StringBuilder( "... [" );
      final Iterator<IRNode> nodes = skippedNodes.iterator();
      while( nodes.hasNext() ) {
        final IRNode node = nodes.next();
        buf.append( model.idNode( node ) );
        if( nodes.hasNext() ) buf.append( ", " );
      }
      buf.append( ']' );
      return new AVPair[] { new AVPair( LabeledForest.LABEL, buf.toString() ) };
    }
	@Override
    public AVPair[] attributesFor( ForestModel model, IRNode root )
    {
      return new AVPair[] { new AVPair( LabeledForest.LABEL,
                                        "... [" + model.idNode( root ) + "]" ) };
    }
  }



  @SuppressWarnings("unchecked")
  public static void main( final String[] args )
  throws Exception
  { 
    final LabeledForest forest =
      StandardLabeledForestFactory.mutablePrototype.create(
       "My Forest", SimpleSlotFactory.prototype );

    final BufferedReader input =
      new BufferedReader( new InputStreamReader( System.in ) );

    // Init PredicateModel
    final PredicateModel predModel = 
      SimplePredicateViewFactory.prototype.create( "Predicate Model", forest );

    // Init Visibility Model
    final VisibilityModel visModel =
      PredicateBasedVisibilityViewFactory.prototype.create(
        "Visibility Model", forest, predModel );

    final ConfigurableForestView config = 
      ConfigurableForestViewFactory.prototype.create(
        "Config", forest, visModel,
        SimpleProxySupportingAttributeInheritancePolicy.prototype,
        new proxyPolicy(), NoEllipsisForestEllipsisPolicy.prototype,
        false, true );

    final ForestDumper dumper = new ForestDumper( config, System.out );

    final Map<String,IRNode> labels = new HashMap<String,IRNode>();
    while( true ) {
      System.out.println(   "quit, root <label>, node <parent> <label>,\n"
                          + "hide <label>, show <label>, expand <label>, collapse <label>,\n"
                          + "flatten, path, vertical, policy [top|bottom|many|none]\n"
                          + "chide <label>, cshow <label>, cexpand <label>, ccollapse <label>,\n"
                          + "cflatten, cpath, cvertical, cpolicy [top|bottom|many|none]\n" );
      System.out.print( "> " );
      
      final StringTokenizer st = new StringTokenizer( input.readLine() );
      final String cmd = st.nextToken().intern();
      
      if( cmd == "quit" ) {
        ModelUtils.shutdownChain(config);
        break;
      } else if( cmd == "root" ) {
        final String label = st.nextToken();
        final IRNode node = new PlainIRNode();
        forest.initNode( node );
        forest.addRoot( node );
        dumper.waitForBreak();
        forest.setLabel( node, label );
        dumper.waitForBreak();
        labels.put( label, node );
      } else if( cmd == "node" ) {
        final String label1 = st.nextToken();
        final String label2 = st.nextToken();
        final IRNode parent = labels.get( label1 );
        final IRNode node = new PlainIRNode();
        forest.initNode( node );
        forest.appendSubtree( parent, node );
        dumper.waitForBreak();
        forest.setLabel( node, label2 );
        dumper.waitForBreak();
        labels.put( label2, node );
      } else if( cmd == "hide" ) { // hide node: "h <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        node.setSlotValue(
          config.getNodeAttribute( ConfigurableView.IS_HIDDEN ), Boolean.TRUE );
        dumper.waitForBreak();
      } else if( cmd == "show" ) { // show node: "h <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        node.setSlotValue(
          config.getNodeAttribute( ConfigurableView.IS_HIDDEN ), Boolean.FALSE );
        dumper.waitForBreak();
      } else if( cmd == "expand" ) { // expand node: "x <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        node.setSlotValue(
          config.getNodeAttribute( ConfigurableForestView.IS_EXPANDED ), Boolean.TRUE );
        dumper.waitForBreak();
      } else if( cmd == "collapse" ) { // collapse node: "x <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        node.setSlotValue(
          config.getNodeAttribute( ConfigurableForestView.IS_EXPANDED ), Boolean.FALSE );
        dumper.waitForBreak();
      } else if( cmd == "flatten" ) { // flatten tree
        config.getCompAttribute(
          ConfigurableForestView.VIEW_MODE ).setValue( config.getEnumElt(ConfigurableForestView.VIEW_FLATTENED) );
        dumper.waitForBreak();
      } else if( cmd == "path" ) { // set path-to-root
        config.getCompAttribute(
            ConfigurableForestView.VIEW_MODE ).setValue( config.getEnumElt(ConfigurableForestView.VIEW_PATH_TO_ROOT) );
        dumper.waitForBreak();
      } else if( cmd == "vertical" ) { // set verical ellipsis mode
        config.getCompAttribute(
            ConfigurableForestView.VIEW_MODE ).setValue( config.getEnumElt(ConfigurableForestView.VIEW_VERTICAL_ELLIPSIS) );
        dumper.waitForBreak();
      } else if( cmd == "policy" ) { // set ellipsis policy: "ep [top|bottom|many]"
        ForestEllipsisPolicy policy = NoEllipsisForestEllipsisPolicy.prototype;
        final String policyStr = st.nextToken().intern();
        if( policyStr == "top" ) {
          policy = new SingleEllipsisForestEllipsisPolicy( config, false );
        } else if( policyStr == "bottom" ) {
          policy = new SingleEllipsisForestEllipsisPolicy( config, true );
        } else if( policyStr == "many" ) {
          policy = new MultipleEllipsisForestEllipsisPolicy( config );
        }
        config.getCompAttribute(
          ConfigurableForestView.ELLIPSIS_POLICY ).setValue( policy );
        dumper.waitForBreak();
      } else if( cmd == "chide" ) { // hide node: "h <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        config.setHidden( node, true );
        dumper.waitForBreak();
      } else if( cmd == "cshow" ) { // show node: "h <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        config.setHidden( node, false );
        dumper.waitForBreak();
      } else if( cmd == "cexpand" ) { // expand node: "x <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        config.setExpanded( node, true );
        dumper.waitForBreak();
      } else if( cmd == "ccollapse" ) { // collapse node: "x <label>"
        final String label = st.nextToken();
        final IRNode node = labels.get( label );
        config.setExpanded( node, false );
        dumper.waitForBreak();
      } else if( cmd == "cflatten" ) { // flatten tree
        config.setViewFlattened();
        dumper.waitForBreak();
      } else if( cmd == "cpath" ) { // set path-to-root
        config.setViewPathToRoot();
        dumper.waitForBreak();
      } else if( cmd == "cvertical" ) { // set vertical ellipsis mode
        config.setViewVerticalEllipsis();
        dumper.waitForBreak();
      } else if( cmd == "cpolicy" ) { // set ellipsis policy: "ep [top|bottom|many]"
        ForestEllipsisPolicy policy = NoEllipsisForestEllipsisPolicy.prototype;
        final String policyStr = st.nextToken().intern();
        if( policyStr == "top" ) {
          policy = new SingleEllipsisForestEllipsisPolicy( config, false );
        } else if( policyStr == "bottom" ) {
          policy = new SingleEllipsisForestEllipsisPolicy( config, true );
        } else if( policyStr == "many" ) {
          policy = new MultipleEllipsisForestEllipsisPolicy( config );
        }
        config.setForestEllipsisPolicy( policy );
        dumper.waitForBreak();
      }
    }
  }
}

