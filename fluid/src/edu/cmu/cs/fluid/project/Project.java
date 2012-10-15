package edu.cmu.cs.fluid.project;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRPersistentReferenceType;
import edu.cmu.cs.fluid.ir.IRRegion;
import edu.cmu.cs.fluid.ir.IRStringType;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.tree.PropagateUpTree;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.util.Base64InputStream;
import edu.cmu.cs.fluid.util.Base64OutputStream;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.UniqueID;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.OverlappingEraException;
import edu.cmu.cs.fluid.version.SharedVersionedRegion;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedChangeRecord;
import edu.cmu.cs.fluid.version.VersionedChunk;
import edu.cmu.cs.fluid.version.VersionedRegion;
import edu.cmu.cs.fluid.version.VersionedSlotFactory;
import edu.cmu.cs.fluid.version.VersionedSlotInfo;

/**
 * A Project is a tree of named components each of which havetheir own data.
 * The project keeps track of the tree structure, a tree-changed attribute 
 * (that also records when a name is changed) and of the connection between
 * the component node (and IRNode) and the component itself.
 * @author Tien
 */
public class Project {
    
  private static Logger LOG = SLLogger.getLogger("FLUID.project");
  
  protected FileLocator fileLocator;
  
  // tree of components
  private static Tree componentHierarchy;  
  // TC
  private static VersionedChangeRecord tc;
  
  // Root of the componentHierachy tree, root cannot change.
  private IRNode root;
  
  private static VersionedSlotInfo<String> componentNameAttr;
  
  // component attribute
  private static SlotInfo<Component> componentAttr;
  // the bundle of attributes for the component hierarchy
  private static Bundle configBundle;

  /** The current name of the project. */
  private String name;
  
  /** The components, each of which is a node. */
  private VersionedRegion components;

  /** Tables mapping version names to versions and vice versa. */
  private Map<Object,String> version2nameTable = new HashMap<Object,String>();
  /** This map maps names to versions <em>OR</em> to pairs of Era IDs and offsets */
  private Map<String,Object> name2versionTable = new HashMap<String,Object>();
  
  /** This table is needed to store the version name mapping
   * as it is read from an ASCII configuration file.
   *
  private Hashtable name2eraoffsetTable = new Hashtable();
  */

  
  // Remember which the delta and component delta for the same eras
  // are loaded.
  static Vector<Era> delta_loaded_eras = new Vector<Era>();
  static Vector<Era> comp_delta_loaded_eras = new Vector<Era>();

  static {
    try {      
      componentHierarchy =
         new Tree("Config.components",VersionedSlotFactory.prototype);   
      tc = new VersionedChangeRecord("Config.treeChanged");
      PropagateUpTree.attach(tc,componentHierarchy);
      componentNameAttr =
        (VersionedSlotInfo<String>) VersionedSlotFactory.makeSlotInfo("Config.component.name",
            IRStringType.prototype);
      componentAttr =
        VersionedSlotFactory.makeSlotInfo("Config.component.attr",
                                          IRPersistentReferenceType.<Component>getInstance());            
      configBundle = Bundle.loadBundle(UniqueID.parseUniqueID("config"),
               IRPersistent.fluidFileLocator);
    } catch (SlotAlreadyRegisteredException ex) {
//      System.out.println("Config attribute names have been registered.");
      System.exit(1);
    } catch (IOException ex) {
      System.out.println(ex.toString());
    }
  }

  static {
    Era.ensureLoaded();
    VersionedChunk.ensureLoaded();
    Component.ensureLoaded();
  }
  
  // constructors
  public Project(String name) {
    this(name,null);
  }
  
  /** New configuration
   * comp: is root component
   */
  public Project(String name, Component comp) {
    // if (comp == null) throw new FluidRuntimeException("Null cannot be a root");
    this.name = name;
    components = new VersionedRegion();
    root = newComponent(comp);
  }

  // Existing one
  public Project(String name, VersionedRegion vr, IRNode r) {
    if (r == null) 
      throw new FluidRuntimeException("Null cannot be a root");
    if (vr == null) 
      throw new FluidRuntimeException("Versioned region cannot be null");
    root = r;
    this.name = name;
    components = vr;
  }
  
  public Project share(String name, Version v) {
    SharedVersionedRegion svr = SharedVersionedRegion.get(components,v);
    IRNode newRoot = svr.getProxy(root);
    return new Project(name,svr,newRoot);
  }

