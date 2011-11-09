package edu.afit.csce593.smallworld.model;

import java.util.*;

/**
 * The simulated world in which simulated players lead their short, simulated
 * lives. In the Model/View/Controller paradigm, this is the model. It is an
 * aggregate that contains everything that exists in the world. It also acts as
 * the subject in the Observer pattern, so that observers (e.g., views) can stay
 * current when the world changes in some interesting way.
 * <p>
 * A newly constructed world contains a {@link Player} and a nowhere
 * {@link Place}. The player is located at the nowhere place. The world, via
 * method calls, may then be mutated into something more interesting. This
 * approach ensures that even a newly constructed world is trivially playable.
 * 
 * @author Robert Graham
 * @author T.J. Halloran
 */
public final class World {

	/**
	 * A map from an upper-case name (thus non-case sensitive) to a
	 * corresponding {@link Place} instance. Typically the key to this map will
	 * be <code>name.toUpperCase()</code>.
	 */
	private final Map<String, Place> f_keyToPlace = new HashMap<String, Place>();

	/**
	 * A place that always exists in every world. It represents a thing being
	 * nowhere.
	 */
	private final Place f_nowhere = createPlace("Very Remote Place", "a",
			"You are in a very remote place.");

	/**
	 * Gets the {@link Place} representing nowhere. This place always exists in
	 * every world.
	 * 
	 * @return the place representing nowhere.
	 */
	public Place getNowherePlace() {
		return f_nowhere;
	}

	/**
	 * A {@link Player} that is controlled by, and represents, the user of the
	 * game.
	 */
	private final Player f_player = new Player(this);

	/**
	 * Constructs a new instance of the world class containing a single player
	 * and the nowhere place. The player is located at the nowhere place.
	 * 
	 * @see #getNowherePlace()
	 */
	public World() {
		clearMessage();
	}

	/**
	 * Gets a reference to the sole player interacting with this world.
	 * 
	 * @return the sole player within this world.
	 */
	public Player getPlayer() {
		return f_player;
	}

	/**
	 * Returns <code>true</code> if the specified name is used for a
	 * {@link Place} within this world, <code>false</code> otherwise. Names
	 * are non-case sensitive, so "NAME" is considered the same name as "nAmE".
	 * In addition, the namespace of the world is shared across all
	 * {@link Place} instances.
	 * 
	 * @param name
	 *            the non-null non-case sensitive name to check.
	 * @return <code>true</code> if the specified name is used for a
	 *         {@link Place} within this world, <code>false</code> otherwise.
	 */
	public boolean isNameUsed(String name) {
		assert (name != null);
		return f_keyToPlace.containsKey(name.toUpperCase());
	}

	/**
	 * Gets the appropriate {@link Place} instance with the specified name.
	 * 
	 * @param name
	 *            the non-null non-case sensitive name of the desired
	 *            {@link Place} instance.
	 * @return the appropriate {@link Place} instance, or <code>null</code> if
	 *         the specified name does not exist.
	 */
	public Place getPlaceByName(String name) {
		assert (name != null);
		return f_keyToPlace.get(name.toUpperCase());
	}

	/**
	 * Returns a copy of all the Places in this world.
	 * 
	 * @return a copy of the set of all Places in this world.
	 */
	public Set<Place> getPlaces() {
		return new HashSet<Place>(f_keyToPlace.values());
	}

	/**
	 * Gets the appropriate {@link Place} instance with the specified name.
	 * 
	 * @param name
	 *            the non-null non-case sensitive name of the desired
	 *            {@link Place} instance.
	 * @return the appropriate {@link Place} instance, or <code>null</code> if
	 *         the specified name does not exist or is not of the {@link Place}
	 *         type.
	 */
	public Place getPlace(String name) {
		assert (name != null);
		Place result = getPlaceByName(name);
		if (result instanceof Place)
			return (Place) result;
		else
			return null;
	}

