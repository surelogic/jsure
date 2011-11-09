package edu.afit.csce593.smallworld.persistence;

import java.io.*;
import java.net.URL;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import edu.afit.csce593.smallworld.model.*;

/**
 * The persistence capability for the game. This class defines static methods to
 * load and store the game state as an XML file. It uses the JDOM library to
 * manipulate XML trees.
 * <p>
 * Persistence is separate architecture layer from the game model. Specifically,
 * this class depends upon the model but the model <i>never</i> depends upon
 * this package.
 * 
 * @author T.J. Halloran
 */
public class WorldPersistence {

	/**
	 * The version of the game as defined by the XML save file format.
	 */
	public static final String SAVEFILE_VERSION = "1.0";

	/**
	 * The full location, on the Java classpath, of the default world file.
	 */
	public static final String DEFAULT_WORLD = "/edu/afit/csce593/smallworld/persistence/DefaultWorld.xml";

	/**
	 * Loads the game state from the specified filename on the Java classpath of
	 * the running program and creates a usable World instance.
	 * 
	 * @param classpathLocation
	 *            the non-null string representing the full location, on the
	 *            Java classpath, of the desired world file.
	 * @return a game world.
	 */
	public static World loadWorld(String classpathLocation) {
		URL defaultURL = Object.class.getResource(classpathLocation);
		if (defaultURL == null) {
			throw new IllegalStateException(
					"Unable to find world file:  cannot locate \""
							+ classpathLocation + "\" in classpath");
		}
		try {
			InputStream in = defaultURL.openStream();
			return loadWorld(in);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to open world file", e);
		}
	}

