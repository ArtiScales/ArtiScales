package fr.ign.cogit.util;

import fr.ign.cogit.simplu3d.util.SimpluParameters;
import fr.ign.parameters.Parameters;

/**
 * 
 * Only a temporary patch
 * 
 * @author mbrasebin
 *
 */
@Deprecated 
public class SimpluParametersXML implements SimpluParameters{
	
	private Parameters p;
	
	public SimpluParametersXML(Parameters p) {
		this.p = p;
	}

	@Override
	public Object get(String name) {
		return p.get(name);
	}

	@Override
	public String getString(String name) {
		return p.getString(name);
	}

	@Override
	public boolean getBoolean(String name) {
		return p.getBoolean(name);
	}

	@Override
	public double getDouble(String name) {
		return p.getDouble(name);
	}

	@Override
	public int getInteger(String name) {
		return p.getInteger(name);
	}

	@Override
	public float getFloat(String name) {
		return p.getFloat(name);
	}

	@Override
	public void set(String name, Object value) {
		 p.set(name, value);
	}
	
public static Parameters emptyParam(Parameters p) {
	p.entry = null;
	return p;
	}

}