	/**
	 * Constructs a new place within this world.
	 * 
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
	 * @return the new {@link Place} instance.
	 * @throws IllegalStateException
	 *             if the specified name already exists within this world.
	 */
	public Place createPlace(String name, String article, String description) {
		assert (name != null);
		assert (article != null);
		assert (description != null);
		if (isNameUsed(name)) {
			throw new IllegalStateException(
					"Construction of a new place named \""
							+ name
							+ "\" failed because the specified name already exists");
		}
		Place newPlace = new Place(this, name, article, description);
		f_keyToPlace.put(name.toUpperCase(), newPlace);
		return newPlace;
	}

	/**
	 * A non-null mutable string message.
	 */
	private StringBuilder f_message;

	/**
	 * A String holding the operating system-specific line separator obtained
	 * from the system properties.
	 */
	public static String LINESEP = System.getProperty("line.separator");

	/**
	 * Returns the current message associated with this world.
	 * 
	 * @return The world's current message.
	 */
	public String getMessage() {
		return f_message.toString();
	}

	/**
	 * Sets the current message associated with this world to the specified
	 * message. Any previous contents are lost.
	 * 
	 * @param message
	 *            a message.
	 */
	public void setMessage(String message) {
		clearMessage();
		addToMessage(message);
	}

	/**
	 * Appends a message to the world's current message.
	 * 
	 * @param message
	 *            The new message to add to the world's current message.
	 */
	public void addToMessage(String message) {
		if (message == null)
			return;
		f_message.append(message + LINESEP);
	}

	/**
	 * Appends a blank line to the world's current message.
	 */
	public void addToMessage() {
		f_message.append(LINESEP);
	}

	private void clearMessage() {
		f_message = new StringBuilder();
	}

	/**
	 * The set of observers for this world. Notified when this world has changed
	 * in some interesting way.
	 * 
	 * @see #addObserver(IWorldObserver)
	 * @see #removeObserver(IWorldObserver)
	 * @see #getObservers()
	 * @see #notifyObservers()
	 */
	private final Set<IWorldObserver> f_observers = new HashSet<IWorldObserver>();

	/**
	 * Gets a copy of the set of all observers of this world.
	 * 
	 * @return a copy of the set of all observers of this world.
	 */
	public Set<IWorldObserver> getObservers() {
		return new HashSet<IWorldObserver>(f_observers); // defensive copy
	}

	/**
	 * Adds an observer to be notified when the world has changed in some
	 * interesting way.
	 * 
	 * @param observer
	 *            the object to notify of changes to this world.
	 */
	public void addObserver(IWorldObserver observer) {
		if (observer == null)
			return;
		f_observers.add(observer);
	}

	/**
	 * Removes an observer from this world. Has no effect if the specified
	 * observer was not previously added as an observer.
	 * 
	 * @param observer
	 *            the object to stop notifying of changes to this world.
	 */
	public void removeObserver(IWorldObserver observer) {
		if (observer == null)
			return;
		f_observers.remove(observer);
	}

	/**
	 * Directs that the player's current turn is complete and that an update
	 * notification should be sent to any observing views. This method erases
	 * the world's current message after view notification is completed.
	 */
	public void turnOver() {
		notifyObservers();
		clearMessage();
	}

	/**
	 * Notifies all observers that the world has changed in some interesting
	 * way.
	 */
	private void notifyObservers() {
		for (IWorldObserver observer : f_observers) {
			observer.update(this);
		}
	}

	/**
	 * Flags if the game is over or not. Its value is <code>true</code> if the
	 * game is over, <code>false</code> otherwise.
	 * 
	 * @see #isGameOver()
	 * @see #setGameOver()
	 */
	private boolean f_gameOver = false;

	/**
	 * Reports if the game is over or not.
	 * 
	 * @return <code>true</code> if the game is over, <code>false</code>
	 *         otherwise.
	 */
	public boolean isGameOver() {
		return f_gameOver;
	}

	/**
	 * Notifies this world that the game is over. Will cause all subsequent
	 * calls to {@link #isGameOver()} to return <code>true</code>.
	 */
	public void setGameOver() {
		f_gameOver = true;
	}
}
