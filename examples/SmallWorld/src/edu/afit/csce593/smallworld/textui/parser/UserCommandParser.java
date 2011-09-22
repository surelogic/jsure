package edu.afit.csce593.smallworld.textui.parser;

import edu.afit.csce593.smallworld.controller.WorldController;
import edu.afit.csce593.smallworld.model.Direction;
import static edu.afit.csce593.smallworld.textui.TextUtilities.*;

/**
 * Parses user commands and informs a specified {@link WorldController} instance
 * of its results. For example, the String "go north" causes the
 * {@link WorldController#travel(Direction)} method to be invoked.
 * 
 * @author T.J. Halloran
 * @author Robert Graham
 */
public final class UserCommandParser {

    /**
     * The non-null world controller associated with this parser.
     */
    private final WorldController f_wc;

    /**
     * An observer of this parser, recipient of certain UI-only
     * commands and error messages.
     */
    private IParserObserver f_pwo;

    /**
     * Constructs a text command parser with the specified observer and
     * WorldController.  The controller is sent commands that affect the
     * World (or could), while the observer is sent error messages and
     * output from commands that only query the world, in particular
     * those expected to be unique to a text-based interface.
     * 
     * @param controller a non-null controller for this parser to invoke once it
     *            understands the user's game command.
     * @param observer a non-null observer for this parser to send messages and
     *            query command output to
     */
    public UserCommandParser(WorldController controller,
                             IParserObserver observer) {
        assert (controller != null && observer != null);

        f_wc = controller;
        f_pwo = observer;
    }

    /**
     * Parses the user's command and sending appropriate messages to the
     * {@link WorldController}. Once the users command is understood (i.e.,
     * parsed) a specific method is invoked on the controller. If the parser
     * couldn't understand the user's command an error message is sent.
     * 
     * @param command the users's command to the game.
     */
    public void parse(String command) {
        String[] words = command.trim().toUpperCase().split("\\s+");

        /*
         * The below flag is used to indicate if we were able to understand the
         * command and callback our observer (f_observer) to execute a command
         * in the game.
         */
        boolean commandExecuted = false;

        if (words[0].equals("GO") || words[0].equals("MOVE")) {
            /*
             * "GO <direction>" command
             */
            Direction direction = null;
            if (words.length == 1) {
                f_pwo.display(
                        words[0] + " where?  You must specify a direction!"
                        + " Please type \"help\" if you need more help.");
            } else {
                direction = Direction.getInstance(words[1]);
                if (direction == null) {
                    f_pwo.display(
                        words[1] + " is not a direction I recognize. "
                        + "Please type \"help\" if you need more help.");
                } else {
                    f_wc.travel(direction);
                }
            }
            commandExecuted = true;

        } else if (words[0].equals("HELP")) {
            /*
             * "HELP" command
             */
            f_pwo.display(getHelpMessage());
            commandExecuted = true;

        } else if (words[0].equals("LOAD")) {
            /*
             * "LOAD <file>" command
             */
            String fileName = command.substring(words[0].length()).trim();
            if (fileName.length() == 0) {
                f_pwo.display(
                        "You must specify a file name to load a saved game.  "
                        + "Please type \"help\" if you need more help.");
            } else {
                f_wc.loadWorld(fileName);
            }
            commandExecuted = true;

        } else if (words[0].equals("LOOK")) {
            /*
             * "LOOK" command
             * This is executed locally and the result sent through
             * the Controller's World back to its observers.
             */
            f_pwo.look(f_wc.getWorld());
            commandExecuted = true;

        } else if (words[0].equals("QUIT") || words[0].equals("EXIT")) {
            /*
             * "QUIT" or "EXIT" command
             */
            f_wc.quit();
            commandExecuted = true;

        } else if (words[0].equals("SAVE")) {
            /*
             * "SAVE <file>" command
             */
            String fileName = command.substring(words[0].length()).trim();
            if (fileName.length() == 0) {
                f_pwo.display(
                        "You must specify a file name to save your game.  "
                        + "Please type \"help\" if you need more help.");
            } else {
                f_wc.saveWorld(fileName);
            }
            commandExecuted = true;
        }

        if (!commandExecuted) {
            /*
             * We couldn't understand the command the user entered. Hence we
             * need report this problem.
             */
            f_pwo.display("Sorry, I don't understand what the command \""
                            + command
                            + "\" means. Please type \"help\" if you need some help.");
        }
    }

    /**
     * Constructs a message containing some (hopefully useful) help
     * to the user about what the game commands do.
     */
    private String getHelpMessage() {
        StringBuilder helpMessage = new StringBuilder();
        helpMessage
                .append("You are controlling a player "
                        + "interacting with a small world created within the computer.  "
                        + "The commands you type control what the player does within "
                        + "the game.  Some actions add points to your score.  You win the "
                        + "game when you obtain enough points through your actions.");
        helpMessage.append(LINESEP2);
        helpMessage
                .append("The following is a description of the commands the "
                        + "game understands:");
        helpMessage.append(LINESEP2);
        helpMessage.append("\"go <north | south | east | west>\" moves the "
                + "player from his or her current location in the specified "
                + "direction to a new location. The word \"move\" may be used "
                + "to mean \"go\" within this command. "
                + "An example is \"go north\" or \"go n\"");
        helpMessage.append(LINESEP2);
        helpMessage.append("\"load <filename>\" loads the specified save file "
                + "into the game.  Discards the existing game state."
                + "  Example \"load C:\\save1.xml\"");
        helpMessage.append(LINESEP2);
        helpMessage.append("\"look\" examines the player's current location.");
        helpMessage.append(LINESEP2);
        helpMessage.append("\"quit\" or \"exit\" terminates the game.");
        helpMessage.append(LINESEP2);
        helpMessage.append("\"save <filename>\" saves the current game state "
                + "to the specified filename. Example \"save C:\\save1.xml\"");
        return helpMessage.toString();
    }
}
