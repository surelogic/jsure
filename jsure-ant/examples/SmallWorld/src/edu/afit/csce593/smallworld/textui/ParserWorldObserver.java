package edu.afit.csce593.smallworld.textui;

import edu.afit.csce593.smallworld.model.IWorldObserver;
import edu.afit.csce593.smallworld.model.Place;
import edu.afit.csce593.smallworld.model.World;
import edu.afit.csce593.smallworld.textui.parser.IParserObserver;
import static edu.afit.csce593.smallworld.textui.TextUtilities.*;

/**
 * An partial implementation of IParserObserver and IWorldObserver that factors
 * out the common code between graphical and command-line text-based interfaces.
 * Subclasses "fill in the blanks" by implementing the abstract methods.
 * 
 * @author Robert Graham
 */
public abstract class ParserWorldObserver implements IParserObserver,
		IWorldObserver {
	/**
	 * Creates a new instance of <code>ParserWorldObserver</code>.
	 */
	protected ParserWorldObserver() {
		super();
	}

	/**
	 * Displays a message on the user interface
	 * 
	 * @param msg
	 *            The message to display
	 */
	public abstract void show(String msg);

	/**
	 * Informs the user that the game is over and arranges for termination
	 */
	public abstract void gameOver();

	/**
	 * Sets the desired width for word-wrapped text, which is the column at
	 * which lines are broken.
	 * 
	 * @param width
	 *            The desired width of each line of text
	 */
	public void setTextWidth(int width) {
		f_message.setDesiredTextWidth(width);
	}

	public void display(String msg) {
		f_message.clear();
		f_message.append(msg);
		show(f_message.toString());
	}

	public void look(World world) {
		display(getFullLocationDescription(world.getPlayer().getLocation()));
	}

	/**
	 * Invoked by the game model when its state has changed. Typically, this
	 * indicates that it has an interesting message it wants to communicate to
	 * the user. Thus, we output the message to the user. If the game is over,
	 * we note this through the game model and terminate this program.
	 * 
	 * @see IWorldObserver#update(World)
	 */
	public void update(World world) {
		/*
		 * Show the world message to the user. If player's location changed,
		 * show its full description.
		 */
		String text = world.getMessage();
		Place newLocation = world.getPlayer().getLocation();
		String newLocationId = newLocation.getName();
		if (!newLocationId.equals(f_playerLocationId)) {
			text += getFullLocationDescription(newLocation);
			f_playerLocationId = newLocationId;
		}
		display(text);
		/*
		 * Check if the game is over. If so, pop up a dialog and let the user
		 * read the final message before we exit.
		 */
		if (world.isGameOver()) {
			gameOver();
		}

	}

	/**
	 * The unique identifier of the player's current location. Whenever this
	 * changes, the UI automatically does a "look" command.
	 */
	private String f_playerLocationId;

	/**
	 * Formatter for text messages.
	 */
	private final WordWrappedMessage f_message = new WordWrappedMessage();
}
