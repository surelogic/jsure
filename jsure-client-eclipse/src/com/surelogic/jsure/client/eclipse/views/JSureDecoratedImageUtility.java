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

import java.util.EnumSet;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

/**
 * An image descriptor for JSure verification results. It decorates an image
 * based upon one of several flags.
 * 
 * @see org.eclipse.jdt.ui.JavaElementImageDescriptor
 */
@Utility
public final class JSureDecoratedImageUtility {

  public enum Flag {
    ASSUME(CommonImages.DECR_ASSUME), CONSISTENT(CommonImages.DECR_CONSISTENT), DELTA(CommonImages.DECR_DELTA), HINT_INFO(
        CommonImages.DECR_INFO), HINT_WARNING(CommonImages.DECR_WARNING), INCONSISTENT(CommonImages.DECR_INCONSISTENT), REDDOT(
        CommonImages.DECR_REDDOT), TRUSTED(CommonImages.DECR_TRUSTED), VIRTUAL(CommonImages.DECR_VIRTUAL);

    Flag(String imageName) {
      ImageDescriptor id = SLImages.getImageDescriptor(imageName);
      if (id == null)
        throw new IllegalArgumentException("No common image " + imageName);
      f_imageDescriptor = id;
    }

    @NonNull
    final ImageDescriptor f_imageDescriptor;

    @NonNull
    ImageDescriptor getImageDescriptor() {
      return f_imageDescriptor;
    }
  };

  public static final Point SIZE = new Point(22, 16);

  /**
   * Returns a decorated image. The image is cached to avoid running out of SWT
   * Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image used as the base image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @param size
   *          the size of the resulting image. Set to {@link #JSURE_ICONSIZE} if
   *          {@code null}.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getImage(@NonNull final Image baseImage, @NonNull final EnumSet<Flag> flags, @Nullable Point size) {
    if (size == null)
      size = SIZE;
    return SLImages.getDecoratedImage(baseImage, getOverlaysArray(flags), size);
  }

  /**
   * Returns a decorated image of {@link #SIZE}. The image is cached to avoid
   * running out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image used as the base image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getImage(@NonNull final Image baseImage, @NonNull final EnumSet<Flag> flags) {
    return getImage(baseImage, flags, SIZE);
  }

  /**
   * Returns a decorated image. The image is cached to avoid running out of SWT
   * Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image descriptor used as the base image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @param size
   *          the size of the resulting image. Set to {@link #JSURE_ICONSIZE} if
   *          {@code null}.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getImage(@NonNull final ImageDescriptor baseImage, @NonNull final EnumSet<Flag> flags, @Nullable Point size) {
    final Image base = baseImage.createImage();
    final Image result = getImage(base, flags, size);
    base.dispose();
    return result;
  }

  /**
   * Returns a decorated image of {@link #SIZE}. The image is cached to avoid
   * running out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image descriptor used as the base image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getImage(@NonNull final ImageDescriptor baseImage, @NonNull final EnumSet<Flag> flags) {
    return getImage(baseImage, flags, SIZE);
  }

  /**
   * Returns a decorated image. The image is cached to avoid running out of SWT
   * Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImageName
   *          an image name from {@link CommonImages} to be used as the base
   *          image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @param size
   *          the size of the resulting image. Set to {@link #JSURE_ICONSIZE} if
   *          {@code null}.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getImage(@NonNull final String baseImageName, @NonNull final EnumSet<Flag> flags, @Nullable Point size) {
    return getImage(SLImages.getImage(baseImageName), flags, size);
  }

  /**
   * Returns a decorated image of {@link #SIZE}. The image is cached to avoid
   * running out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImageName
   *          an image name from {@link CommonImages} to be used as the base
   *          image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getImage(@NonNull final String baseImageName, @NonNull final EnumSet<Flag> flags) {
    return getImage(baseImageName, flags, SIZE);
  }

  /**
   * Returns a grayscale decorated image. The image is cached to avoid running
   * out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image used as the base image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @param size
   *          the size of the resulting image. Set to {@link #JSURE_ICONSIZE} if
   *          {@code null}.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getGrayscaleImage(@NonNull final Image baseImage, @NonNull final EnumSet<Flag> flags, @Nullable Point size) {
    if (size == null)
      size = SIZE;
    return SLImages.getDecoratedGrayscaleImage(baseImage, getOverlaysArray(flags), size);
  }

  /**
   * Returns a grayscale decorated image of {@link #SIZE}. The image is cached
   * to avoid running out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image used as the base image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getGrayscaleImage(@NonNull final Image baseImage, @NonNull final EnumSet<Flag> flags) {
    return getImage(baseImage, flags, SIZE);
  }

  /**
   * Returns a grayscale decorated image. The image is cached to avoid running
   * out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image descriptor used as the base image
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @param size
   *          the size of the resulting image. Set to {@link #JSURE_ICONSIZE} if
   *          {@code null}.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getGrayscaleImage(@NonNull final ImageDescriptor baseImage, @NonNull final EnumSet<Flag> flags,
      @Nullable Point size) {
    final Image base = baseImage.createImage();
    final Image result = getGrayscaleImage(base, flags, size);
    base.dispose();
    return result;
  }

  /**
   * Returns a grayscale decorated image of {@link #SIZE}. The image is cached
   * to avoid running out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImage
   *          an image descriptor used as the base image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getGrayscaleImage(@NonNull final ImageDescriptor baseImage, @NonNull final EnumSet<Flag> flags) {
    return getGrayscaleImage(baseImage, flags, SIZE);
  }

  /**
   * Returns a grayscale decorated image. The image is cached to avoid running
   * out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImageName
   *          an image name from {@link CommonImages} to be used as the base
   *          image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @param size
   *          the size of the resulting image. Set to {@link #JSURE_ICONSIZE} if
   *          {@code null}.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getGrayscaleImage(@NonNull final String baseImageName, @NonNull final EnumSet<Flag> flags,
      @Nullable Point size) {
    return getGrayscaleImage(SLImages.getImage(baseImageName), flags, size);
  }

  /**
   * Returns a grayscale decorated image of {@link #SIZE}. The image is cached
   * to avoid running out of SWT Image objects.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this
   * method.
   * 
   * @param baseImageName
   *          an image name from {@link CommonImages} to be used as the base
   *          image.
   * @param flags
   *          flags indicating which adornments are to be rendered from
   *          {@link Flag}, or use {@link EnumSet#noneOf(Class)} for none.
   * @return an image that is managed by this utility, please do not call
   *         {@link Image#dispose()} on it.
   */
  public static Image getGrayscaleImage(@NonNull final String baseImageName, @NonNull final EnumSet<Flag> flags) {
    return getGrayscaleImage(baseImageName, flags, SIZE);
  }

