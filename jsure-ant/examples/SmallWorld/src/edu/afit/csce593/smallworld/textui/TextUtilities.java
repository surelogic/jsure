package edu.afit.csce593.smallworld.textui;

import edu.afit.csce593.smallworld.model.Place;
import edu.afit.csce593.smallworld.model.Direction;

/**
 * A convenience class of handy constants and methods for working with text.
 * 
 * @author Robert Graham
 */
public class TextUtilities {

	/**
	 * Disable default constructor
	 */
	private TextUtilities() {
	}

	/**
	 * The operating system-specific line separation character.
	 */
	public static final String LINESEP = System.getProperty("line.separator");

	/**
	 * Convenience constant for adding two line separators at once.
	 */
	public static final String LINESEP2 = LINESEP + LINESEP;

	/**
	 * Gets several lines of directions about the possible travel destinations
	 * from a place. These are in the form "To the north you see the Hall."
	 * 
	 * @param place
	 *            the place of origin
	 * @return directions and travel destinations from this place.
	 */
	public static String getDirectionsFrom(Place place) {
		StringBuilder result = new StringBuilder();
		for (Direction possibleDirection : Direction.values()) {
			if (place.isTravelAllowedToward(possibleDirection)) {
				result.append("To the "
						+ possibleDirection.toString().toLowerCase()
						+ " you see "
						+ place.getTravelDestinationToward(possibleDirection)
								.getShortDescription() + ". ");
			}
		}
		return result.toString().trim();
	}

	/**
	 * Gets a full description of the specified place, including possible
	 * destinations for travel from there.
	 * 
	 * @param place
	 *            the place to describe
	 */
	public static String getFullLocationDescription(Place place) {
		StringBuilder msg = new StringBuilder(place.getShortDescription());
		// Capitalize the first letter
		msg.replace(0, 1, msg.substring(0, 1).toUpperCase());
		msg.append(". ");
		msg.append(place.getDescription());
		msg.append(LINESEP2);
		msg.append(TextUtilities.getDirectionsFrom(place));
		return msg.toString();
	}
}
