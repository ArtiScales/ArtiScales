package fr.ign.cogit.Indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.parameters.Parameters;

public abstract class indicators {
	String zipCode;
	Parameters p;
	File rootFile;
	File simuFile;
	boolean firstLineGen = true;
	boolean firstLineIndic = true;

	/**
	 * getter of the scenar's name
	 * 
	 */
	public String getnameScenar() {
		return p.getString("nom");
	}

	/**
	 * getters of the MUP-City's scenario name
	 * 
	 * @return the name of the MUP-City's scenario used
	 */
	public String getMupScenario() {
		return p.getString("nameScenarMup");
	}

	/**
	 * getters of the MUP-City's techical parameters informations
	 * 
	 * @return the name of the MUP-City's scenario used
	 */
	public String getMupTech() {
		return p.getString("nameTechMup");
	}

	/**
	 * getters of the simulation's selections stuff TODO a faire
	 * 
	 * @param fileRef a building file to get the general informations of
	 * @return the name of the selection's methods
	 */
	public String getSelection() {
		
		return simuFile.getName();
	}

	/**
	 * getters of the simulation's characteristics
	 * 
	 * @param fileRef a building file to get the general informations of
	 * @return the zipCode number
	 */
	public String getZipCode(File fileRef) {
		return fileRef.getParentFile().getParentFile().getName();
	}

	/**
	 * pré-format de la première ligne des tableaux. à vocation à être surchargé pour s'adapter aux indicateurs
	 * @return
	 */
	protected String getFirstlineCsv() {
		return (getnameScenar() + ", paramètres techniques MUP-City,ZipCode,type de sélection,");
	}

	/**
	 * Writing on the general .csv situated on the rootFile
	 * @param line : the line to be writted
	 * @param firstline : the first line (can be empty)
	 * @throws IOException
	 */
	public void toGenCSV(String line, String firstline) throws IOException {
		File fileName = new File(rootFile, "results.csv");
		FileWriter writer = new FileWriter(fileName, true);
		// si l'on a pas encore insrit la premiere ligne
		if (firstLineGen) {
			writer.append(firstline);
			writer.append("\n");
			firstLineGen = false;
		}
		
		//on cole les infos du scénario à la première ligne
		line = getnameScenar() + "--" + getMupScenario() + "," + getMupTech() + "," + zipCode + "," + getSelection()
		+ ","+line;
		writer.append(line);
		writer.append("\n");
		writer.close();
	}

	public void toCSV(File f, String name, String fLine, String line) throws IOException {
		File fileName = new File(f, name);
		FileWriter writer = new FileWriter(fileName, !firstLineIndic);
		if (firstLineIndic == true) {
			writer.append(fLine);
			writer.append("\n");
			firstLineIndic = false;
		}
		writer.append(line);
		writer.append("\n");
		writer.close();
	}
}
