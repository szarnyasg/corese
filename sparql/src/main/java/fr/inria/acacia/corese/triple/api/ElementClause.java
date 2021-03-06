package fr.inria.acacia.corese.triple.api;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.triple.parser.Atom;
import fr.inria.acacia.corese.triple.parser.Constant;
import fr.inria.acacia.corese.triple.parser.Variable;

/**
 *  API of Atom for backward engine
 * @author corby
 *
 */
public interface ElementClause {
	
	/**
	 * the Atom is a constant?
	 */
	public boolean isConstant();
	
	/**
	 * the Atom is a variable?
	 */
	public boolean isVariable();
	
	/**
	 * value of the element
	 */
	public Constant getConstant();
	
	public IDatatype getDatatypeValue();
	
	/**
	 * name of the element
	 */
	public Variable getVariable();
	
	public Atom getAtom();
	
	public String getName();
	
}