  @NonNull
  private static ImageDescriptor[] getOverlaysArray(@NonNull final EnumSet<Flag> flags) {
    final ImageDescriptor[] overlaysArray = { getTopLeft(flags), getTopRight(flags), getBottomLeft(flags), getBottomRight(flags),
        null };
    return overlaysArray;
  }

  @Nullable
  private static ImageDescriptor getTopRight(@NonNull final EnumSet<Flag> flags) {
    if (flags.contains(Flag.ASSUME)) {
      return Flag.ASSUME.getImageDescriptor();
    } else if (flags.contains(Flag.VIRTUAL)) {
      return Flag.VIRTUAL.getImageDescriptor();
    } else if (flags.contains(Flag.TRUSTED)) {
      return Flag.TRUSTED.getImageDescriptor();
    }
    return null;
  }

  @Nullable
  private static ImageDescriptor getBottomRight(@NonNull final EnumSet<Flag> flags) {
    if (flags.contains(Flag.DELTA)) {
      return Flag.DELTA.getImageDescriptor();
    } else if (flags.contains(Flag.HINT_WARNING)) {
      return Flag.HINT_WARNING.getImageDescriptor();
    } else if (flags.contains(Flag.HINT_INFO)) {
      return Flag.HINT_INFO.getImageDescriptor();
    }
    return null;
  }

  @Nullable
  private static ImageDescriptor getTopLeft(@NonNull final EnumSet<Flag> flags) {
    if (flags.contains(Flag.REDDOT)) {
      return Flag.REDDOT.getImageDescriptor();
    }
    return null;
  }

  @Nullable
  private static ImageDescriptor getBottomLeft(@NonNull final EnumSet<Flag> flags) {
    if (flags.contains(Flag.CONSISTENT)) {
      return Flag.CONSISTENT.getImageDescriptor();
    } else if (flags.contains(Flag.INCONSISTENT)) {
      return Flag.INCONSISTENT.getImageDescriptor();
    }
    return null;
  }
}