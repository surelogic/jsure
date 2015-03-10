package edu.cmu.cs.fluid.project;

import java.util.Hashtable;

import edu.cmu.cs.fluid.util.UniqueID;

/**
 * An object which creates unique component on-demand from identifiers.
 * The factory is used to create components before loading them; it is NOT used
 * to create a new (in the fluid persistence sense) component.  For that, one
 * simply uses the constructor.  Component factories are used to enable the 
 * persistence system to read in a reference to a component without knowing what
 * class it is. Component factories are only distinguished by their class, 
 * not by anything else.  It does not work to have two factories of the same class
 * that do different things.
 * @author Tien
 */
public abstract class ComponentFactory {
  private static final Hashtable<String,ComponentFactory> allFactories = 
    new Hashtable<String,ComponentFactory>();

  public ComponentFactory() 
  {
    allFactories.put(getName(),this);
  }
   
  public String getName() {
    return this.getClass().getName(); // guaranteed unique (per class)
  }

  public static ComponentFactory getComponentFactory(String factName) {
    ComponentFactory factory = allFactories.get(factName);
    if (factory == null) {
      try {
        Class.forName(factName);
        factory = allFactories.get(factName);
        if (factory == null) {
//          System.out.println("ComponentFactory class " + factName + " does not define a prototype !");
          return null;
        }
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
    }
    return factory;
  }

  /**
   * Create a component given the ID.  The component
   * itself will still need to be loaded.
   * @param id unique identifier for the component
   * @return a component with this ID (new in the Java sense, old in the Fluid IR sense)
   */
  public abstract Component create(UniqueID id);
}
