package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;

public abstract class Indicators {
	SimpluParametersJSON p;
	protected File rootFile, paramFolder, mapStyle, mupOutputFile, parcelDepotGenFile, simPLUDepotGenFile, indicFile, mapDepotFile, graphDepotFile;
	protected String scenarName, variantName, echelle, indicName;

	boolean firstLineGen = true;
	boolean firstLineSimu = true;
	boolean particularExists = false;

	public Indicators(SimpluParametersJSON p, File rootfile, String scenarname, String variantname, String indicName) throws Exception {
		this.p = p;
		this.rootFile = rootfile;
		this.scenarName = scenarname;
		this.variantName = variantname;
		this.paramFolder = new File(rootFile, "paramFolder");
		// we're not sure what's the name of MUp-City's outputs
		for (File f : (new File(rootFile, "MupCityDepot/" + scenarName + "/" + variantName + "/")).listFiles()) {
			if (f.getName().endsWith(".shp")) {
				this.mupOutputFile = f;
				break;
			}
		}
		this.parcelDepotGenFile = new File(rootFile, "ParcelSelectionDepot/" + scenarName + "/" + variantName + "/parcelGenExport.shp");
		this.simPLUDepotGenFile = new File(rootFile, "SimPLUDepot/" + scenarName + "/" + variantName + "/TotBatSimuFill.shp");
		if (!simPLUDepotGenFile.exists() && !scenarname.equals("") && !variantname.equals("")) {
			FromGeom.mergeBatis(simPLUDepotGenFile.getParentFile());
		}
		indicFile = new File(rootFile, "indic/" + indicName + "/" + scenarName + "/" + variantName);
		indicFile.mkdirs();
		mapStyle = new File(rootFile, "mapStyle");
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

	public File getBuildingTotalFile() {
		return simPLUDepotGenFile;
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
		return ("nameScenar,nameVariant,");
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

	public File joinStatoBTHCommunnities(String nameFileToJoin) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		ShapefileDataStore communitiesOGSDS = new ShapefileDataStore((new File(rootFile, "/dataGeo/old/communities.shp")).toURI().toURL());
		SimpleFeatureCollection communitiesOG = communitiesOGSDS.getFeatureSource().getFeatures();
		File result = joinStatToBhTSFC(communitiesOG, new File(indicFile, nameFileToJoin), new File(indicFile, "commStat.shp"));
		communitiesOGSDS.dispose();
		return result;
	}

	public File joinStatToBhTSFC(SimpleFeatureCollection collec, File statFile, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("communities");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("SDPTot", Double.class);
		sfTypeBuilder.add("iniDens", Double.class);
		sfTypeBuilder.add("avDensite", Double.class);
		sfTypeBuilder.add("SDDensite", Double.class);
		sfTypeBuilder.add("avSDPpHU", Double.class);
		sfTypeBuilder.add("sdSDPpHU", Double.class);
		sfTypeBuilder.add("difObjDens", Double.class);
		sfTypeBuilder.add("nbBuilding", Double.class);
		sfTypeBuilder.add("nbHU", Double.class);
		sfTypeBuilder.add("difObjHU", Double.class);
		sfTypeBuilder.add("nbDetach", Double.class);
		sfTypeBuilder.add("nbSmall", Double.class);
		sfTypeBuilder.add("nbFamH", Double.class);
		sfTypeBuilder.add("nbSmallBk", Double.class);
		sfTypeBuilder.add("nbMidBk", Double.class);
		sfTypeBuilder.add("nbU", Double.class);
		sfTypeBuilder.add("nbAU", Double.class);
		sfTypeBuilder.add("nbNC", Double.class);
		sfTypeBuilder.add("nbCentr", Double.class);
		sfTypeBuilder.add("nbBanl", Double.class);
		sfTypeBuilder.add("nbPeriU", Double.class);
		sfTypeBuilder.add("nbRur", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		SimpleFeatureIterator it = collec.features();

		try {
			while (it.hasNext()) {
				SimpleFeature featCity = it.next();
				String insee = (String) featCity.getAttribute("DEPCOM");
				CSVReader stat = new CSVReader(new FileReader(statFile), ',', '\0');
				String[] firstLine = stat.readNext();
				int inseeP = 0, SDPTotP = 0, iniDens = 0, avDensiteP = 0, SDDensiteP = 0, avSDPpHUP = 0, sdSDPpHUP = 0, difObjDensP = 0,
						nbBuildingP = 0, nbHUP = 0, difObjHUP = 0, nbSmallP = 0, nbDetachP = 0, nbFamHP = 0, nbSmallBkP = 0, nbMidBkP = 0, nbUP = 0,
						nbAUP = 0, nbNCP = 0, nbCentrP = 0, nbBanlP = 0, nbPeriUP = 0, nbRurP = 0;
				for (int i = 0; i < firstLine.length; i++) {
					switch (firstLine[i]) {
					case "code":
						inseeP = i;
						break;
					case "SDPTot":
						SDPTotP = i;
						break;
					case "initial_densite":
						iniDens = i;
						break;
					case "average_densite":
						avDensiteP = i;
						break;
					case "standardDev_densite":
						SDDensiteP = i;
						break;
					case "diff_objectifSCOT_densite":
						difObjDensP = i;
						break;
					case "average_SDP_per_HU":
						avSDPpHUP = i;
						break;
					case "standardDev_SDP_per_HU":
						sdSDPpHUP = i;
						break;
					case "nb_building":
						nbBuildingP = i;
						break;
					case "nb_housingUnit":
						nbHUP = i;
						break;
					case "diff_objectifPLH_housingUnit":
						difObjHUP = i;
						break;
					case "nbHU_detachedHouse":
						nbDetachP = i;
						break;
					case "nbHU_smallHouse":
						nbSmallP = i;
						break;
					case "nbHU_multiFamilyHouse":
						nbFamHP = i;
						break;
					case "nbHU_smallBlockFlat":
						nbSmallBkP = i;
						break;
					case "nbHU_midBlockFlat":
						nbMidBkP = i;
						break;
					case "nbHU_U":
						nbUP = i;
						break;
					case "nbHU_AU":
						nbAUP = i;
						break;
					case "nbHU_NC":
						nbNCP = i;
						break;
					case "nbHU_centre":
						nbCentrP = i;
						break;
					case "nbHU_banlieue":
						nbBanlP = i;
						break;
					case "nbHU_periUrbain":
						nbPeriUP = i;
						break;
					case "nbHU_rural":
						nbRurP = i;
						break;
					}
				}
				for (String[] l : stat.readAll()) {
					if (l[inseeP].equals(insee)) {
						builder.set("the_geom", featCity.getDefaultGeometry());
						builder.set("INSEE", l[inseeP]);
						builder.set("SDPTot", Double.valueOf(l[SDPTotP]));
						builder.set("iniDens", Double.valueOf(l[iniDens]));
						builder.set("avDensite", Double.valueOf(l[avDensiteP]));
						builder.set("SDDensite", Double.valueOf(l[SDDensiteP]));
						builder.set("avSDPpHU", Double.valueOf(l[avSDPpHUP]));
						builder.set("sdSDPpHU", Double.valueOf(l[sdSDPpHUP]));
						builder.set("difObjDens", Double.valueOf(l[difObjDensP]));
						builder.set("nbBuilding", Double.valueOf(l[nbBuildingP]));
						builder.set("nbHU", Double.valueOf(l[nbHUP]));
						builder.set("difObjHU", Double.valueOf(l[difObjHUP]));
						builder.set("nbDetach", Double.valueOf(l[nbDetachP]));
						builder.set("nbSmall", Double.valueOf(l[nbSmallP]));
						builder.set("nbFamH", Double.valueOf(l[nbFamHP]));
						builder.set("nbSmallBk", Double.valueOf(l[nbSmallBkP]));
						builder.set("nbMidBk", Double.valueOf(l[nbMidBkP]));
						builder.set("nbU", Double.valueOf(l[nbUP]));
						builder.set("nbAU", Double.valueOf(l[nbAUP]));
						builder.set("nbNC", Double.valueOf(l[nbNCP]));
						builder.set("nbCentr", Double.valueOf(l[nbCentrP]));
						builder.set("nbBanl", Double.valueOf(l[nbBanlP]));
						builder.set("nbPeriU", Double.valueOf(l[nbPeriUP]));
						builder.set("nbRur", Double.valueOf(l[nbRurP]));
						result.add(builder.buildFeature(null));
						break;
					}
				}
				stat.close();
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		return Vectors.exportSFC(result, outFile);
	}

	// public File joinStatToBhTSFC(SimpleFeatureCollection collec, File statFile, File outFile)
	// throws IOException, NoSuchAuthorityCodeException, FactoryException {
	// DefaultFeatureCollection result = new DefaultFeatureCollection();
	// SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
	// CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
	// sfTypeBuilder.setName("communities");
	// sfTypeBuilder.setCRS(sourceCRS);
	// sfTypeBuilder.add("the_geom", Polygon.class);
	// sfTypeBuilder.setDefaultGeometry("the_geom");
	// sfTypeBuilder.add("INSEE", String.class);
	// sfTypeBuilder.add("SDPTot", Double.class);
	// sfTypeBuilder.add("iniDens", Double.class);
	// sfTypeBuilder.add("avDensite", Double.class);
	// sfTypeBuilder.add("SDDensite", Double.class);
	// sfTypeBuilder.add("avSDPpHU", Double.class);
	// sfTypeBuilder.add("sdSDPpHU", Double.class);
	// sfTypeBuilder.add("difObjDens", Double.class);
	// sfTypeBuilder.add("nbBuilding", Integer.class);
	// sfTypeBuilder.add("nbHU", Integer.class);
	// sfTypeBuilder.add("difObjHU", Integer.class);
	// sfTypeBuilder.add("nbDetach", Integer.class);
	// sfTypeBuilder.add("nbSmall", Integer.class);
	// sfTypeBuilder.add("nbFamH", Integer.class);
	// sfTypeBuilder.add("nbSmallBk", Integer.class);
	// sfTypeBuilder.add("nbMidBk", Integer.class);
	// sfTypeBuilder.add("nbU", Integer.class);
	// sfTypeBuilder.add("nbAU", Integer.class);
	// sfTypeBuilder.add("nbNC", Integer.class);
	// sfTypeBuilder.add("nbCentr", Integer.class);
	// sfTypeBuilder.add("nbBanl", Integer.class);
	// sfTypeBuilder.add("nbPeriU", Integer.class);
	// sfTypeBuilder.add("nbRur", Integer.class);
	// SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	// SimpleFeatureIterator it = collec.features();
	//
	// try {
	// while (it.hasNext()) {
	// SimpleFeature featCity = it.next();
	// String insee = (String) featCity.getAttribute("DEPCOM");
	// CSVReader stat = new CSVReader(new FileReader(statFile), ',', '\0');
	// String[] firstLine = stat.readNext();
	// int inseeP = 0, SDPTotP = 0, iniDens = 0, avDensiteP = 0, SDDensiteP = 0, avSDPpHUP = 0, sdSDPpHUP = 0, difObjDensP = 0,
	// nbBuildingP = 0, nbHUP = 0, difObjHUP = 0, nbSmallP = 0, nbDetachP = 0, nbFamHP = 0, nbSmallBkP = 0, nbMidBkP = 0, nbUP = 0,
	// nbAUP = 0, nbNCP = 0, nbCentrP = 0, nbBanlP = 0, nbPeriUP = 0, nbRurP = 0;
	// for (int i = 0; i < firstLine.length; i++) {
	// System.out.println(firstLine[i]);
	//
	// switch (firstLine[i]) {
	// case "code":
	// inseeP = i;
	// break;
	// case "SDPTot":
	// SDPTotP = i;
	// break;
	// case "initial_densite":
	// iniDens = i;
	// break;
	// case "average_densite":
	// avDensiteP = i;
	// break;
	// case "standardDev_densite":
	// SDDensiteP = i;
	// break;
	// case "diff_objectifSCOT_densite":
	// difObjDensP = i;
	// break;
	// case "average_SDP_per_HU":
	// avSDPpHUP = i;
	// break;
	// case "standardDev_SDP_per_HU":
	// sdSDPpHUP = i;
	// break;
	// case "nb_building":
	// nbBuildingP = i;
	// break;
	// case "nb_housingUnit":
	// nbHUP = i;
	// break;
	// case "diff_objectifPLH_housingUnit":
	// difObjHUP = i;
	// break;
	// case "nbHU_detachedHouse":
	// nbDetachP = i;
	// break;
	// case "nbHU_smallHouse":
	// nbSmallP = i;
	// break;
	// case "nbHU_multiFamilyHouse":
	// nbFamHP = i;
	// break;
	// case "nbHU_smallBlockFlat":
	// nbSmallBkP = i;
	// break;
	// case "nbHU_midBlockFlat":
	// nbMidBkP = i;
	// break;
	// case "nbHU_U":
	// nbUP = i;
	// break;
	// case "nbHU_AU":
	// nbAUP = i;
	// break;
	// case "nbHU_NC":
	// nbNCP = i;
	// break;
	// case "nbHU_centre":
	// nbCentrP = i;
	// break;
	// case "nbHU_banlieue":
	// nbBanlP = i;
	// break;
	// case "nbHU_periUrbain":
	// nbPeriUP = i;
	// break;
	// case "nbHU_rural":
	// nbRurP = i;
	// break;
	// }
	// }
	//
	// for (String[] l : stat.readAll()) {
	// if (l[inseeP].equals(insee)) {
	// builder.set("the_geom", featCity.getDefaultGeometry());
	// builder.set("INSEE", l[inseeP]);
	// builder.set("SDPTot", Double.valueOf(l[SDPTotP]));
	// builder.set("iniDens", Double.valueOf(l[iniDens]));
	// builder.set("avDensite", Double.valueOf(l[avDensiteP]));
	// builder.set("SDDensite", Double.valueOf(l[SDDensiteP]));
	// builder.set("avSDPpHU", Double.valueOf(l[avSDPpHUP]));
	// builder.set("sdSDPpHU", Double.valueOf(l[sdSDPpHUP]));
	// builder.set("difObjDens", Double.valueOf(l[difObjDensP]));
	// builder.set("nbBuilding", Integer.valueOf(l[nbBuildingP]));
	// builder.set("nbHU", Integer.valueOf(l[nbHUP]));
	// builder.set("difObjHU", Double.valueOf(l[difObjHUP]));
	// builder.set("nbDetach", Integer.valueOf(l[nbDetachP]));
	// builder.set("nbSmall", Integer.valueOf(l[nbSmallP]));
	// builder.set("nbFamH", Integer.valueOf(l[nbFamHP]));
	// builder.set("nbSmallBk", Integer.valueOf(l[nbSmallBkP]));
	// builder.set("nbMidBk", Integer.valueOf(l[nbMidBkP]));
	// builder.set("nbU", Integer.valueOf(l[nbUP]));
	// builder.set("nbAU", Integer.valueOf(l[nbAUP]));
	// builder.set("nbNC", Integer.valueOf(l[nbNCP]));
	// builder.set("nbCentr", Integer.valueOf(l[nbCentrP]));
	// builder.set("nbBanl", Integer.valueOf(l[nbBanlP]));
	// builder.set("nbPeriU", Integer.valueOf(l[nbPeriUP]));
	// builder.set("nbRur", Integer.valueOf(l[nbRurP]));
	// result.add(builder.buildFeature(null));
	// break;
	// }
	// }
	// stat.close();
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// it.close();
	// }
	// return Vectors.exportSFC(result, outFile);
	// }
}
