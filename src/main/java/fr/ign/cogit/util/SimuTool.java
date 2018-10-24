package fr.ign.cogit.util;

import java.io.FileNotFoundException;
import java.util.List;

import fr.ign.parameters.Parameters;

public class SimuTool {

	public static Parameters getParamFile(List<Parameters> lP, String scenar) throws FileNotFoundException {
	
		for (Parameters p : lP) {
			if (p.getString("nom").equals(scenar)) {
				return p;
			}
		}
	throw new FileNotFoundException("pas de param file correspodnant");	
	}
	
}
