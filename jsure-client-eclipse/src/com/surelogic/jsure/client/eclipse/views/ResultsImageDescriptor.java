/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.surelogic.jsure.client.eclipse.views;

import static com.surelogic.common.jsure.xml.CoE_Constants.ASSUME;
import static com.surelogic.common.jsure.xml.CoE_Constants.CONSISTENT;
import static com.surelogic.common.jsure.xml.CoE_Constants.DELTA;
import static com.surelogic.common.jsure.xml.CoE_Constants.HINT_INFO;
import static com.surelogic.common.jsure.xml.CoE_Constants.HINT_WARNING;
import static com.surelogic.common.jsure.xml.CoE_Constants.INCONSISTENT;
import static com.surelogic.common.jsure.xml.CoE_Constants.REDDOT;
import static com.surelogic.common.jsure.xml.CoE_Constants.TRUSTED;
import static com.surelogic.common.jsure.xml.CoE_Constants.VIRTUAL;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;

/**
 * A AssuranceImageDescriptor consists of a base image and several adornments.
 * The adornments are computed according to the flags either passed during
 * creation or set via the method * <code>setAdornments</code>. This class
 * "cloned" from {@link org.eclipse.jdt.ui.JavaElementImageDescriptor} within
 * the Eclipse JDT source code. It has been modified to output assurance
 * decorators and all dependencies to internal Eclipse packages was removed.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @see org.eclipse.jdt.ui.JavaElementImageDescriptor
 */
public class ResultsImageDescriptor extends CompositeImageDescriptor {

  public static final Point JSURE_ICONSIZE = new Point(22, 16);

  public static final ImageDescriptor DESC_ASSUME_DECR = SLImages.getImageDescriptor(CommonImages.IMG_ASSUME_DECR);

  public static final ImageDescriptor DESC_CONSISTENT_DECR = SLImages.getImageDescriptor(CommonImages.IMG_CONSISTENT_DECR);

  public static final ImageDescriptor DESC_INCONSISTENT_DECR = SLImages.getImageDescriptor(CommonImages.IMG_INCONSISTENT_DECR);

  public static final ImageDescriptor DESC_REDDOT_DECR = SLImages.getImageDescriptor(CommonImages.IMG_REDDOT_DECR);

  public static final ImageDescriptor DESC_TRUSTED_DECR = SLImages.getImageDescriptor(CommonImages.IMG_TRUSTED_DECR);

  public static final ImageDescriptor DESC_VIRTUAL_DECR = SLImages.getImageDescriptor(CommonImages.IMG_VIRTUAL_DECR);

  public static final ImageDescriptor DESC_WARNING_DECR = SLImages.getImageDescriptor(CommonImages.IMG_WARNING_DECR);

  public static final ImageDescriptor DESC_INFO_DECR = SLImages.getImageDescriptor(CommonImages.IMG_INFO_DECR);

  public static final ImageDescriptor DESC_DELTA_DECR = SLImages.getImageDescriptor(CommonImages.IMG_DELTA_DECR);

  private final ImageDescriptor fBaseImage;

  private final int fFlags;

  private final Point fSize;

  /**
   * ImageDescriptor:fBaseImage -> (MAP fFlags+size -> Image)
   */
  private static final Map<ImageDescriptor, Map<String, Image>> imageCache = new HashMap<ImageDescriptor, Map<String, Image>>();

  /**
   * ImageDescriptor:fBaseImage -> (MAP fFlags+size -> Image)
   */
  private static final Map<ImageDescriptor, Map<String, Image>> imageGrayCache = new HashMap<ImageDescriptor, Map<String, Image>>();

  private static boolean f_needToRegDisposeExec = true;

  private static void disposeExec() {
    if (f_needToRegDisposeExec) {
      f_needToRegDisposeExec = false;
      Display.getCurrent().disposeExec(new Runnable() {
        public void run() {
          disposeCacheHelper(imageCache);
          disposeCacheHelper(imageGrayCache);
          imageCache.clear();
          imageGrayCache.clear();
        }
      });
    }
  }

  private static void disposeCacheHelper(Map<ImageDescriptor, Map<String, Image>> cache) {
    if (cache == null)
      return;

    for (Map<String, Image> m : cache.values())
      disposeCacheMapHelper(m);
  }

  private static void disposeCacheMapHelper(Map<String, Image> m) {
    if (m == null)
      return;

    for (Image image : m.values()) {
      image.dispose();
    }
  }

