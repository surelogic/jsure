package com.surelogic.jsure.client.eclipse.handlers;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.surelogic.common.ui.dialogs.DialogUtility;
import com.surelogic.jsure.client.eclipse.LibResources;

public class SaveHtmlDocsHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final DialogUtility.ZipResourceFactory source = new DialogUtility.ZipResourceFactory() {
      public InputStream getInputStream() throws IOException {
        return LibResources.getStreamFor(LibResources.HTML_DOCS_ZIP_PATHNAME);
      }
    };
    DialogUtility.copyZipResourceToUsersDiskDialogInteractionHelper(source, LibResources.HTML_DOCS_ZIP, LibResources.HTML_DOCS_ZIP,
        "jsure.eclipse.dialog.html-docs");
    return null;
  }
}
