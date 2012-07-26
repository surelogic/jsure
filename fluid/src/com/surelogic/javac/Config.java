package com.surelogic.javac;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.tool.ToolProperties;
import com.surelogic.common.xml.Entities;
import com.surelogic.javac.persistence.JSureProjectsXMLCreator;
import com.surelogic.javac.persistence.PersistenceConstants;

import edu.cmu.cs.fluid.util.*;

public class Config extends AbstractClassPathEntry {
	protected static final boolean followRefs = false;
	public static final String SOURCE_LEVEL = "sourceLevel";
	public static final String AS_SOURCE = "asSource";
	
	private final String name;
	private final File location;
	private final Map<String,Object> options = new HashMap<String, Object>();
	private final List<IClassPathEntry> classPath = new ArrayList<IClassPathEntry>();

	/**
	 * Qualified name, and file location
	 */
	private final List<JavaSourceFile> files = new ArrayList<JavaSourceFile>();
	/**
	 * Used for initializing the map (esp. for other projects)
	 */
	private final List<JavaSourceFile> origFiles = new ArrayList<JavaSourceFile>();
	private final List<File> removed = new ArrayList<File>();
	private final Set<String> pkgs = new HashSet<String>();
	
	private String run;
	private final Map<URI,JavaSourceFile> pathMapping = new HashMap<URI, JavaSourceFile>();
	private Config requiringConfig = null;
	private final boolean containsJavaLangObject;
	
	public Config(String name, File loc, boolean isExported, boolean hasJLO) {
		super(isExported);
		this.name = name;
		location = loc;
		containsJavaLangObject = hasJLO;
	}

	public void outputToXML(JSureProjectsXMLCreator creator, int indent, StringBuilder b) {
		// Just the reference to this
		Entities.start(PersistenceConstants.PROJECT, b, indent);
		creator.addAttribute("name", name);
		Entities.closeStart(b, true);
	}
	
	@Override
	public String toString() {
		return "Config: "+name;
	}
	
	public String getProject() {
		return name;
	}
	
	public File getLocation() {
		return location;
	}
	
	public void setOption(String key, Object value) {
		Object replaced = options.put(key, value);
		if (replaced != null && !replaced.equals(value)) {
			SLLogger.getLogger().warning(key+": replaced "+replaced);
		}
	}
	
	public int getIntOption(String key) {
		Integer i = (Integer) options.get(key);
		return i != null ? i : 0;
	}
	
	public boolean getBoolOption(String key) {
		Boolean i = (Boolean) options.get(key);
		return i != null ? i : false;
	}
	
	public String[] getListOption(String key) {
		String l = (String) options.get(key);
		if (l == null) {
			return ToolProperties.noStrings;
		}
		return l.split("[ ,]+");
	}
	
	public void setAsSource() {
		setOption(Config.AS_SOURCE, true);
	}
	
	public void outputOptionsToXML(JSureProjectsXMLCreator creator, int indent,	StringBuilder b) {
		for(Map.Entry<String,Object> e : options.entrySet()) {
			Entities.indent(b, indent);
			creator.addAttribute(e.getKey(), e.getValue().toString());
		}
	}
	
	public void addPackage(String pkg) {
		pkgs.add(pkg);
	}

	public void addToClassPath(IClassPathEntry e) {
		if (e == null) {
			return;
		}
		if (!classPath.contains(e)) {
			classPath.add(e);
			if (followRefs && e != this && e instanceof Config) {				
				Config c = (Config) e;
				c.requiringConfig = this;
			}
		} else if (e != this) {
			//System.out.println("Ignoring duplicate: "+e);
		}
	}
	
	public final void addJar(String path) {
		addJar(new File(path), true);
	}
	
	public final void addJar(File path, boolean isExported) {
		if (path == null) {
			return;
		}
		addToClassPath(new JarEntry(this, path, isExported));
	}
	
	public void addRemovedFile(File f) {
		removed.add(f);
	}
	
	public Iterable<File> getRemovedFiles() {
		return removed;
	}
	
	public void addFile(JavaSourceFile file) {
		files.add(file);
		origFiles.add(file);
	}
	
	protected void setFiles(Collection<JavaSourceFile> newSources) {     
		mapFiles(newSources, files);
		mapFiles(newSources, origFiles);
	}
	
