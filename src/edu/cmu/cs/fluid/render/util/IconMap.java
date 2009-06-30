// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/render/util/IconMap.java,v 1.6 2005/06/30 21:45:41 chance Exp $
package edu.cmu.cs.fluid.render.util;

import java.awt.Canvas;
import java.awt.Image;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class IconMap extends HashMap<String,Icon> {
  static void debug(String message) {
    System.out.println(message);
  }

  private static final String[] names =
    {
      "accessed10.gif",
      "added10.gif",
      "default10.gif",
      "deleted10.gif",
      "ellipsis10.gif",
      "fileicon10.gif",
      "folder10.gif",
      "minus10.gif",
      "modified10.gif",
      "phantom10.gif",
      "plus10.gif",
      "renamed10.gif",
      "arrow.gif",
      "assets.gif",
      "assetsmall.gif",
      "base.gif",
      "clients.gif",
      "clientsmall.gif",
      "closedf.gif",
      "computer.gif",
      "current.gif",
      "dash.gif",
      "default.gif",
      "deletedfileicon.gif",
      "down.gif",
      "ellipsis.gif",
      "eyeclosed.gif",
      "eyeopen.gif",
      "fileicon.gif",
      "line.gif",
      "mailicon.gif",
      "minus.gif",
      "newfileicon.gif",
      "openf.gif",
      "paintclosed.gif",
      "paintopen.gif",
      "plus.gif",
      "plusarrow.gif",
      "reddot.gif",
      "star_sma.gif",
      "textbox.gif",
      "up.gif",
      "version.gif",
      "yball.gif",
      };

  public static final IconMap prototype = new IconMap();
  private static final ImageLoader images = new ImageLoader(new Canvas());

  static {
    // load default images
    for (int i = 0; i < names.length; i++) {
      Image image = images.loadImage(names[i]);
      if (image != null) {
        Icon icon = new ImageIcon(image);
        prototype.put(names[i], icon);
      }
    }
    prototype.getIcon(names[0]);
  }

  public Icon getIcon(final String name) {
    Icon icon = get(name);
    if (icon == null) {
      Image image = images.loadImage(name);
      if (image != null) {
        icon = new ImageIcon(image);
        put(name, icon);
      }

      // starts loading all images tracked
      try {
        images.waitForAll();
      } catch (InterruptedException e) {
        debug("exception caught when loading images: " + e);
        e.printStackTrace();
      }
    }
    return icon;
  }
}
