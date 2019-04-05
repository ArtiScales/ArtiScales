package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import fr.ign.analyse.obj.ProjetAnalyse;
import fr.ign.analyse.obj.ScenarAnalyse;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;

public abstract class Indicators {
	SimpluParametersJSON p;
	protected File rootFile, paramFolder, parcelDepotGenFile, simPLUDepotGenFile, indicFile, mapDepotFile;
	protected String scenarName, variantName, echelle;

	static boolean firstLineGen = true;
	static boolean firstLineSimu = true;
	static boolean particularExists = false;
	ScenarAnalyse sA;

	public Indicators(SimpluParametersJSON p, File rootfile, String scenarname, String variantname) throws Exception {
		this.p = p;
		this.rootFile = rootfile;
		this.scenarName = scenarname;
		this.variantName = variantname;
		this.paramFolder = new File(rootFile, "paramFolder");
		this.parcelDepotGenFile = new File(rootFile, "ParcelSelectionDepot/" + scenarName + "/" + variantName + "/parcelGenExport.shp");
		this.simPLUDepotGenFile = new File(rootFile, "SimPLUDepot/" + scenarName + "/" + variantName + "/TotBatSimuFill.shp");
		if (!simPLUDepotGenFile.exists() && !scenarname.equals("") && !variantname.equals("")) {
			FromGeom.mergeBatis(simPLUDepotGenFile.getParentFile());
		}

		// lazy way to get MUP-City's informations
		String strictStr = "St";
		String meanStr = "Moy";
		if (p.getBoolean("strict")) {
			strictStr = "Ba";
		}
		if (p.getBoolean("mean")) {
			meanStr = "Yag";
		}
		sA = new ScenarAnalyse(p.getString("cm"), p.getString("emprise"), p.getString("seuil"), p.getString("data"), "N" + p.getString("N"),
				p.getString("ahpName"), strictStr, meanStr, "seed_" + p.getString("seed"));
	}

	/**
	 * getter of the scenar's name
	 * 
	 */
	public String getnameScenar() {
		return scenarName;
	}

	public String getnameVariant() {
		return variantName;
	}

	/**
	 * getters of the MUP-City's scenario name
	 * 
	 * @return the name of the MUP-City's scenario used
	 */
	public String getMupScenario() {
		return sA.getShortScenarNameWthSeed();
	}

	/**
	 * getters of the MUP-City's technical parameters informations
	 * 
	 * @return the name of the MUP-City's scenario used
	 */
	public String getMupTech() {
		return ((ProjetAnalyse) sA).getNiceName().replace(";", "#");
	}

	/**
	 * getters of the simulation's selections stuff
	 * 
	 * @param fileRef
	 *            a building file to get the general informations of
	 * @return the name of the selection's methods
	 */
	public String getIndicFolderName() {
		return indicFile.getName();
	}

	/**
	 * getters of the simulation's characteristics
	 * 
	 * @param fileRef
	 *            a building file to get the general informations of
	 * @return the zipCode number
	 */
	public String getZipCode(File fileRef) {
		return fileRef.getParentFile().getParentFile().getName();
	}

	/**
	 * pré-format de la première ligne des tableaux. à vocation à être surchargé pour s'adapter aux indicateurs
	 * 
	 * @return
	 */
	protected String getFirstlineCsv() {
		return ("nameScenar, nameVariant,");
	}

	/**
	 * Writing on the general .csv situated on the rootFile
	 * 
	 * @param f
	 *            : where the csv must be saved
	 * @param indicName
	 *            : name of the indicator
	 * @param line
	 *            : the line to be writted
	 * @param firstline
	 *            : the first line (can be empty)
	 * @throws IOException
	 */
	public void toGenCSV(String indicName, String firstline, String line) throws IOException {
		File fileName = new File(indicFile, indicName + ".csv");
		FileWriter writer = new FileWriter(fileName, true);
		// si l'on a pas encore inscrit la premiere ligne
		if (firstLineGen) {
			writer.append(getFirstlineCsv() + firstline);
			writer.append("\n");
			firstLineGen = false;
		}

		// on cole les infos du scénario à la première ligne
		line = getnameScenar() + "," + getnameVariant() + "," + line;
		writer.append(line);
		writer.append("\n");
		writer.close();
	}

	public void toParticularCSV(File f, String name, String fLine, String line) throws IOException {
		File fileName = new File(f, name);

		FileWriter writer = new FileWriter(fileName, particularExists);

		if (particularExists == false) {
			particularExists = true;
		}

		if (firstLineSimu) {
			writer.append(fLine);
			writer.append("\n");
			firstLineSimu = false;
		}
		writer.append(line);
		writer.append("\n");
		writer.close();
	}
}