	/**
	 * Loads the game state from the specified {@link File} and creates a usable
	 * World instance. This is a convenience method that simply opens and
	 * {@link java.io.InputStream} on the specified {@link File} and calls
	 * {@link #loadWorld(InputStream)}.
	 * 
	 * @param file
	 *            the non-null file to read the game state from.
	 * @return a game world.
	 */
	public static World loadWorld(File file) {
		try {
			return loadWorld(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Unable to find world file", e);
		}
	}

	/**
	 * Loads the game state from the specified {@link java.io.InputStream} and
	 * creates a {@link World} usable World instance.
	 * 
	 * @param in
	 *            the non-null stream to read the game state from.
	 * @return a game world.
	 */
	public static World loadWorld(InputStream in) {
		assert (in != null);

		World world = new World();
		SAXBuilder parser = new SAXBuilder();
		try {
			Document saveXML = parser.build(in);
			Element root = saveXML.getRootElement();

			loadPlaceXML(root, world);

			loadPlayerXML(root, world);

		} catch (IOException e) {
			throw new IllegalStateException(
					"A system error occurred while reading world file:", e);
		} catch (JDOMException e) {
			throw new IllegalStateException("Errors found in world file:", e);
		}
		return world;
	}

	/**
	 * Saves the state of the specified {@link World} into the specified
	 * {@link File} in XML format. It is suggested calls to this surround the
	 * call with a try-catch block if recovery from a save problem is desired.
	 * 
	 * @param world
	 *            the game state to save.
	 * @param file
	 *            the file to save the game state to.
	 * @throws IllegalStateException
	 *             if something goes wrong.
	 */
	public static void saveWorld(World world, File file) {
		Element worldElement = new Element(SMALLWORLD_TAG);

		worldElement.setAttribute(VERSION_TAG, SAVEFILE_VERSION);

		/*
		 * Create XML for Places
		 */
		for (Place l : world.getPlaces()) {
			/*
			 * We don't save the nowhere place to the save file. This place
			 * always exists in every world so its inclusion in the save file
			 * XML will cause an attempt on loading a save file into a model to
			 * try and create it again (resulting in an exception).
			 */
			if (l != world.getNowherePlace()) {
				worldElement.addContent(createPlaceXML(l));
			}
		}

		/*
		 * Create XML for the Player
		 */
		worldElement.addContent(createPlayerXML(world.getPlayer()));

		Document gameStateInformation = new Document(worldElement);

		try {
			OutputStream save = new BufferedOutputStream(new FileOutputStream(
					file));
			// XML outputter with two-space indentation and newlines after
			// elements
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			// actually output the XML tree to the save file
			outputter.output(gameStateInformation, save);
			save.close();
		} catch (IOException e) {
			// something went wrong
			throw new IllegalStateException("Unable to write world file", e);
		}
	}

	/**
	 * Creates an XML tree for a game place.
	 * 
	 * @param place
	 *            the game place.
	 * @return the constructed XML tree.
	 */
	private static Element createPlaceXML(Place place) {
		Element placeElement = new Element(PLACE_TAG);
		placeElement.setAttribute(NAME_TAG, place.getName());
		placeElement.setAttribute(ARTICLE_TAG, place.getArticle());
		Element description = new Element(DESCRIPTION_TAG);
		placeElement.addContent(description);
		description.setText(place.getDescription());

		for (Direction possibleDirection : Direction.values()) {
			if (place.isTravelAllowedToward(possibleDirection)) {
				Element neighbor = new Element(TRAVEL_TAG);
				placeElement.addContent(neighbor);
				neighbor.setAttribute(DIRECTION_TAG, possibleDirection
						.getAbbreviation());
				neighbor.setText(place.getTravelDestinationToward(
						possibleDirection).getName());
			}
		}

		return placeElement;
	}

	/**
	 * Creates an XML tree for the player.
	 * 
	 * @param player
	 *            the player.
	 * @return the constructed XML tree.
	 */
	private static Element createPlayerXML(Player player) {
		Element playerElement = new Element(PLAYER_TAG);
		playerElement.setAttribute(LOCATION_TAG, ""
				+ player.getLocation().getName());
		return playerElement;
	}

	/**
	 * Loads all places found within the root XML element into the world under
	 * construction.
	 * 
	 * @param root
	 *            the root of the save file's XML tree.
	 * @param world
	 *            the game world under construction.
	 */
	@SuppressWarnings("unchecked")
	private static void loadPlaceXML(Element root, World world) {
		List<Element> placeList = root.getChildren(PLACE_TAG);
		/*
		 * First Pass: We need to be careful on creating the map of places
		 * because the interconnections require the places to exist in the world
		 * (a chicken and the egg type problem). Hence, we do this in two steps.
		 * The first step is to load in the descriptive information about all
		 * the places and create them all within the world under construction.
		 */
		for (Element placeElement : placeList) {
			String name = placeElement.getAttributeValue(NAME_TAG);
			String article = placeElement.getAttributeValue(ARTICLE_TAG);
			String description = placeElement.getChild(DESCRIPTION_TAG)
					.getText();
			if (name == null || article == null || description == null)
				throw new IllegalStateException();

			world.createPlace(name, article, description);
		}
		/*
		 * Second Pass: Next, we need to connect the places into a map as
		 * specified by the "travel" elements in the XML.
		 */
		for (Element placeElement : placeList) {
			Place l = world.getPlace(placeElement.getAttributeValue(NAME_TAG));
			if (l == null)
				throw new IllegalStateException(
						"Unable to find a place named \""
								+ placeElement.getAttributeValue(NAME_TAG)
								+ "\" during the second pass through the file..."
								+ "did the file change while we were reading it?");

			List<Element> travelList = placeElement.getChildren(TRAVEL_TAG);
			for (Element t : travelList) {
				Direction d = Direction.getInstance(t
						.getAttributeValue(DIRECTION_TAG));
				if (d == null)
					throw new IllegalStateException("\""
							+ t.getAttributeValue(DIRECTION_TAG)
							+ "\" is not a valid direction for travel from "
							+ "the place named \"" + l.getName() + "\"");

				Place destPlace = world.getPlace(t.getText());
				if (destPlace == null)
					throw new IllegalStateException(
							"Unable to find a place named \"" + t.getText()
									+ "\" as the destination when traveling "
									+ d + " from the place named \""
									+ l.getName() + "\"");

				l.setTravelDestination(d, destPlace);
			}
		}
	}

	/**
	 * Loads information about the player found within the root XML element into
	 * the world under construction.
	 * 
	 * @param root
	 *            the root of the save file's XML tree.
	 * @param world
	 *            the game world under construction.
	 */
	private static void loadPlayerXML(Element root, World world) {
		Element playerElement = root.getChild(PLAYER_TAG);
		if (playerElement == null)
			throw new IllegalStateException();

		String locationName = playerElement.getAttributeValue(LOCATION_TAG);
		if (locationName != null) {
			Place location = world.getPlace(locationName);
			if (location != null) {
				world.getPlayer().setLocation(location);
			} else {
				System.err.println("Unable to find a place named \""
						+ locationName + "\" as the player's location");
			}
		}
	}

	private static final String ARTICLE_TAG = "article";

	private static final String DESCRIPTION_TAG = "description";

	private static final String DIRECTION_TAG = "direction";

	private static final String PLACE_TAG = "place";

	private static final String LOCATION_TAG = "location";

	private static final String NAME_TAG = "name";

	private static final String PLAYER_TAG = "player";

	private static final String SMALLWORLD_TAG = "smallworld";

	private static final String TRAVEL_TAG = "travel";

	private static final String VERSION_TAG = "version";
}