  /** Save attributes in a bundle */
  private static void saveAttributes(Bundle b) {
    componentHierarchy.saveAttributes(b);
    b.saveAttribute(componentAttr);   
    b.saveAttribute(componentNameAttr);
  }

  // used only to create the initial bundle
  public static void main(String args[]) {
    Bundle b = getBundle();
    if (b != null) {
      b.describe(System.out);
      return;
    }
    b = new Bundle();
    saveAttributes(b);
    try {
      b.store(IRPersistent.fluidFileLocator);
      System.out.println("Saving config.bundles");
    } catch (IOException ex) {
      System.out.println(ex.toString());
      System.out.println("Please press return to try again");
      try {
        System.in.read();
        b.store(IRPersistent.fluidFileLocator);
      } catch (IOException ex2) {
        System.out.println(ex2.toString());
      }
    }
    System.out.println("Now you must rename " + b.getID() + ".ab as config.ab");
  }
  
  /** Create a new component for this configuration.
   */
  public IRNode newComponent(Component comp) {
    return newComponent(comp,null);
  }
  
  /**
   * Create a component node to attach to the new component passed in.
   * The component must not already belong to any project.
   * @param comp component to add (must be new or null)
   * @param name name (updateable) to assign
   * @return node in the project tree associated with this component
   */
  public IRNode newComponent(Component comp, String name) {
    if (components == null) return null;
    IRNode n = new PlainIRNode(components);
    componentHierarchy.initNode(n);
    // componentHierarchy.removeSubtree(n);
    setComponent(n,comp);   
    if (comp != null) comp.setProjectNode(n);
    setComponentName(n,name);
    return n;
  }
  
  // accessors and mutators

  /** Return the current name of this project. */
  public String getName() { return name; }

  /** Change the current name of this project. */
  public void setName(String s) { name = s; }

  public String getComponentName(IRNode n) {
    return n.getSlotValue(componentNameAttr);
  }
  public void setComponentName(IRNode n, String s) {
    if (s == null) s = "";
    n.setSlotValue(componentNameAttr,s);
    tc.setChanged(n);
  }
  
  public boolean nameChanged(IRNode n, Version v1, Version v2) {
    Version.saveVersion(v1);
    try {
      String s1, s2;
      s1 = getComponentName(n);
      Version.setVersion(v2);
      s2 = getComponentName(n);
      return !s1.equals(s2);
    } finally {
      Version.restoreVersion();
    }
  }
  
  /** Get the component associated with a node */
  public Component getComponent(IRNode n) {
    if (n.valueExists(componentAttr)) 
      return n.getSlotValue(componentAttr);
    else return null;
  }

  /** Set component for a node */
  public void setComponent(IRNode n, Component comp) {
    n.setSlotValue(componentAttr,comp);
  }

  /** Return all components as an enumeration of IR nodes. 
   *  Return an enumeration of all nodes in the region 
   *  from the initial time to the era of the version given.
   *  If the version is not yet in an era, we use the nodes 
   *  in it and parent versions back until one *is* in an era. 
   */
  public Iterator getComponents(Version v) {
    return components.allNodes(v);
  }

  public VersionedRegion getRegion() {
    return components;
  }

  /** Return the config bundle in this project. */
  public static Bundle getBundle() { return configBundle; }

    /** Get the root of componentHierarchy */
  public IRNode getRoot() {
    return root;
  }

  public static Tree getTree() { return componentHierarchy;}
  
  // TC
  public static VersionedChangeRecord getTC() { return tc;}  
  
  public static Iterator<IRNode> changeIterator(IRNode compNode, Version v1, Version v2) {
    return tc.iterator(componentHierarchy, compNode, v1, v2);
  }

  // MANAGE THE NAME TABLES
  
