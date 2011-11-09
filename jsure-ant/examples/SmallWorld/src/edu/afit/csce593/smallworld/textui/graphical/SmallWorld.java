package edu.afit.csce593.smallworld.textui.graphical;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;

import edu.afit.csce593.smallworld.controller.WorldController;
import edu.afit.csce593.smallworld.model.World;
import edu.afit.csce593.smallworld.textui.ParserWorldObserver;
import edu.afit.csce593.smallworld.textui.parser.UserCommandParser;

/**
 * The main program for SmallWorld with a simple graphical user interface. It
 * has a window that lets the user type commands and see game output in a text
 * box. Thus, it is not that much different from a text-only user interface.
 * <p>
 * This application uses the Swing graphical user interface library (part of the
 * Java standard library).
 * 
 * @author T.J. Halloran
 * @author Robert Graham
 */
public final class SmallWorld extends JFrame {

	/**
	 * The main program for SmallWorld with a text user interface.
	 * 
	 * @param args
	 *            command-line arguments (ignored by this program).
	 */
	public static void main(String[] args) {
		new SmallWorld();
	}

	/**
	 * This constructor sets up the user interface for our application. This
	 * includes registering all listeners (observers of user interface actions,
	 * e.g., clicking on a button).
	 */
	public SmallWorld() {
		super("It's a Small World");

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		setSize(600, 500);

		// we'll use a fixed font for dialog entry and messages
		Font fixed = new Font("Courier", Font.PLAIN, 14);

		/**
		 * The controller this user interface interacts with. The controller
		 * initially uses a default world, however this can be subsequently
		 * changed by calling {@link WorldController#setWorld(World)}.
		 */
		final WorldController f_wc = new WorldController();

		/*
		 * SWING (user interface) variables
		 */
		final JLabel f_messageLabel = new JLabel(
				"Enter message or type 'help':");
		final JTextField f_messageField = new JTextField();
		final JButton f_sendButton = new JButton("Send");
		final JScrollPane f_scroll;
		final JTextArea f_serverResponsesTextArea = new JTextArea("");

		FocusListener f_selectOnFocus = new FocusListener() {
			public void focusGained(FocusEvent e) {
				((JTextComponent) e.getSource()).selectAll();
			}

			public void focusLost(FocusEvent e) {
			}
		};

		/**
		 * The listener that receives commands and messages from the parser
		 */
		final GraphicalParserWorldObserver pwo = new GraphicalParserWorldObserver(
				f_serverResponsesTextArea);
		f_wc.getWorld().addObserver(pwo);

		/**
		 * A text parser for game commands. This parser aggregates our world
		 * controller and invokes game logic on the controller when it
		 * understands player commands to the game.
		 */
		final UserCommandParser f_parser = new UserCommandParser(f_wc, pwo);

		// setup the main application panel
		JPanel p1 = new JPanel();
		p1.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.insets = new Insets(10, 5, 10, 5);
		c.anchor = GridBagConstraints.CENTER;
		// 1st row: Enter message: [____] [Send]
		c.weightx = 0;
		p1.add(f_messageLabel, c);
		c.gridx = 1;
		c.weightx = 1;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		p1.add(f_messageField, c);
		f_messageField.setFont(fixed);
		f_messageField.addFocusListener(f_selectOnFocus);
		c.gridx = 4;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		p1.add(f_sendButton, c);
		f_sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String messageForServer = f_messageField.getText();
				f_messageField.setText(""); // clear input field on the screen
				f_parser.parse(messageForServer);
			}
		});
		getContentPane().add(p1, BorderLayout.NORTH);

		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.LOWERED),
				"Last game response:"));
		p2.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		f_serverResponsesTextArea.setEditable(false);
		f_serverResponsesTextArea.setFont(fixed);
		f_scroll = new JScrollPane(f_serverResponsesTextArea);
		p2.add(f_scroll, c);
		getContentPane().add(p2, BorderLayout.CENTER);

		this.getRootPane().setDefaultButton(f_sendButton);

		// make the user interface visible on the screen
		setVisible(true);

		// We can't do this until the GUI is fully realized
		Insets inset = f_serverResponsesTextArea.getInsets();
		pwo
				.setTextWidth((f_serverResponsesTextArea.getWidth()
						- inset.left - inset.right)
						/ getFontMetrics(fixed).charWidth('M') - 1);

		// Update the display so the user can see the player's initial location
		pwo.update(f_wc.getWorld());
	}

	private class GraphicalParserWorldObserver extends ParserWorldObserver {
		final JTextArea f_textArea;

		GraphicalParserWorldObserver(JTextArea area) {
			f_textArea = area;
		}

		@Override
		public void gameOver() {
			JOptionPane.showMessageDialog(SmallWorld.this,
					"Your adventure in SmallWorld is over.", "Game Over",
					JOptionPane.INFORMATION_MESSAGE);
			System.exit(0);
		}

		@Override
		public void show(String msg) {
			f_textArea.setText(msg);
		}
	}

	/**
	 * Needed to make the compiler happy (JFrame is serializable). Just ignore
	 * this field.
	 */
	private static final long serialVersionUID = 1L;
}
