package com.surelogic.persistence;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.irfree.XmlCreator;

public class JSureResultsXMLCreator extends XmlCreator {
  public JSureResultsXMLCreator(OutputStream out) throws IOException {
    super(out);
  }

  public void reportResults(CUDrop cud, List<IAnalysisResult> results) {
    try {
      b.start(PersistenceConstants.COMP_UNIT);
      b.addAttribute("path", cud.getJavaOSFileName());
      for (IAnalysisResult r : results) {
        r.outputToXML(this, b);
      }
      b.end();
    } finally {
      flushBuffer();
    }
  }
}
