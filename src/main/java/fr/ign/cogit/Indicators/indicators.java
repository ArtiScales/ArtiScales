package fr.ign.cogit.Indicators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class indicators {
	String mUPSimu;
	String zipCode;
	String selection;
	String simPLUSimu;
	File rootFile;
	boolean firstLine = true;

	/**
	 * getters of the simulation's characteristics
	 * @param fileRef a building file to get the general informations of
	 * @return the name of the MUP-City's output
	 */
 public String getMupSimu(File fileRef){
	 mUPSimu = fileRef.getParentFile().getParentFile().getParentFile().getParentFile().getName();
	 return mUPSimu;
 }
	/**
	 * getters of the simulation's characteristics
	 * @param fileRef a building file to get the general informations of
	 * @return the name of the SimPLU's simulation
	 */
 public String getsimPLUSimu(File fileRef){
		simPLUSimu = fileRef.getParentFile().getName();
		return simPLUSimu;
 }
	/**
	 * getters of the simulation's characteristics
	 * @param fileRef a building file to get the general informations of
	 * @return the name of the selection's methods
	 */
 public String getSelection(File fileRef){
		selection = fileRef.getParentFile().getParentFile().getName();
		return selection;
}
 
	/**
	 * getters of the simulation's characteristics
	 * @param fileRef a building file to get the general informations of
	 * @return the zipCode number
	 */
 public String getZipCode(File fileRef){
		zipCode = fileRef.getParentFile().getParentFile().getParentFile().getName();
		return zipCode;
}

 /**
  * automatically put all the simu chararcteristic's names into the state variables
  * @param listFile
  */
	protected void putSimuNames(File f) {
		simPLUSimu = f.getName();
		selection = f.getParentFile().getName();
		zipCode = f.getParentFile().getParentFile().getName();
		mUPSimu = f.getParentFile().getParentFile().getParentFile().getName();
		rootFile = f.getParentFile().getParentFile().getParentFile().getParentFile();
	}
	
	protected String getInfoSimuCsv(){
	return(	mUPSimu + "," + zipCode + "," + selection + "," + simPLUSimu + ",");
	}
 
	public void toGenCSV(String line) throws IOException {
		File fileName = new File(rootFile, "results.csv");

		FileWriter writer = new FileWriter(fileName, true);

		writer.append(line);
		writer.append("\n");
		writer.close();
	}
	
	public void toCSV(File f,String name,String fLine, String line) throws IOException {
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
