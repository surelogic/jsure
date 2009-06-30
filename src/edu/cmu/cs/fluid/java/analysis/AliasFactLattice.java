/*
 * Created on Jul 14, 2003
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
import edu.cmu.cs.fluid.util.Lattice;

import java.util.Set;
/**
 * Interface for lattices used by a location analysis (a la IWACO) to keep track of
 * facts concerning the aliasing of locations.
 */
@Deprecated
interface AliasFactLattice extends Lattice{
	/**
	 * Returns a new AliasFactLattice which now also holds
	 * a == b to be true in addition to any prior facts.
	 */
	public abstract AliasFactLattice addDoesAlias(SimpleLocation a, SimpleLocation b);
	/**
	 * Returns a new AliasFactLattice which now also holds
	 * a != b to be true in addition to any prior facts.
	 */
	public abstract AliasFactLattice addDoesNotAlias(SimpleLocation a, SimpleLocation b);
	/**
	 * Returns a new AliasFactLattice in which contains a new fact concerning
	 * newer which corresponds to each existing fact concerning older.
	 * 
	 * Because facts are <strong>never</strong> false, the old facts do not need
	 * to be removed.  (But they can be)
	 */
	public abstract AliasFactLattice substitute(SimpleLocation older, SimpleLocation newer);
	/**
	 * Given the facts in the lattice, do SimpleLocations a and b provably alias?
	 */
	public abstract boolean doesAlias(SimpleLocation a, SimpleLocation b);
	/**
	 * Given the facts in the lattice, do SimpleLocations a and b provably not alias?
	 */
	public abstract boolean doesNotAlias(SimpleLocation a, SimpleLocation b);
	
	/**
	 *  Returns the set of all locations which provably alias a, according to the lattice.
	 */
	public abstract Set doesAlias(SimpleLocation a);
	
	/**
	 * Returns the set of all locations which provably do not alias a according to the lattice
	 */
	public abstract Set doesNotAlias(SimpleLocation a);

	public abstract boolean makeClaim(SimpleLocation l, LocationClaim c);

	public interface LocationClaim{
		boolean makeClaim(SimpleLocation l);
	}
	public interface AggregatingClaim extends LocationClaim{
		public AggregatingClaim extend(edu.cmu.cs.fluid.ir.IRNode new_decl);
	}

}