  protected Version pair2version(Pair p) {
    UniqueID id = (UniqueID) p.first();
    int off = ((Integer)p.second()).intValue();
    try {
      Era era = Era.loadEra(id,fileLocator);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Loaded era " + era);
        era.describe(System.out);
      }
      return era.getVersion(off);
    } catch (IOException e) {
      LOG.warning("Cannot load era " + e);
      return null;
    }
  }
  
  protected Pair<UniqueID,Integer> version2pair(Version v) {
    return new Pair<UniqueID,Integer>(v.getEra().getID(),new Integer(v.getEraOffset()));
  }
  
  /** Return the version known by this name in this configuration.
   * @return version associated currently with this name, or null
   */
  public synchronized Version lookupVersion(String name) {
    Object v = name2versionTable.get(name);
    if (v instanceof Pair) {
      Version v1 = pair2version((Pair)v);
      name2versionTable.put(name,v1);
      version2nameTable.put(v1,name);
      v = v1;
    }
    return (Version)v;
  }

  /** Return the name this version is currently known by 
   * in the configuration.
   * @return string for this version or null
   */
  public synchronized String getVersionName(Version v) {
    String name = version2nameTable.get(v);
    if (name != null) return name;
    Pair p = version2pair(v);
    return version2nameTable.get(p);
  }

  /**
   * Get the list of versions' names
   */
  public Iterator getAllVersionNames() {
    return name2versionTable.keySet().iterator();
  }

  /* should not export this information
  public Hashtable getname2eraoffsetTable() {
    return name2eraoffsetTable;
  }
  
  public Hashtable getname2versionTable() {
    return name2versionTable;
  }
  */
  
  // LOAD/SAVE DELTAS and SNAPSHOTS
  
  /** loadDelta */
  public void loadDelta(Era era, FileLocator floc)
    throws IOException {
    
    VersionedChunk vc = VersionedChunk.get(components,configBundle);
    vc.getDelta(era).load(floc);
    // System.out.println("Describing the versioned chunk");
    // vc.describe(System.out);
    
  }
  
  /** Load deltas for components */
  public void loadComponentDelta(Era era, int offset, FileLocator floc)
    throws IOException {
    Version.saveVersion();
    Version.setVersion(era.getVersion(offset));        
    Iterator enm = components.allNodes(era.getVersion(offset));
    while (enm.hasNext()) {
      IRNode node = (IRNode) enm.next();
      if (node.valueExists(componentAttr)) {
        Component comp = node.getSlotValue(componentAttr);
        if (comp != null) {
          comp.getDelta(era).load(floc);
          comp.loadDelta(era,floc);
        }
        // System.out.println("Loaded delta for component " 
        //                    + comp.getName(era.getVersion(offset)));
      }      
    }
    Version.restoreVersion();    
  }

  /** saveDelta */
  public void saveDelta(Era era, FileLocator floc)
    throws IOException {    
    saveRegion(floc);
    VersionedChunk ch = VersionedChunk.get(components,configBundle);  
    IRPersistent vcd = ch.getDelta(era);
    vcd.store(floc);
    // vcd.describe(System.out);    
  }

  /** Save deltas for components in a particular version */
  public void saveComponentDelta(Version v, Era era, FileLocator floc)
    throws IOException {
    // save deltas for components        
    Iterator enm = getComponents(v);
    while (enm.hasNext()) {
      IRNode node = (IRNode) enm.next();
      if (node.valueExists(componentAttr)) {
        Component comp = node.getSlotValue(componentAttr);
        // System.out.println("SAVING DELTA for \"" + comp.getName(v) + "\" ... (Slots)");
        comp.getDelta(era).store(floc);
        // System.out.println("SAVING DELTA for \"" + comp.getName(v) + "\" ...(Attrs)");
        comp.saveDelta(era,floc);
      }      
    }        
  }

  /** Save deltas for components in an era , not a particular version */
  public void saveComponentDeltaForEra(Era era, FileLocator floc)
    throws IOException {
    // save deltas for components
    // Iterator enum = getComponents(era.getVersion(1));
    Iterator enm = components.allNodes(era);
    while (enm.hasNext()) {
      IRNode node = (IRNode) enm.next();
      if (node.valueExists(componentAttr)) {
        Component comp = node.getSlotValue(componentAttr);
        if (comp == null) continue;
        // System.out.println("SAVING DELTA for \"" + comp.getName(era.getRoot()) + "\" ... (Slots)");
        comp.getDelta(era).store(floc);
        // System.out.println("SAVING DELTA for \"" + comp.getName(era.getRoot()) + "\" ...(Attrs)");
        comp.saveDelta(era,floc);
      }
      else throw new IOException("Error in storing this project");
    }    
  }

  /** Load the snapshot of this component for the given version. */
  public void loadSnapshot(Version v, FileLocator floc) 
    throws IOException {    
    // System.out.println("Loading snapshot  ...");
    VersionedChunk vc = VersionedChunk.get(components,configBundle);
    ((IRPersistent) vc.getSnapshot(v)).load(floc); 
    // vc.describe(System.out);
    Iterator enm = getComponents(v);
    while (enm.hasNext()) {
      IRNode node = (IRNode) enm.next();
      if (node.valueExists(componentAttr)) {
        Component comp = node.getSlotValue(componentAttr);
        comp.getSnapshot(v).load(floc);
        comp.loadSnapshot(v,floc);
      }
      else throw new FluidRuntimeException("Error in loading this configuration");
    }    
  }
  
  /** Store a snapshot of this component for the given version. */
  public void saveSnapshot(Version v, FileLocator floc) 
    throws IOException {
    saveRegion(floc);
    // Save the deltas for docTreeBundle
    // System.out.println("Saving snapshot  ... ");
    VersionedChunk ch = VersionedChunk.get(components,configBundle);
    IRPersistent vcs = ch.getSnapshot(v);
    vcs.store(floc);
    // vcs.describe(System.out);
    Iterator enm = getComponents(v);
    while (enm.hasNext()) {
      IRNode node = (IRNode) enm.next();
      if (node.valueExists(componentAttr)) {
        Component comp = node.getSlotValue(componentAttr);
        comp.saveSnapshot(v,floc);
        comp.getSnapshot(v).store(floc);
      }
      else throw new FluidRuntimeException("Error in storing this configuration");
    }
  }

  // ASCII FILE READ/WRITE

  public void storeASCII(Writer w) throws IOException, FluidRuntimeException {
    // 1. write the project's name
    w.write(name + "\n");
    // 2. write versioned region's name
    w.write(components.getID().toString() + "\n");
    // 3. Write the reference to the region that the root node belongs to
    DataOutputStream os = new DataOutputStream(new Base64OutputStream(w));
    IRRegion.getOwner(root).describe(System.out);
    IRRegion.getOwner(root).writeReference(os);
    os.flush();
    // 4. Write the index of the root node within the region
    w.write(IRRegion.getOwnerIndex(root) +"\n");
    // 5. Write version name mapping infos
    for (Iterator it = name2versionTable.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry)it.next();
      String vname = (String)e.getKey();
      w.write(vname + ", ");
      Object x = e.getValue();
      if (x instanceof Version) {
        x = version2pair((Version)x);
      }
      Pair p = (Pair)x;
      w.write(p.first() + ", ");
      w.write(p.second() + "\n");
    }
    w.flush();
  }
  
  public static Project loadASCII(Reader r, FileLocator floc) 
    throws IOException {
    // Parse the ASCII file to get name, region's name 
    // and mapping tables
    BufferedReader br = new BufferedReader(r);
    // 1. read the project's name
    String config_name = br.readLine();
    // 2. read versioned region's name, then load versioned region
    String region_name = br.readLine();
    VersionedRegion vr = loadRegionFromName(region_name,floc);
    if (vr != null) vr.describe(System.out);
    else throw new IOException("Versioned region is null !");
    // 3. read in the region
    StringReader sr = new StringReader(br.readLine());
    Base64InputStream base64 = new Base64InputStream(sr);
    DataInputStream is = new DataInputStream(base64);
    IRRegion ir_region = (IRRegion) IRPersistent.readReference(is);
    // 4. read the index of the root
    int index = Integer.parseInt(br.readLine());
    IRNode root_node = ir_region.getNode(index);
    Project project = new Project(config_name,vr,root_node);
    project.fileLocator = floc;
    // 5. read the mapping info
    String s = br.readLine();
    while (s != null) {
      StringTokenizer stokenizer = new StringTokenizer(s,",");
      String vname = stokenizer.nextToken().trim();
      String eraname = stokenizer.nextToken().trim();
      String offset = stokenizer.nextToken().trim();
      Pair<UniqueID,Integer> p = new Pair<UniqueID,Integer>(UniqueID.parseUniqueID(eraname), Integer.valueOf(offset));
      project.assignPairName(p,vname);
      s = br.readLine(); 
    }
    return project;
  }

  /**
   * Save the region of the document
   */
  public void saveRegion(FileLocator floc) {
    if (!components.isStored()) {
      // System.out.println("Saving REGION ...");
      try {
        components.store(floc);
        // components.describe(System.out);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /** Load a region from a name */
  public static VersionedRegion loadRegionFromName(String rname, FileLocator floc)
  {
    try {
      
      UniqueID id = UniqueID.parseUniqueID(rname);
      VersionedRegion vr = VersionedRegion.loadVersionedRegion(id,floc);
      return vr;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  // VERSIONING OPERATIONS

  /** Return a list of unassigned versions */
  public String[] unAssignedVersions() {
    Iterator<Object> it = version2nameTable.keySet().iterator();
    ArrayList<String> result = new ArrayList<String>();
    while (it.hasNext()) {
      Object key = it.next();
      if (key instanceof Version &&
          isInASavedEra((Version)key) == false)
        result.add(version2nameTable.get(key));
    }
    return result.toArray(SLUtility.EMPTY_STRING_ARRAY);
  }
  
  /** Associate this version with the given name.
   *  Breaks any other previous association for this name.
   */
  public void assignVersionName(Version version, String name) {
    assignKeyName(version,name);
  }
  protected void assignPairName(Pair<UniqueID,Integer> pair, String name) {
    assignKeyName(pair,name);
  }
  private void assignKeyName(Object key, String name) {
    Object oldKey = name2versionTable.get(name);
    if (oldKey!= null && name.equals(version2nameTable.get(oldKey))) {
      version2nameTable.remove(oldKey);
    }
    name2versionTable.put(name,key);
    version2nameTable.put(key,name);
  }
  
  public boolean isInASavedEra(Version v) {
    Era e = v.getEra();
    if ((e != null) && e.isStored()) return true;
    else return false;
  }
  
   
  /** Save an era and its ancestors if they have not been saved 
   *  (Recursive)
   */  
  private void saveEraAndDelta(Era era, FileLocator floc)
    throws IOException {
    boolean isParentEraSaved = false;
    Version r = era.getRoot();
    if (r == Version.getInitialVersion()) isParentEraSaved = true;
    Era parent_era = r.getEra();
    if (parent_era != null && parent_era.isStored()) isParentEraSaved = true;
    
    if (!isParentEraSaved) saveEraAndDelta(parent_era,floc);
    
    try {
      era.complete();
      saveDelta(era,floc);
      saveComponentDeltaForEra(era,floc);
      era.store(floc);      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /** Save a version of this project
   */
  public void saveVersionByDelta(Version v, FileLocator floc) throws IOException {
    
    // if the version v is already saved in an era, done.
    if (isInASavedEra(v)) return;
    // if it is in an era but not saved, then save the era and make sure all ancestor
    // eras are saved.
    Era era = v.getEra();
    if (era != null && (!era.isStored())) {
      saveEraAndDelta(era,floc);
      return;
    }
    // Here, era is null, create a new era
    Version r = v.parent();
    // find the root of this editing session
    Version alpha = Version.getInitialVersion();
    while (r != alpha) {
      // System.out.println("Examine " + version2nameTable.get(r));
      if (r.getEra() != null) break;
      r = r.parent();
    }
    
    // if r.getEra() has not been saved
    if (r != alpha && (!r.getEra().isStored())) 
      saveEraAndDelta(r.getEra(),floc);
        
    // create and save the era
    try {
      
      Era e = new Era(r, new Version[]{v});      
      
      // Save delta in this era
      // System.out.println("SAVING the DELTA for the new ERA");
      saveDelta(e,floc);
      saveComponentDelta(v,e,floc); //? or saveComponentDeltaForEra(e,floc);
      
      // System.out.println("Saving ERA ...");
      e.store(floc);
      // e.describe(System.out);
    } catch (OverlappingEraException ex) {
        System.out.println("Overlapping eras!");
        ex.printStackTrace();
    }
  }

  /** Save an era and its ancestors if they have not been saved 
   *  using snapshot (Recursive)
   */ 
  private void saveEraAndSnapshot(Era era, FileLocator floc)
    throws IOException {
    boolean isParentEraSaved = false;
    Version r = era.getRoot();
    if (r == Version.getInitialVersion()) isParentEraSaved = true;
    Era parent_era = r.getEra();
    if (parent_era != null && parent_era.isStored()) isParentEraSaved = true;
    
    if (!isParentEraSaved) saveEraAndSnapshot(parent_era,floc);
        
    try {
      era.complete();
      // Save snapshots for all versions in this era
      for (int i = 0; i < era.maxVersionOffset(); i++)
        saveSnapshot(era.getVersion(i),floc);
      era.store(floc);      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /** Save a version of this project
   *  using snapshot
   */
  public void saveVersionBySnapshot(Version v, FileLocator floc) 
    throws IOException {
    // if the version v is already saved in an era, done.
    if (isInASavedEra(v)) return;
    // if it is in an era but not saved, then save it and make sure all ancestor
    // eras are saved.
    Era era = v.getEra();
    if (era != null && (!era.isStored())) {
      saveEraAndSnapshot(era,floc);
      return;
    }
    // Here, era is null, create a new era
    Version r = v.parent();
    // find the root of this editing session
    Version alpha = Version.getInitialVersion();
    while (r != alpha) {
      if (r.getEra() != null) break;
      r = r.parent();
    }
    // if r.getEra() has not been saved
    if (r != alpha && (!r.getEra().isStored())) 
      saveEraAndSnapshot(r.getEra(),floc);
    
    // create and save the era
    try {
      Era e = new Era(r, new Version[]{v});
      e.complete();
      // Save all the snapshots of this era
      for (int i = 0; i < e.maxVersionOffset(); i++)
        saveSnapshot(e.getVersion(i),floc);
      e.store(floc);
    } catch (OverlappingEraException ex) {
        System.out.println("Overlapping eras!");
        ex.printStackTrace();
    }
  }

  /** Load an era
   */
  public static Era loadEraFromName(String era_name, FileLocator floc) 
          throws IOException {
      // Era.ensureLoaded();
      UniqueID id = UniqueID.parseUniqueID(era_name);
      Era era = Era.loadEra(id, floc);
      era.describe(System.out);
      return era;
  }
  
  /** Load versioned chunk delta for this project 
   *  in this era and all above eras 
   */
  public void loadDeltaForEras(Era era, FileLocator floc) 
    throws IOException {
    if (era == null) return;
    VersionedChunk ourChunk = VersionedChunk.get(components,configBundle);
    if (era.isLoaded(ourChunk)) return;
    loadDeltaForEras(era.getParentEra(),floc);
    if (era.isLoaded(ourChunk)) return; // may be unchanged since parent
    loadDelta(era,floc);
  }
      
  /** Load deltas for components of this project 
   *  in this era and all above eras 
   */
  private void loadComponentDeltaForEras(Era era, FileLocator floc) 
    throws IOException {
    // TODO: VICs need to be loaded in the initial era.
    if (era == Era.getInitialEra()) return;
    else {
      Era parent_era = era.getParentEra();
      loadComponentDeltaForEras(parent_era,floc);
    }
    // TODO: maybe we should use Era.isLoaded
    if (comp_delta_loaded_eras.contains(era) == false) {
      // System.out.println("Loading components' deltas for the era " + era.getID());
      loadComponentDelta(era,era.maxVersionOffset(),floc);
      comp_delta_loaded_eras.addElement(era);
    }
  }
  
  /** Load a version of this project
   */
  public Version loadVersionByDelta(String version_name, FileLocator floc) 
      throws IOException {
    fileLocator = floc;
    Version v = lookupVersion(version_name);  
    Era era = v.getEra();
    int offset = v.getEraOffset();
    
    // Load delta for this config given this era and ALL above eras
    loadDeltaForEras(era,floc);
        
    // Load all components' deltas for this era and ALL above eras
    Era parent_era = era.getParentEra();
    loadComponentDeltaForEras(parent_era,floc);
    loadComponentDelta(era,offset,floc);    
    //Fix: Bug
    if (comp_delta_loaded_eras.contains(era) == false)
        comp_delta_loaded_eras.addElement(era);
    return v; 
  }
    
    
  /** Load a version of this project by Snapshot
   */
  public Version loadVersionBySnapshot(String version_name, FileLocator floc) 
      throws IOException {
    // Get the era and offset name from name2eraoffsetTable
    fileLocator = floc;
    Version v = lookupVersion(version_name);
    // load snapshot for this config given this era
    loadSnapshot(v,floc);    
    return v; 
  }
  
  /**
   * Demand load the structure of the project (not the individual components)
   * at the given version.  The current policy is to load deltas.  We may wish
   * to load snapshosts when delats are unavailable.  Currently it throw a run-time
   * exception if loading isn't possible.  Not sure if this is a good idea or not.
   * @param v version at which we want to make sure the thing is loaded.
   * @param floc place to look for files.
   */
  public void ensureLoaded(Version v, FileLocator floc) {
    Era e = v.getEra();
    if (e == null) {
      for (Version v1 = v.parent(); e == null; v1 = v1.parent()) {
        e = v1.getEra();
      }
    }
    try {
      loadDeltaForEras(e,floc);
    } catch (IOException e1) {
      e1.printStackTrace();
      LOG.severe("Cannot load project at " + v);
      throw new FluidRuntimeException("unloadable: "+e1);
    }
  }
  
  public static void ensureLoaded() {
    VersionedRegion.ensureLoaded();
    Era.ensureLoaded();
    VersionedChunk.ensureLoaded();
    Bundle.ensureLoaded();
    Component.ensureLoaded();
  }
}
