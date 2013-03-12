package com.surelogic.persistence;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xml.sax.Attributes;

import com.surelogic.analysis.IIRProjects;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.irfree.IXmlResultListener;
import com.surelogic.dropsea.irfree.NestedJSureXmlReader;

public abstract class AbstractJSureResultsXMLReader<T> extends NestedJSureXmlReader implements IXmlResultListener,
    PersistenceConstants {

  public AbstractJSureResultsXMLReader(IIRProjects p) {
    projects = p;
  }

  private final IIRProjects projects;

  public IIRProjects getProjects() {
    return projects;
  }

  @Override
  protected final String checkForRoot(String name, Attributes attributes) {
    if (COMP_UNIT.equals(name)) {
      if (attributes == null) {
        return "";
      }
      return attributes.getValue("path");
    }
    return null;
  }

  @Override
  public final Entity makeEntity(String name, Attributes a) {
    return new Entity(name, a);
  }

  @Override
  public final void start(String uid, String project) {
    // System.out.println("uid = "+uid);
  }

  @Override
  public void notify(Entity e) {
    if (!RESULT.equals(e.getName())) {
      throw new IllegalStateException("Unexpected top-level entity: " + e.getName());
    }
    if (e.numRefs() < 2) {
      throw new IllegalStateException("Missing about/source-ref: " + e.getName());
    }
    // I cannot build each result drop here
    boolean checkedPromises = false;
    T result = createResult();

    for (Entity nested : e.getReferences()) {
      if (ABOUT_REF.equals(nested.getName())) {
        PromiseDrop<?> pd = handlePromiseRef(nested);
        handleAboutRef(result, nested, pd);
      } else if ("source-ref".equals(nested.getName())) {
        handleSourceRef(result, nested);
      } else if (AND_REF.equals(nested.getName())) {
        PromiseDrop<?> pd = handlePromiseRef(nested);
        handleAndRef(result, nested, pd);
        checkedPromises = true;
      }
    }
    finishResult(result, e, checkedPromises);
  }

  protected abstract T createResult();

  protected abstract void handleSourceRef(T result, Entity srcRef);

  protected abstract PromiseDrop<?> handlePromiseRef(Entity pr);

  protected abstract void handleAboutRef(T result, Entity pe, PromiseDrop<?> pd);

  protected abstract void handleAndRef(T result, Entity pe, PromiseDrop<?> pd);

  protected abstract void finishResult(T result, Entity e, boolean checkedPromises);

  @Override
  public final void done() {
    // Nothing to do here?
  }

  public void readXMLArchive(final File results) throws Exception {
    if (!results.exists()) {
      System.out.println("No results to read");
      return;
    }
    ZipFile f = new ZipFile(results);
    Enumeration<? extends ZipEntry> e = f.entries();
    while (e.hasMoreElements()) {
      ZipEntry ze = e.nextElement();
      InputStream in = f.getInputStream(ze);
      read(in);
      finishedZipEntry(ze);
    }
  }

  protected void finishedZipEntry(ZipEntry ze) {
    // Nothing to do
  }
}
