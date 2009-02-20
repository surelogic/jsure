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
package edu.cmu.cs.fluid.dcf.views.coe;

import static com.surelogic.xml.results.coe.CoE_Constants.ASSUME;
import static com.surelogic.xml.results.coe.CoE_Constants.CONSISTENT;
import static com.surelogic.xml.results.coe.CoE_Constants.INCONSISTENT;
import static com.surelogic.xml.results.coe.CoE_Constants.INFO;
import static com.surelogic.xml.results.coe.CoE_Constants.INFO_WARNING;
import static com.surelogic.xml.results.coe.CoE_Constants.REDDOT;
import static com.surelogic.xml.results.coe.CoE_Constants.TRUSTED;
import static com.surelogic.xml.results.coe.CoE_Constants.VIRTUAL;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;

/**
 * A AssuranceImageDescriptor consists of a base image and several adornments.
 * The adornments are computed according to the flags either passed during
 * creation or set via the method * <code>setAdornments</code>. This class
 * "cloned" from {@link org.eclipse.jdt.ui.JavaElementImageDescriptor} within
 * the Eclipse JDT source code. It has been modifed to output assurance
 * decrators and all dependencies to internal Eclipse packages was removed.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @see org.eclipse.jdt.ui.JavaElementImageDescriptor
 */
public class ResultsImageDescriptor extends CompositeImageDescriptor {
	public static final ImageDescriptor DESC_ASSUME_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("assume_decr.gif");

	public static final ImageDescriptor DESC_CONSISTENT_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("consistent_decr.gif");

	public static final ImageDescriptor DESC_INCONSISTENT_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("inconsistent_decr.gif");

	public static final ImageDescriptor DESC_REDDOT_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("reddot_decr.gif");

	public static final ImageDescriptor DESC_TRUSTED_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("trusted_decr.gif");

	public static final ImageDescriptor DESC_VIRTUAL_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("virtual_decr.gif");

	public static final ImageDescriptor DESC_WARNING_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("warning_decr.gif");

	public static final ImageDescriptor DESC_INFO_DECR = AbstractDoubleCheckerView
			.getImageDescriptor("info_decr.gif");

	private final ImageDescriptor fBaseImage;

	private boolean fDrawBaseAtZero;

	private int fFlags;

	private Point fSize;

	/**
	 * ImageDescriptor:fBaseImage -> (MAP String.intern():fFlags -> Image)
	 */
	private static final Map<ImageDescriptor, Map<String, Image>> imageCache = new HashMap<ImageDescriptor, Map<String, Image>>();

	/**
	 * Creates a new JavaElementImageDescriptor.
	 * 
	 * @param baseImage
	 *            an image descriptor used as the base image
	 * @param flags
	 *            flags indicating which adornments are to be rendered. See
	 *            <code>setAdornments</code> for valid values.
	 * @param size
	 *            the size of the resulting image
	 * @see #setAdornments(int)
	 */
	public ResultsImageDescriptor(ImageDescriptor baseImage, int flags,
			Point size) {
		fBaseImage = baseImage;
		fDrawBaseAtZero = false;
		assert fBaseImage != null;
		fFlags = flags;
		assert fFlags >= 0;
		fSize = size;
		assert fSize != null;
	}

	public void drawBaseAtZero() {
		fDrawBaseAtZero = true;
	}

	/**
	 * Returns the image, should always be used to avoid running out of SWT
	 * Image objects.
	 * 
	 * @return an image
	 */
	public Image getCachedImage() {
		Image result;
		String flagsKey = (new Integer(fFlags)).toString();
		if (imageCache.containsKey(fBaseImage)) {
			Map<String, Image> flagMap = imageCache.get(fBaseImage);
			if (flagMap.containsKey(flagsKey)) {
				result = flagMap.get(flagsKey);
			} else {
				// add the flag cachemap
				result = this.createImage();
				flagMap.put(flagsKey, result);
			}
		} else {
			// create image and add to both cachemaps
			result = this.createImage();
			Map<String, Image> flagMap = new HashMap<String, Image>();
			flagMap.put(flagsKey, result);
			imageCache.put(fBaseImage, flagMap);
		}
		return result;
	}

	/**
	 * Sets the descriptors adornments. Valid values are:
	 * <code>CONSISTENT</code>, <code>INCONSISTENT</code>,
	 * <code>REDDOT</code>, or any combination of those.
	 * 
	 * @param adornments
	 *            the image descritpors adornments
	 */
	public void setAdornments(int adornments) {
		assert adornments >= 0;
		fFlags = adornments;
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
	 * Sets the size of the image created by calling <code>createImage()</code>.
	 * 
	 * @param size
	 *            the size of the image returned from calling
	 *            <code>createImage()</code>
	 * @see ImageDescriptor#createImage()
	 */
	public void setImageSize(Point size) {
		assert size != null;
		assert size.x >= 0 && size.y >= 0;
		fSize = size;
	}

	/**
	 * Returns the size of the image created by calling
	 * <code>createImage()</code>.
	 * 
	 * @return the size of the image created by calling
	 *         <code>createImage()</code>
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

	/**
	 * Method declared on Object.
	 */
	@Override
	public boolean equals(Object object) {
		if (object == null
				|| !ResultsImageDescriptor.class.equals(object.getClass()))
			return false;

		ResultsImageDescriptor other = (ResultsImageDescriptor) object;
		return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize
				.equals(other.fSize));
	}

	/**
	 * Method declared on Object.
	 */
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

		drawImage(bg, (fDrawBaseAtZero ? 0 : 3), 0);

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
			System.out.println("Image data not available: "
					+ descriptor.toString());
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
		if ((fFlags & INFO_WARNING) != 0) {
			ImageData data = getImageData(DESC_WARNING_DECR);
			drawImage(data, size.x - data.width, size.y - data.height);
		} else if ((fFlags & INFO) != 0) {
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
}