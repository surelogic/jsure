package edu.afit.csce593.smallworld.textui.console;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.afit.csce593.smallworld.controller.WorldController;
import edu.afit.csce593.smallworld.model.World;
import edu.afit.csce593.smallworld.textui.ParserWorldObserver;
import edu.afit.csce593.smallworld.textui.parser.UserCommandParser;

/**
 * The main program for SmallWorld with a text user interface.
 * 
 * @author T.J. Halloran
 * @author Robert Graham
 */
public final class SmallWorld {

	/**
	 * The main program for SmallWorld with a text user interface.
	 * 
	 * @param args
	 *            command-line arguments (ignored by this program).
	 */
	public static void main(String[] args) {
		out.println("Welcome to It's a Small World!");
		out.println();
		SmallWorld sm = new SmallWorld();
		/*
		 * Setup an observer of the game world. This causes the world to call
		 * the update(World) method below when the world changes. For example, a
		 * new message is ready to be output to the user or its time to quit the
		 * game.
		 */
		sm.f_wc.getWorld().addObserver(sm.f_po); // observe the game world
		sm.f_po.setTextWidth(72);
		/*
		 * Trigger a description of the player's starting location.
		 */
		sm.f_po.update(sm.f_wc.getWorld());
		/*
		 * Enter the main loop (input-response) for the game.
		 */
		sm.playGame();
	}

	/**
	 * The controller this user interface interacts with. The controller
	 * initially uses a default world, however this can be subsequently changed
	 * by calling {@link WorldController#setWorld(World)}.
	 */
	private final WorldController f_wc = new WorldController();

	/**
	 * A listener that receives commands and messages from the parser
	 */
	private final TextualParserWorldObserver f_po = new TextualParserWorldObserver();

	/**
	 * A text parser for game commands. This parser aggregates our world
	 * controller and invokes game logic on the controller when it understands
	 * player commands to the game.
	 */
	private final UserCommandParser f_parser = new UserCommandParser(f_wc, f_po);

	/**
	 * A IO stream used to input the user's commands from the console.
	 */
	private static final BufferedReader f_in = new BufferedReader(
			new InputStreamReader(System.in));

	/**
	 * Flag to indicate if the user has requested the game to end. A value of
	 * <code>true</code> indicates the game should continue. A value of
	 * <code>false</code> indicates the game should terminate as soon as
	 * possible.
	 */
	private boolean f_continuePlaying = true;

	/**
	 * The main loop for the user interface. This method repeatedly prompts the
	 * user for commands and processes the user's commands. It does not, by
	 * design, output the results of the command on the game because this is
	 * done via a callback to {@link #update(World)}. Typically, this will
	 * occur as a result of each user command, so the user will see some output
	 * describing of the impact of his or her command on the game.
	 */
	private void playGame() {
		try {
			while (f_continuePlaying) {
				String command = readLineFromConsole();
				if (command != null)
					f_parser.parse(command);
			}
		} catch (IOException e) {
			/*
			 * Our ability to input from the console has failed for some reason.
			 * Print out a stack trace to the console.
			 */
			e.printStackTrace();
		}
	}

	/**
	 * Reads a single line of text from the console. This is the users's next
	 * command for the game.
	 * 
	 * @return the user's command (a single line of text).
	 * @throws IOException
	 *             if our attempt to input from the console has failed for some
	 *             reason. This should typically not happen.
	 */
	private String readLineFromConsole() throws IOException {
		out.print("> "); // output a prompt
		return f_in.readLine();
	}

	private class TextualParserWorldObserver extends ParserWorldObserver {
		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.afit.csce593.smallworld.textui.ParserWorldObserver#gameOver()
		 */
		@Override
		public void gameOver() {
			f_continuePlaying = false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.afit.csce593.smallworld.textui.ParserWorldObserver#show(java.lang.String)
		 */
		@Override
		public void show(String msg) {
			out.println(msg);
		}
	}
}
