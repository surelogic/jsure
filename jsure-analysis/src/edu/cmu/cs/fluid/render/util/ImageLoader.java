package edu.cmu.cs.fluid.render.util;

import java.awt.*;
import java.net.URL;

import javax.swing.ImageIcon;

public class ImageLoader {

  MediaTracker mt;
  int imagecount;

  public ImageLoader() {
    mt = null;
    imagecount = 0;
  }

  public ImageLoader(Component comp) {
    mt = new MediaTracker(comp);
    imagecount = 0;
  }

  public ImageIcon loadImageIcon(String imageFileName) {
    return new ImageIcon(loadImage(imageFileName));
  }

  public ImageIcon loadImageIcon(Image image) {
    return new ImageIcon(image);
  }

  public Image loadImage(String imageFileName) {
    // get the actual file
    URL imageURL = getClass().getResource(imageFileName);
    Toolkit tk = Toolkit.getDefaultToolkit();

    Image img = null;

    // try to get the image
    try {
      img =
        tk.createImage((java.awt.image.ImageProducer) imageURL.getContent());
    } catch (java.io.IOException ex) {
      System.out.println("Image not loaded: " + imageFileName + "  " + ex);
      ex.printStackTrace();
    }
    // add the image to the tracker
    if (img == null)
      System.out.println(imageFileName + " Does Not Exist");
    else {

      if (mt != null)
        mt.addImage(img, imagecount);
      imagecount++;
    }
    return img;
  }

  public Image loadSimpleImage(String imageFileName) {
    URL imageURL = getClass().getResource(imageFileName);
    Toolkit tk = Toolkit.getDefaultToolkit();
    Image img = null;

    try {
      img =
        tk.createImage((java.awt.image.ImageProducer) imageURL.getContent());
    } catch (java.io.IOException ex) {
      System.out.println(ex);
    }

    if (img == null)
      System.out.println(imageFileName + " Does Not Exist");

    return img;
  }

  public void waitForAll() throws java.lang.InterruptedException {
    if (mt != null)
      mt.waitForAll();
  }
}
