package edu.afit.csce593.smallworld.model;

/**
 * This class defines an enumerated type for the directions by which Locations
 * are related and Characters can move.
 * 
 * @author Robert Graham
 * @author T.J. Halloran
 */
public enum Direction {

	NORTH, SOUTH, EAST, WEST;

	/**
	 * Gets the abbreviation for this direction. This is defined to be the first
	 * letter of the direction, for example <code>N</code> for
	 * <code>NORTH</code>.
	 * 
	 * @return the abbreviation used for this direction.
	 */
	public String getAbbreviation() {
		return this.toString().substring(0, 1);
	}

	/**
	 * Gets the appropriate {@link Direction} instance from a full or
	 * abbreviated string mnemonic. For example, {@link Direction#NORTH} is
	 * returned for the mnemonic "NORTH" or "N".
	 * 
	 * @param mnemonic
	 *            the full or abbreviated {@link String} name for the desired
	 *            direction.
	 * @return the appropriate {@link Direction} instance, or <code>null</code>
	 *         if the mnemonic was not recognized.
	 */
	static public Direction getInstance(String mnemonic) {
		if (mnemonic == null)
			return null;
		for (Direction possibleDirection : Direction.values()) {
			if (mnemonic.equalsIgnoreCase(possibleDirection.toString())
					|| mnemonic.equalsIgnoreCase(possibleDirection
							.getAbbreviation()))
				return possibleDirection;
		}
		return null;
	}
}