	private void mapFiles(Collection<JavaSourceFile> newSources, List<JavaSourceFile> oldSources) {
		if (oldSources.isEmpty()) {
			return;
		}
	    /*   
		//if ("java_common_dev".equals(getProject())) {
			System.out.println("setFiles for "+getProject()+": "+files.size()+" -> "+srcs.size());
		//}
        */
	    // Record qname mapping for old files
	    Map<String,JavaSourceFile> qname2File = new HashMap<String, JavaSourceFile>();
	    for(JavaSourceFile f : oldSources) {
	        /*
	    	if (f.toString().contains("pphtml")) {
	    		System.out.println("Looking at pphtml");
	    	}
            */
	    	qname2File.put(f.qname, f);
	    }
	    oldSources.clear();	    	    
	    
	    // Map new files back to old files
	    for(JavaSourceFile p : newSources) {
	    	JavaSourceFile last = qname2File.get(p.qname);
	    	if (last != null) {
	    		JavaSourceFile bumped = pathMapping.put(p.file.toURI(), last);
	    		if (bumped != null && !last.equals(bumped)) {
	    			System.out.println("Bumped "+bumped+" for "+last);
	    		}
	    		// Only add the ones that maps to old files
	    		oldSources.add(new JavaSourceFile(p.qname, p.file, last.relativePath, last.asBinary));
	    		//System.out.println("Mapped "+p.qname+" to "+last.relativePath);
	    	} else {
	    		// Omit those that don't match
	    		//System.out.println("Omitted: "+p.file);
	    		//throw new IllegalStateException("Couldn't find "+p.first()+" among copies");
	    	}
	    }
	}

    public void setRun(String name) {
        run = name;        
    }

    public String getRun() {
        return run;
    }
    
	public Iterable<String> getPackages() {
		return pkgs;
	}

	public Collection<JavaSourceFile> getFiles() {
		return files;
	}

    public JavaSourceFile mapPath(URI path) {
    	for(IClassPathEntry e : classPath) {
    		JavaSourceFile mapped;    	
    		if (e != this) {
    			mapped = e.mapPath(path);
    		} else {
    	        mapped = pathMapping.get(path);
    		}
    		if (mapped != null) {
    			/*
    			if (path.contains("jsure-message")) {
    				System.out.println("Mapped "+path+" back to "+mapped);
    			}
    			*/
    			return mapped;
    		}
    	}
    	return null;
    }

	public void init(JavacProject jp, JavacClassParser loader) throws IOException {
		if (loader.ensureInitialized(jp, this)) {
			return;
		}
		
		final boolean processSources = !followRefs || 
		                               (requiringConfig != null && requiringConfig.requiringConfig == null);
		// TODO which are actually exported, and which are not?
		if (processSources) {
			int num = 0;
			for(JavaSourceFile p : origFiles) {
				//System.out.println("Initializing "+p.qname+" for "+jp.getName());
				loader.mapFile(jp.getName(), p.qname, name, p);
				/* 
				if (getProject().contains("common")) {
					System.out.println("Mapping "+p.first()+" to "+p.second());
				}
				*/
				// Add packages to type env
				for(String pkg : pkgs) {
					jp.getTypeEnv().addPackage(pkg);
				}
				/*
				final String parentPath = p.second().getParent().replace(File.separatorChar, '.');
				String suffix = p.first();
				while (suffix.length() > 0) {
					if (parentPath.endsWith(suffix)) {
						tEnv.addPackage(suffix);		
						break;					
					}				
					int lastDot = suffix.lastIndexOf('.');
					if (lastDot >= 0) {
						suffix = suffix.substring(0, lastDot);
					} else {
						suffix = "";
					}
				}*/
				num++;
			}
			System.out.println("Done initializing "+getProject()+": "+num);
		}
		
		for(IClassPathEntry e : classPath) {
			if (e != this && (requiringConfig == null || e.isExported())) {
				if (e instanceof Config) {
					System.out.println(jp.getName()+": "+name+" -> "+((Config) e).name);
				}
				//if (followRefs || !(e instanceof Config)) {
					e.init(jp, loader);
				//}
			} 
		}
	}
	
	/**
	 * This must be overridden to complete things
	 */
	public void zipSources(File zipDir) throws IOException {
		if (followRefs) {
			for(IClassPathEntry e : classPath) {
				if (e != this) {
					e.zipSources(zipDir);
				}
			}
		}
	}

	public void copySources(File zipDir, File targetDir) throws IOException {
		if (followRefs) {
			for(IClassPathEntry e : classPath) {
				if (e != this) {
					e.copySources(zipDir, targetDir);
				}
			}
		}
	}

	public void relocateJars(File targetDir) throws IOException {
		if (followRefs) {
			for(IClassPathEntry e : classPath) {
				if (e != this) {
					e.relocateJars(targetDir);
				}
			}
		}
	}

	protected Config newConfig(String name, File location, boolean isExported, boolean hasJLO) {
		return new Config(name, location, isExported, hasJLO);
	}
	
	Config merge(Config delta) throws MergeException {
		if (!name.equals(delta.name) || isExported() != delta.isExported()) {
			throw new IllegalStateException("Names don't match: "+name+" != "+delta.name);
		}
		if (location == null) {
			if (delta.location != null) {
				throw new IllegalStateException("Locations don't match: "+delta.location+" != null");
			}
		} else if (!location.equals(delta.location)) {
			throw new IllegalStateException("Locations don't match: "+location+" != "+delta.location);
		}
		final Config merged = newConfig(name, location, isExported(), containsJavaLangObject());
		mergeClasspath(delta, merged);
		mergeFiles(delta, merged);
		merged.pkgs.addAll(this.pkgs);
		merged.pkgs.addAll(delta.pkgs);
		merged.run = delta.run;
		merged.pathMapping.putAll(this.pathMapping);
		merged.pathMapping.putAll(delta.pathMapping);
		merged.options.putAll(this.options);
		merged.options.putAll(delta.options);
		// requiringConfig should be set as we merge the classpath
		return merged;
	}

