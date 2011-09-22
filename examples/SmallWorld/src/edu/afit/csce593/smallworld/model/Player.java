package edu.afit.csce593.smallworld.model;

/**
 * An avatar that is controlled by, and represents, the user of the game. At
 * present there is one {@link Player} roaming around per {@link World}
 * instance.
 * 
 * @author Robert Graham
 * @author T.J. Halloran
 */
public final class Player {

	/**
	 * Constructs a new instance. Only to be invoked by the {@link World} class
	 * once per {@link World} instance as each world only contains a single
	 * player.
	 * 
	 * @param world
	 *            the {@link World} instance this exists within.
	 */
	Player(World world) {
		assert world != null;
		f_world = world;
		f_name = "Player";
		f_location = f_world.getNowherePlace();
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
	 * The name of this Player. This identifier uniquely identifies this Player
	 * instance within the game.
	 */
	private final String f_name;

	/**
	 * Gets the name of this Player.
	 * 
	 * @return the name of this Player.
	 */
	public final String getName() {
		return f_name;
	}

	/**
	 * Gets the short description of this Player.
	 * 
	 * @return the short description for this Player.
	 */
	public final String getShortDescription() {
		return "the " + f_name;
	}

	/**
	 * A long description of this Player.
	 */
	private final String f_description = "Our Hero";

	/**
	 * Gets the long description of this Player.
	 * 
	 * @return the long description for this Player.
	 */
	public final String getDescription() {
		return f_description;
	}

	/**
	 * The non-null location of this Player.
	 */
	private Place f_location;

	/**
	 * Returns the non-null location of this Player.
	 * 
	 * @return the location of this Player
	 */
	public Place getLocation() {
		return f_location;
	}

	/**
	 * Sets the non-null location of this Player.
	 * 
	 * @param location
	 *            the new non-null location of this Player.
	 */
	public void setLocation(Place location) {
		assert (location != null);
		f_location = location;
	}
}
