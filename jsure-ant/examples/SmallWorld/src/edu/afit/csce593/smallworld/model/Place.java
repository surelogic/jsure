package edu.afit.csce593.smallworld.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a place, such as a room or corridor, that a character in the game
 * can travel through. Each instance is also contained, or exists within, a
 * single {@link World} instance.
 * 
 * @author Robert Graham
 * @author T.J. Halloran
 */
public class Place {

	/**
	 * Constructs a new instance. Only to be invoked by the {@link World} class.
	 * 
	 * @param world
	 *            the {@link World} instance this exists within.
	 * @param name
	 *            a non-null unique name for the instance. The uniqueness of the
	 *            name can't be dependent upon case, e.g., "Hall" is considered
	 *            the same as "hall".
	 * @param article
	 *            the appropriate non-null indefinite article with which to
	 *            prefix the name so as to form a proper short description,
	 *            e.g., "the" or "a".
	 * @param description
	 *            a long, possibly mult-line, non-null description of this
	 *            thing.
	 */
	Place(World world, String name, String article, String description) {
		assert (world != null);
		assert (name != null);
		assert (article != null);
		assert (description != null);
		f_world = world;
		f_name = name;
		f_article = article;
		f_description = description;
	}

	/**
	 * The immutable reference to the {@link World} instance this is contained
	 * within.
	 */
	private final World f_world;

	/**
	 * Gets the world this exists in.
	 * 
	 * @return a reference to the {@link World} instance containing this.
	 */
	public final World getWorld() {
		return f_world;
	}

	/**
	 * The name of this thing, e.g., "Grande Hall". This identifier uniquely
	 * identifies this {@link Thing} instance within the game.
	 */
	private final String f_name;

	/**
	 * Gets the name of this.
	 * 
	 * @return the name of this.
	 */
	public final String getName() {
		return f_name;
	}

	/**
	 * The appropriate indefinite article with which to prefix the name so as to
	 * form a proper short description, e.g., "the" or "a".
	 */
	private final String f_article;

	/**
	 * Gets the article of this. This refers to the appropriate indefinite
	 * article with which to prefix the name so as to form a proper short
	 * description, e.g., "the" or "a".
	 * 
	 * @return the article of this.
	 */
	public final String getArticle() {
		return f_article;
	}

	/**
	 * Gets the short description, e.g., "the Grande Hall", for this.
	 * 
	 * @return the short description for this.
	 */
	public final String getShortDescription() {
		return f_article + " " + f_name;
	}

	/**
	 * A long description of this thing.
	 */
	private final String f_description;

	/**
	 * Gets the long description of this.
	 * 
	 * @return the long description for this.
	 */
	public final String getDescription() {
		return f_description;
	}

	/**
	 * A mapping of directions of travel to the neighboring places. The map is
	 * ragged in the sense that if a direction is not legal to travel in a
	 * <code>null</code> will be returned. For example,
	 * <code>null == f_directionOfTravelToplace.get(Direction.NORTH)</code>
	 * will be <code>true</code> if no place exists to the north of this one.
	 */
	private final Map<Direction, Place> f_directionOfTravelToPlace = new HashMap<Direction, Place>();

	/**
	 * Returns <code>true</code> if travel is allowed in the specified
	 * direction from this place, <code>false</code> otherwise.
	 * 
	 * @param d
	 *            the non-null direction of travel.
	 * @return <code>true</code> if travel is allowed in the specified
	 *         direction from this place, <code>false</code> otherwise.
	 */
	public boolean isTravelAllowedToward(Direction d) {
		assert (d != null);
		return f_directionOfTravelToPlace.containsKey(d);
	}

	/**
	 * Gets the destination of travel in the specified direction.
	 * 
	 * @param d
	 *            the non-null direction of travel.
	 * @return the destination of travel in the specified direction, or
	 *         <code>null</code> if travel is not allowed in that direction.
	 * @see #isTravelAllowedToward(Direction)
	 */
	public Place getTravelDestinationToward(Direction d) {
		assert (d != null);
		return f_directionOfTravelToPlace.get(d);
	}

	/**
	 * Sets the destination of travel in the specified direction for this place.
	 * 
	 * @param d
	 *            the non-null direction of travel.
	 * @param l
	 *            the non-null destination of travel in the specified direction.
	 */
	public void setTravelDestination(Direction d, Place l) {
		assert (d != null);
		assert (l != null);
		f_directionOfTravelToPlace.put(d, l);
	}
}