	private void mergeFiles(Config delta, Config merged) {
		final Map<String,JavaSourceFile> map = new HashMap<String,JavaSourceFile>();
		for(JavaSourceFile p : delta.files) {
			map.put(p.qname, p);
		}
		for(JavaSourceFile p : this.files) {
			JavaSourceFile dF = map.get(p.qname);
			if (dF != null) {
				merged.addFile(dF);
				map.remove(p.qname);
			} else { // Copy unchanged
				merged.addFile(p);
			}
		}
		// Add remaining
		for(JavaSourceFile p : map.values()) {
			merged.addFile(p);
		}
	}

	void checkForDiffs(Config delta) throws MergeException {
		// TODO how to deal with changes in the ordering?
		if (classPath.size() != delta.classPath.size()) {
			// TODO how to deal with a change of classpath?
			throw new MergeException("Different classpaths: "+classPath.size()+" != "+delta.classPath.size());
		}
		for(int i=0; i<classPath.size(); i++) {
			IClassPathEntry e1 = classPath.get(i);
			IClassPathEntry d2 = delta.classPath.get(i);
			if (e1.getClass() != d2.getClass()) {
				throw new MergeException("Different types: "+e1.getClass()+" != "+d2.getClass());
			}
			if (e1 instanceof Config) {
				if (e1 == this) {
					// Handle the special case of its own sources
					if (d2 != delta) {
						throw new MergeException("Different Configs in delta: "+d2+" != "+delta);
					}
				} else if (d2 == delta) {
					throw new MergeException("Different Configs in orig: "+e1+" != "+this);		
				}
			} else if (e1 instanceof JarEntry) {
				JarEntry j1 = (JarEntry) e1;
				if (!j1.equals(d2)) {
					throw new MergeException("Different jars: "+j1.getPath()+" != "+((JarEntry) d2).getPath());
				}
			} else if (e1 instanceof SrcEntry) {
				SrcEntry s1 = (SrcEntry) e1;
				if (!s1.equals(d2)) {
					throw new MergeException("Different jars: "+s1.getProjectRelativePath()+" != "+((SrcEntry) d2).getProjectRelativePath());
				}
			} else {
				throw new IllegalStateException("Unexpected entry: "+e1);
			}
		}
	}
	
	private void mergeClasspath(Config delta, final Config merged) throws MergeException {
		checkForDiffs(delta);
		for(int i=0; i<classPath.size(); i++) {
			IClassPathEntry e1 = classPath.get(i);
			IClassPathEntry d2 = delta.classPath.get(i);
			if (e1 instanceof Config) {
				if (e1 == this) {
					// Handle the special case of its own sources
					if (d2 == delta) {
						merged.addToClassPath(merged);
					}
				} else if (d2 != delta) {
					Config c1 = (Config) e1;
					Config c2 = (Config) d2;
					merged.addToClassPath(c1.merge(c2));					
				}
			} else if (e1 instanceof JarEntry) {
				JarEntry j1 = (JarEntry) e1;
				merged.addJar(j1.getPath(), j1.isExported());
			} else {
				throw new IllegalStateException("Unexpected entry: "+e1);
			}
		}
	}

	public Iterable<IClassPathEntry> getClassPath() {
		return classPath;
	}
	
	public Iteratable<Config> getDependencies() {
		return new FilterIterator<IClassPathEntry,Config>(classPath.iterator()) {
			@Override
			protected Object select(IClassPathEntry o) {
				if (o instanceof Config) {
					return (Config) o;
				}
				return IteratorUtil.noElement;
			}
		};
	}
	
	public void intersectFiles(Collection<JavaSourceFile> files2) {
		if (files2.size() > 0) {
			final Map<String,JavaSourceFile> orig = new HashMap<String, JavaSourceFile>();
			for(JavaSourceFile f : files) {
				orig.put(f.qname, f);
			}
			files.clear();
			
			for(JavaSourceFile f : files2) {
				JavaSourceFile o = orig.get(f.qname);
				if (o != null) {
					/*
					if (!f.file.equals(o.file)) {
						System.out.println("Files not the same");
					}
					System.out.println("Intersecting: "+f.relativePath);
					*/
					files.add(o);
				} else {
					//System.out.println("Omitting: "+f.relativePath);
				}
			}
		} else {
			System.out.println(getProject()+": No files to intersect, so clearing "+files.size());
			files.clear();
		}
	}

	public Iterable<Map.Entry<String, Object>> getOptions() {
		return options.entrySet();
	}

	public boolean containsJavaLangObject() {
		return containsJavaLangObject;
	}
}
