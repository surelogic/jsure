/*
 * Created on May 14, 2004
 */
package edu.cmu.cs.fluid.tree;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import edu.cmu.cs.fluid.ir.*;

import java.util.HashSet;
/**
 * @author Amit Khare
 *
 * This class can be used to find out the Strongly Connected components     
 * of a CFG.
 *
 */
public class StronglyConnectedComponents {  
  /* This function does the DFS traversal of a given Graph*/
  
  public List dfsTraversal(SymmetricDigraphInterface d , IRNode root)
  {
    IRNode node;
    List<IRNode> arr = new ArrayList<IRNode>();
    HashSet<IRNode> hash = new HashSet<IRNode>();
    Stack<IRNode> sta = new Stack<IRNode>();
    
    sta.push(root);
    
    while(!sta.empty())
    {
      
      node = sta.pop();
      
      if (!hash.contains(node))
      {
        
        arr.add(node);
        hash.add(node);
        
        Iterator<IRNode> e = d.children(node);
        
        while(e.hasNext())
        {
          IRNode currentChild = e.next();
          sta.push(currentChild);
        }
      }		
      
    }	
    
    return arr;
    
  }
  
  /*This function does the post Order Traversal for Directed Graph     */
  
  public List<IRNode> postOrderTraversal(SymmetricDigraphInterface d, IRNode roots)
  {
    
    IRNode node = roots ;
    ArrayList<IRNode> arr = new ArrayList<IRNode>();
    HashSet<IRNode> hash = new HashSet<IRNode>();
    postOrderHelper(d,node,hash,arr);
    
    return arr;
  }
  
  /* This function is the helper function for post order traversal  */
  
  public static void postOrderHelper(SymmetricDigraphInterface d, IRNode node, HashSet<IRNode> hash, List<IRNode> arr)
  {
    
    if (!hash.contains(node))
    {
      Iterator<IRNode> e = d.children(node);
      hash.add(node);
      
      while(e.hasNext())
      {
        IRNode currentChild = e.next();
        
        if (!hash.contains(currentChild))
        {
          postOrderHelper(d,currentChild,hash,arr);
        }	
        
      }
      
      arr.add(node);   	    	    
    }
  }
  
  
  /*This function does the backward DFS traversal of a graph */
  private static List<IRNode> backwardDFSTraversal(SymmetricDigraphInterface d , IRNode leaf, HashSet<IRNode> hash)
  {
    IRNode node;
    List<IRNode> arr = new ArrayList<IRNode>();
    //HashSet hash = new HashSet();
    Stack<IRNode> sta = new Stack<IRNode>();
    
    sta.push(leaf);
    
    while(!sta.empty())
    {
      
      node = sta.pop();
      
      if (!hash.contains(node))
      {
        
        arr.add(node);
        hash.add(node);
        
        Iterator<IRNode> e = d.parents(node);
        
        while(e.hasNext())
        {
          IRNode currentParent = e.next();
          sta.push(currentParent);
        }
      }		
      
    }	
    
    return arr;
  }
  
  
  /* This function deletes elements after comparison */
  public static List<IRNode> compareReverseBack(List<IRNode> rev, List<IRNode> bac)
  {
    //ArrayList arr = new ArrayList();	
    
    for (int i = 0; i <= bac.size()-1; i++)
    {
      rev.remove(bac.get(i));
    }
    
    return rev;
  }
  
  /** This function returns a list of nodes in the SID reachable
   * from the roots (in either direction).  The list of nodes includes
   * the strongly connected regions of the nodes in reverse postorder,
   * and within each region, they are ordered in reverse postorder.
   * @param sdi
   * @param roots
   * @return
   */
  
  public static List sccPostOrder(SymmetricDigraphInterface sdi, IRNode[] roots)
  {
    List<IRNode> reverse = new ArrayList<IRNode>();
    List<IRNode> srpostorder = new ArrayList<IRNode>();
    //List<IRNode> sccNodes = new ArrayList<IRNode>();
    HashSet<IRNode> hash = new HashSet<IRNode>();
    HashSet<IRNode> hash1 = new HashSet<IRNode>();
    ArrayList<IRNode> post = new ArrayList<IRNode>();
    
    int x = 0;
    
    // Feeding the nodes one at a time to get a post order traversal	    
    for (int i = 0; i <= roots.length-1 ; i++)
    {
      postOrderHelper(sdi,roots[i],hash,post);
    }
    
    
    //Reversing the ArrayList List to get the list in reverse post order
    for (int j = post.size()-1 ; j >= 0; j--)
      reverse.add(post.get(j));
    
    
    while(!reverse.isEmpty())
    {	
      
      List<IRNode> back =  backwardDFSTraversal(sdi,reverse.get(x),hash1);
      
      for (Iterator<IRNode> it1 = back.iterator(); it1.hasNext();)
        srpostorder.add(it1.next()); 
      
      reverse = compareReverseBack(reverse,back); 
      
    } 
    
    return srpostorder;
  }
  
  
}