  /**
   * Creates a new JavaElementImageDescriptor.
   * 
   * @param baseImage
   *          an image descriptor used as the base image
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link CoE_Constants}. Valid values are:
   *          {@link CoE_Constants#CONSISTENT},
   *          {@link CoE_Constants#INCONSISTENT}, {@link CoE_Constants#REDDOT},
   *          {@link CoE_Constants#ASSUME}, {@link CoE_Constants#TRUSTED},
   *          {@link CoE_Constants#VIRTUAL} or any combination of those.
   * @param size
   *          the size of the resulting image
   * @see #setAdornments(int)
   */
  public ResultsImageDescriptor(ImageDescriptor baseImage, int flags, Point size) {
    fBaseImage = baseImage;
    assert fBaseImage != null;
    fFlags = flags;
    assert fFlags >= 0;
    fSize = size;
    assert fSize != null;
  }

  /**
   * Creates a new JavaElementImageDescriptor.
   * 
   * @param imageName
   *          an image name from {@link CommonImages}.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link CoE_Constants}. Valid values are:
   *          {@link CoE_Constants#CONSISTENT},
   *          {@link CoE_Constants#INCONSISTENT}, {@link CoE_Constants#REDDOT},
   *          {@link CoE_Constants#ASSUME}, {@link CoE_Constants#TRUSTED},
   *          {@link CoE_Constants#VIRTUAL} or any combination of those.
   * @param size
   *          the size of the resulting image
   * @see #setAdornments(int)
   */
  public ResultsImageDescriptor(String imageName, int flags, Point size) {
    fBaseImage = SLImages.getImageDescriptor(imageName);
    assert fBaseImage != null;
    fFlags = flags;
    assert fFlags >= 0;
    fSize = size;
    assert fSize != null;
  }

  /**
   * Returns the image, should always be used to avoid running out of SWT Image
   * objects.
   * 
   * @return an image
   */
  public Image getCachedImage() {
    Image result;
    final String flagsKey = getCacheKey();
    Map<String, Image> flagMap = imageCache.get(fBaseImage);
    if (flagMap != null) {
      result = flagMap.get(flagsKey);
      if (result == null) {
        // add image to the existing base image cache
        result = this.createImage();
        flagMap.put(flagsKey, result);
      }
    } else {
      // add image to both cache maps
      result = this.createImage();
      flagMap = new HashMap<String, Image>();
      flagMap.put(flagsKey, result);
      imageCache.put(fBaseImage, flagMap);
      disposeExec();
    }
    return result;
  }

  /**
   * Returns the image in grayscale, should always be used to avoid running out
   * of SWT Image objects.
   * 
   * @return a grayscale image
   */
  public Image getCachedGrayImage() {
    Image result;
    final String flagsKey = getCacheKey();
    Map<String, Image> flagMap = imageGrayCache.get(fBaseImage);
    if (flagMap != null) {
      result = flagMap.get(flagsKey);
      if (result == null) {
        // add image to the existing base image cache
        result = toGray(getCachedImage());
        flagMap.put(flagsKey, result);
      }
    } else {
      // add image to both cache maps
      result = toGray(getCachedImage());
      flagMap = new HashMap<String, Image>();
      flagMap.put(flagsKey, result);
      imageGrayCache.put(fBaseImage, flagMap);
      disposeExec();
    }
    return result;
  }

  private Image toGray(Image i) {
    return new Image(i.getDevice(), i, SWT.IMAGE_GRAY);
  }

  /**
   * Constructs a cache key for this descriptor. This includes the decorator
   * flags in hex followed by the size of the requested image.
   * <p>
   * Examples: <tt>84(22,16)</tt>, <tt>0(22,16)</tt>, <tt>2(16,16)</tt>
   * 
   * @return a cache key;
   */
  @NonNull
  private String getCacheKey() {
    final String result = Integer.toHexString(fFlags) + "(" + fSize.x + "," + fSize.y + ")";
    return result;
  }

  /**
   * Returns the current adornments.
   * 
   * @return the current adornments
   */
  public int getAdronments() {
    return fFlags;
  }

  /**
   * Returns the size of the image created by calling <code>createImage()</code>
   * .
   * 
   * @return the size of the image created by calling <code>createImage()</code>
   * @see ImageDescriptor#createImage()
   */
  public Point getImageSize() {
    return new Point(fSize.x, fSize.y);
  }

  /**
   * Method declared in CompositeImageDescriptor
   */
  @Override
  protected Point getSize() {
    return fSize;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || !ResultsImageDescriptor.class.equals(object.getClass()))
      return false;

