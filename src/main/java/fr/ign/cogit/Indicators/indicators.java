package fr.ign.cogit.Indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public abstract class indicators {
	String mUPSimu;
	String zipCode;
	String selection;
	String simPLUSimu;
	File rootFile;
	boolean filling = false;
	boolean firstLine = true;

	/**
	 * getters of the simulation's characteristics
	 * 
	 * @param fileRef
	 *            a building file to get the general informations of
	 * @return the name of the MUP-City's output
	 */
	public String getMupSimu(File fileRef) {
		mUPSimu = fileRef.getParentFile().getParentFile().getParentFile().getName();
		return mUPSimu;
	}

	/**
	 * getters of the simulation's characteristics
	 * 
	 * @param fileRef
	 *            a building file to get the general informations of
	 * @return the name of the SimPLU's simulation
	 */
	public String getsimPLUSimu(File fileRef) {
		simPLUSimu = fileRef.getName();
		return simPLUSimu;
	}

	/**
	 * getters of the simulation's characteristics
	 * 
	 * @param fileRef
	 *            a building file to get the general informations of
	 * @return the name of the selection's methods
	 */
	public String getSelection(File fileRef) {
		selection = fileRef.getParentFile().getName();
		return selection;
	}

	/**
	 * getters of the simulation's characteristics
	 * 
	 * @param fileRef
	 *            a building file to get the general informations of
	 * @return the zipCode number
	 */
	public String getZipCode(File fileRef) {
		zipCode = fileRef.getParentFile().getParentFile().getName();
		return zipCode;
	}

	/**
	 * automatically put all the simu chararcteristic's names into the state
	 * variables
	 * 
	 * @param listFile
	 */
	protected void putSimuNames(File f) {
		if (f.toString().contains("fillingBuildings")) {
			boolean filling = true;
			simPLUSimu = f.getParentFile().getName();
			selection = f.getParentFile().getParentFile().getName();
			zipCode = f.getParentFile().getParentFile().getParentFile().getName();
			mUPSimu = f.getParentFile().getParentFile().getParentFile().getParentFile().getName();
			rootFile = f.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
		} else {
			simPLUSimu = f.getName();
			selection = f.getParentFile().getName();
			zipCode = f.getParentFile().getParentFile().getName();
			mUPSimu = f.getParentFile().getParentFile().getParentFile().getName();
			rootFile = f.getParentFile().getParentFile().getParentFile().getParentFile();
		}
	}

	protected String getInfoSimuCsv() {
		return (mUPSimu + "," + zipCode + "," + selection + "," + simPLUSimu + ",");
	}

	public void toGenCSV(String line) throws IOException {
		File fileName = new File(rootFile, "results.csv");

		FileWriter writer = new FileWriter(fileName, true);

		writer.append(line);
		writer.append("\n");
		writer.close();
	}

	// TODO automatiser la mise des infos fill dans le csv general
	public void toGenCSVFill(String toAdd) throws IOException {
		File fileName = new File(rootFile, "results.csv");
		CSVReader csvReader = new CSVReader(new FileReader(fileName));
		CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName, true));

		List content = csvReader.readAll();
		String[] newRow;
		for (Object object : content) {
			String[] row = (String[]) object;
			for (String s : row) {
				if (row[0].equals(mUPSimu)) {
					if (row[1].equals(zipCode)) {
						if (row[2].equals(selection)) {
							if (row[3].equals(simPLUSimu)) {

							}
						}
					}
				}
			}
		}
		for (Object object : content) {
			String[] row = (String[]) object;
		}

		csvWriter.close();

	}

	public void toCSV(File f, String name, String fLine, String line) throws IOException {
		File fileName = new File(f, name);
		FileWriter writer = new FileWriter(fileName, !firstLine);
		if (firstLine == true) {
			writer.append(fLine);
			firstLine = false;
		}
		writer.append(line);
		writer.append("\n");
		writer.close();
	}
}
