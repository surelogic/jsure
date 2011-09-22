package edu.afit.csce593.smallworld.controller;

import java.io.File;

import edu.afit.csce593.smallworld.model.*;
import edu.afit.csce593.smallworld.persistence.WorldPersistence;

/**
 * This class is responsible for executing a user's commands. In the
 * Model/View/Controller paradigm, a {@link WorldController} instance is the
 * controller for one {@link World} instance which is the model. It receives
 * commands from a user interface, makes the corresponding changes to its
 * associated {@link World}, and triggers notification to the {@link World}'s
 * observers.
 * 
 * @author Robert Graham
 * @author T.J. Halloran
 * 
 * @see World
 */
public final class WorldController {

	/**
	 * Creates a new instance of <code>WorldController</code> for the default
	 * world.
	 */
	public WorldController() {
		this(WorldPersistence.DEFAULT_WORLD);
	}

	/**
	 * Creates a new instance of <code>WorldController</code> for the
	 * specified world file, which it loads.
	 * 
	 * @param fileName
	 *            the world file to load
	 */
	public WorldController(String fileName) {
		assert (fileName != null);
		try {
			f_world = WorldPersistence.loadWorld(fileName);
		} catch (IllegalStateException e) {
			// World file failed to load, so create a trivial but valid world
			f_world = new World();
			reportException(e);
		}
	}

	/**
	 * Gets the world (i.e., model) associated with this controller.
	 * 
	 * @return the world associated with this controller.
	 */
	public World getWorld() {
		return f_world;
	}

	/**
	 * Loads a previously saved {@link World} from a file.
	 * 
	 * @param fileName
	 *            The name of the file to load
	 */
	public void loadWorld(String fileName) {
		assert (fileName != null);
		File file = new File(fileName);
		World newWorld;
		try {
			newWorld = WorldPersistence.loadWorld(file);
			setWorld(newWorld);
			f_world.addToMessage("File \"" + file.getAbsolutePath()
					+ "\" loaded.");
			f_world.addToMessage();
		} catch (IllegalStateException e) {
			reportException(e);
			f_world.addToMessage("File \"" + file.getAbsolutePath()
					+ "\" failed to load: ");
			f_world.addToMessage("Keeping current world");
		}
		f_world.turnOver();
	}

	/**
	 * Notifies the world that the user wants to quit the game.
	 */
	public void quit() {
		f_world.addToMessage("Bye!");
		f_world.setGameOver();
		f_world.turnOver();
	}

	/**
	 * Saves the current state of the {@link World} to a file. This world can be
	 * loaded and game play resumed using {@link #loadWorld(String)}.
	 * 
	 * @param fileName
	 *            The name of the file to create
	 */
	public void saveWorld(String fileName) {
		assert (fileName != null);
		File file = new File(fileName);
		try {
			WorldPersistence.saveWorld(f_world, file);
			f_world.addToMessage("Save file \"" + file.getAbsolutePath()
					+ "\" created.");
		} catch (IllegalStateException e) {
			reportException(e);
			f_world.addToMessage("Save to file \"" + file.getAbsolutePath()
					+ "\" FAILED.");
			f_world.addToMessage("You will not be able to load this world");
		}
		f_world.turnOver();
	}

	/**
	 * Removes the specified item from the player's location and places it in
	 * the player's inventory.
	 * 
	 * @param item
	 *            the item to take.
	 */
	public void take(Item item) {
	}

	/**
	 * Removes all items from the player's location and places them in the
	 * player's inventory.
	 */
	public void takeAll() {
	}

	/**
	 * Drops the specified item from the player's inventory.
	 * 
	 * @param item
	 *            the item to drop.
	 */
	public void drop(Item item) {
	}

	/**
	 * Examines the items in the player's inventory.
	 */
	public void inventory() {
	}

	/**
	 * Moves the player in the direction indicated.
	 * 
	 * @param direction
	 *            The direction the user wants the player to travel.
	 */
	public void travel(Direction direction) {
		assert direction != null;
		Player player = f_world.getPlayer();
		Place playerLocation = player.getLocation();
		if (playerLocation.isTravelAllowedToward(direction)) {
			Place newPlayerLocation = playerLocation
					.getTravelDestinationToward(direction);
			/*
			 * Move the player
			 */
			player.setLocation(newPlayerLocation);
		} else {
			/*
			 * Travel is not allowed from the player's location in the specified
			 * direction.
			 */
			f_world.addToMessage("Sorry, you can't move "
					+ direction.toString().toLowerCase() + " from here.");
			// addShortLocationDescription("You are at");
		}
		f_world.turnOver();
	}

	/**
	 * @param e
	 */
	private void reportException(Throwable e) {
		StringBuilder s = new StringBuilder();
		s.append(e.getMessage());
		if (e.getCause() != null)
			s.append(": " + e.getCause().getMessage());
		f_world.addToMessage(s.toString());
	}

	/**
	 * Sets the world (i.e., model) associated with this controller. The set of
	 * observers of the old world are setup to observer the new world.
	 * 
	 * @param world
	 *            a non-null game world.
	 */
	public void setWorld(World world) {
		assert (world != null);
		/*
		 * Transfer all observers of the old world to the new world.
		 */
		for (IWorldObserver o : f_world.getObservers()) {
			world.addObserver(o);
		}
		f_world = world;
	}

	/**
	 * The world associated with this controller. It must be non-null, but may
	 * be changed via the {@link #setWorld(World)} method.
	 */
	private World f_world;
}