    ResultsImageDescriptor other = (ResultsImageDescriptor) object;
    return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize.equals(other.fSize));
  }

  @Override
  public int hashCode() {
    return fBaseImage.hashCode() | fFlags | fSize.hashCode();
  }

  /**
   * Method declared in CompositeImageDescriptor
   */
  @Override
  protected void drawCompositeImage(int width, int height) {
    ImageData bg = getImageData(fBaseImage);

    drawImage(bg, 3, 0);

    drawTopRight();
    drawTopLeft();
    drawBottomRight();
    drawBottomLeft();
  }

  private ImageData getImageData(ImageDescriptor descriptor) {
    ImageData data = descriptor.getImageData(); // see bug 51965:
    // getImageData
    // can return null
    if (data == null) {
      data = DEFAULT_IMAGE_DATA;
      System.out.println("Image data not available: " + descriptor.toString());
    }
    return data;
  }

  private void drawTopRight() {
    int x = getSize().x;
    if ((fFlags & ASSUME) != 0) {
      ImageData data = getImageData(DESC_ASSUME_DECR);
      x -= data.width;
      drawImage(data, x, 0);
    } else if ((fFlags & VIRTUAL) != 0) {
      ImageData data = getImageData(DESC_VIRTUAL_DECR);
      x -= data.width;
      drawImage(data, x, 0);
    } else if ((fFlags & TRUSTED) != 0) {
      ImageData data = getImageData(DESC_TRUSTED_DECR);
      x -= data.width;
      drawImage(data, x, 0);
    }
  }

  private void drawBottomRight() {
    Point size = getSize();
    if ((fFlags & DELTA) != 0) {
      ImageData data = getImageData(DESC_DELTA_DECR);
      drawImage(data, size.x - data.width, size.y - data.height);
    } else if ((fFlags & HINT_WARNING) != 0) {
      ImageData data = getImageData(DESC_WARNING_DECR);
      drawImage(data, size.x - data.width, size.y - data.height);
    } else if ((fFlags & HINT_INFO) != 0) {
      ImageData data = getImageData(DESC_INFO_DECR);
      drawImage(data, size.x - data.width, size.y - data.height);
    }
  }

  private void drawTopLeft() {
    if ((fFlags & REDDOT) != 0) {
      ImageData data = getImageData(DESC_REDDOT_DECR);
      drawImage(data, 0, 0);
    }
  }

  private void drawBottomLeft() {
    Point size = getSize();
    int x = 0;
    if ((fFlags & CONSISTENT) != 0) {
      ImageData data = getImageData(DESC_CONSISTENT_DECR);
      drawImage(data, x, size.y - data.height);
      x += data.width / 2;
    }
    if ((fFlags & INCONSISTENT) != 0) {
      ImageData data = getImageData(DESC_INCONSISTENT_DECR);
      drawImage(data, x, size.y - data.height);
    }
  }

  /*
   * Convenience methods for getting images
   */

  /**
   * Gets a cached image with an optional conflict (warning) decorator.
   * 
   * @param symbolicName
   *          a name from {@link CommonImages}.
   * @param showWariningDecorator
   *          {@code true} if a warning decorator should be shown, {@code false}
   *          otherwise.
   * @return an image that is carefully cached. The image should <i>not</i> be
   *         disposed by the calling code.
   */
  public static final Image getCachedImage(String symbolicName, Decorator d) {
    return getCachedImage(SLImages.getImageDescriptor(symbolicName), d);
  }

  /**
   * Gets a cached image with an optional conflict (warning) decorator.
   * 
   * @param imageDescriptor
   *          an image descriptor.
   * @param showWarningDecorator
   *          {@code true} if a warning decorator should be shown, {@code false}
   *          otherwise.
   * @return an image that is carefully cached. The image should <i>not</i> be
   *         disposed by the calling code.
   */
  public static final Image getCachedImage(ImageDescriptor imageDescriptor, Decorator d) {
    ResultsImageDescriptor rid = new ResultsImageDescriptor(imageDescriptor, d.flag, new Point(22, 16));
    return rid.getCachedImage();
  }

  public static enum Decorator {
    NONE(CoE_Constants.NONE), WARNING(CoE_Constants.HINT_WARNING), RED_DOT(CoE_Constants.REDDOT);

    final int flag;

    Decorator(int flag) {
      this.flag = flag;
    }
  }
}