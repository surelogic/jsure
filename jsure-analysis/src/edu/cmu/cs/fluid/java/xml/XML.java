/*
 * Created on Mar 23, 2005
 */
package edu.cmu.cs.fluid.java.xml;

/**
 * @author Edwin
 */
public class XML {
  private static final XML prototype = new XML();
  
  public static XML getDefault() { return prototype; }

  private boolean processing = false;
  
  public boolean processingXML() {
    return processing;
  }
  
  public void setProcessingXML(boolean busy) {
    processing = busy;
  }
  
  private boolean processingSrc = false;
  
  public boolean processingXMLInSrc() {
    return processingSrc;
  }
  
  public void setProcessingXMLInSrc(boolean busy) {
    processingSrc = busy;
  }
}
