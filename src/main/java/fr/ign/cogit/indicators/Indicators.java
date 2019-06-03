package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

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
import fr.ign.cogit.util.SimuTool;

public abstract class Indicators {
	SimpluParametersJSON p;
	private File rootFile, paramFolder, mupOutputFile, parcelDepotGenFile, simPLUDepotGenFile, mapStyle, graphDepotFolder, indicFile, mapDepotFolder;
	protected String scenarName, variantName, echelle, indicName;

	boolean firstLineGen = true;
	boolean firstLineSimu = true;
	boolean particularExists = false;

	public Indicators(SimpluParametersJSON p, File rootfile, String scenarname, String variantname, String indicName) throws Exception {
		this.p = p;
		this.rootFile = rootfile;
		this.scenarName = scenarname;
		this.variantName = variantname;
		this.setParamFolder(new File(rootFile, "paramFolder"));
		// we're not sure what's the name of MUp-City's outputs
		if ((new File(rootFile, "MupCityDepot/" + scenarName + "/" + variantName + "/")).exists()) {
			for (File f : (new File(rootFile, "MupCityDepot/" + scenarName + "/" + variantName + "/")).listFiles()) {
				if (f.getName().endsWith(".shp")) {
					this.setMupOutputFile(f);
					break;
				}
			}
		}
		this.setParcelDepotGenFile(new File(rootFile, "ParcelSelectionDepot/" + scenarName + "/" + variantName + "/parcelGenExport.shp"));
		this.setSimPLUDepotGenFile(new File(rootFile, "SimPLUDepot/" + scenarName + "/" + variantName + "/TotBatSimuFill.shp"));
		if (!getSimPLUDepotGenFile().exists() && !scenarname.equals("") && !variantname.equals("")) {
			File buildingFile = FromGeom.mergeBatis(getSimPLUDepotGenFile().getParentFile());
			// TODO the nex treatement is made for the simu that puts zones into a wrong order and will be deleted one day
			SimuTool.fixBuildingForZone(buildingFile, new File(rootFile, "/dataRegulation/zoning.shp"), true);
		}
		// if there's a will of saving the infos
		if (scenarname != "") {
			setIndicFile(new File(rootFile, "indic/" + indicName + "/" + scenarName + "/" + variantName));
			getIndicFolder().mkdirs();
			setMapStyle(new File(rootFile, "mapStyle"));
			setMapDepotFolder(new File(getIndicFolder(), "mapDepot"));
			getMapDepotFolder().mkdir();
			setGraphDepotFolder(new File(getIndicFolder(), "graphDepot"));
			getGraphDepotFolder().mkdir();
		}
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
		return getSimPLUDepotGenFile();
	}

	/**
	 * getters of the simulation's selections stuff
	 * 
	 * @param fileRef
	 *            a building file to get the general informations of
	 * @return the name of the selection's methods
	 */
	public String getIndicFolderName() {
		return getIndicFolder().getName();
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
		File fileName = new File(getIndicFolder(), indicName + ".csv");
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

	public File joinStatBTHtoCommunities(String nameFileToJoin) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		ShapefileDataStore communitiesOGSDS = new ShapefileDataStore((new File(rootFile, "/dataGeo/old/communities.shp")).toURI().toURL());
		SimpleFeatureCollection communitiesOG = communitiesOGSDS.getFeatureSource().getFeatures();
		File result = joinStatToBTHCommunities(communitiesOG, new File(getIndicFolder(), nameFileToJoin), new File(getIndicFolder(), "commStat.shp"));
		communitiesOGSDS.dispose();
		return result;
	}

	public File joinStatToBTHCommunities(SimpleFeatureCollection collec, File statFile, File outFile)
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
		sfTypeBuilder.add("empriseTot", Double.class);
		sfTypeBuilder.add("avDensHU", Double.class);
		sfTypeBuilder.add("SDDensHU", Double.class);
		sfTypeBuilder.add("avDensSDP", Double.class);
		sfTypeBuilder.add("SDDensSDP", Double.class);
		sfTypeBuilder.add("avDensEmp", Double.class);
		sfTypeBuilder.add("SDDensEmp", Double.class);
		sfTypeBuilder.add("avSDPpHU", Double.class);
		sfTypeBuilder.add("sdSDPpHU", Double.class);
		sfTypeBuilder.add("difObjDens", Double.class);
		sfTypeBuilder.add("nbBuilding", Double.class);
		sfTypeBuilder.add("nbHU", Double.class);
		sfTypeBuilder.add("objHU", Double.class);
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
				int inseeP = 0, SDPTotP = 0, empriseTotP = 0, avDensiteHUP = 0, SDDensiteHUP = 0, avDensiteSDPP = 0, SDDensiteSDPP = 0,
						avDensiteEmpriseP = 0, SDDensiteEmpriseP = 0, avSDPpHUP = 0, sdSDPpHUP = 0, difObjDensP = 0, nbBuildingP = 0, nbHUP = 0,
						objHUP = 0, difObjHUP = 0, nbSmallP = 0, nbDetachP = 0, nbFamHP = 0, nbSmallBkP = 0, nbMidBkP = 0, nbUP = 0, nbAUP = 0,
						nbNCP = 0, nbCentrP = 0, nbBanlP = 0, nbPeriUP = 0, nbRurP = 0;
				for (int i = 0; i < firstLine.length; i++) {
					switch (firstLine[i]) {
					case "code":
						inseeP = i;
						break;
					case "SDPTot":
						SDPTotP = i;
						break;
					case "empriseTot":
						empriseTotP = i;
						break;
					case "average_densiteHU":
						avDensiteHUP = i;
						break;
					case "standardDev_densiteHU":
						SDDensiteHUP = i;
						break;
					case "average_densiteSDP":
						avDensiteSDPP = i;
						break;
					case "standardDev_densiteSDP":
						SDDensiteSDPP = i;
						break;
					case "average_densiteEmprise":
						avDensiteEmpriseP = i;
						break;
					case "standardDev_densiteEmprise":
						SDDensiteEmpriseP = i;
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
					case "objectifPLH_housingUnit":
						objHUP = i;
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
						builder.set("empriseTot", Double.valueOf(l[empriseTotP]));
						builder.set("avDensHU", Double.valueOf(l[avDensiteHUP]));
						builder.set("SDDensHU", Double.valueOf(l[SDDensiteHUP]));
						builder.set("avDensSDP", Double.valueOf(l[avDensiteSDPP]));
						builder.set("SDDensSDP", Double.valueOf(l[SDDensiteSDPP]));
						builder.set("avDensEmp", Double.valueOf(l[avDensiteEmpriseP]));
						builder.set("SDDensEmp", Double.valueOf(l[SDDensiteEmpriseP]));
						builder.set("avSDPpHU", Double.valueOf(l[avSDPpHUP]));
						builder.set("sdSDPpHU", Double.valueOf(l[sdSDPpHUP]));
						builder.set("difObjDens", Double.valueOf(l[difObjDensP]));
						builder.set("nbBuilding", Double.valueOf(l[nbBuildingP]));
						builder.set("nbHU", Double.valueOf(l[nbHUP]));
						builder.set("objHU", Double.valueOf(l[objHUP]));
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

	public static String makeLabelPHDable(String s) throws FileNotFoundException {
		switch (s) {
		case "buildDensity":
			return "densité de logement simulé par hectare";
		case "nbHU_detachedHouse":
			return "Maison isolée";
		case "nbHU_smallHouse":
			return "Pavillon de lotissement";
		case "nbHU_multiFamilyHouse":
			return "Immeuble d'habitat intermédiaire";
		case "nbHU_smallBlockFlat":
			return "Petit immeuble collectif";
		case "nbHU_midBlockFlat":
			return "Immeuble collectif de taille moyenne";
		case "nbHU_U":
			return "Urbanisable";
		case "nbHU_AU":
			return "À urbaniser";
		case "nbHU_NC":
			return "fermé à l'urbanisation";
		case "nbHU_rural":
			return "rurale";
		case "nbHU_periUrbain":
			return "périurbaine";
		case "nbHU_banlieue":
			return "banlieue";
		case "nbHU_centre":
			return "centre ville";
		case "nbParcelSimulatedU":
		case "nbParcelSimulFailedU":
		case "surfParcelSimulatedU":
		case "surfParcelSimulFailedU":
			return "Urbanisable (U)";
		case "nbParcelSimulatedAU":
		case "nbParcelSimulFailedAU":
		case "surfParcelSimulatedAU":
		case "surfParcelSimulFailedAU":
			return "À Urbaniser (AU)";
		case "surfParcelSimulatedNC":
		case "surfParcelSimulFailedNC":
		case "nbParcelSimulatedNC":
		case "nbParcelSimulFailedNC":
			return "Non Urbanisable (NC)";
		case "nbParcelSimulatedBanlieue":
		case "nbParcelSimulFailedBanlieue":
		case "surfParcelSimulFailedBanlieue":
		case "surfParcelSimulatedBanlieue":
			return "banlieue";
		case "nbParcelSimulatedPeriUrb":
		case "nbParcelSimulFailedPeriUrb":
		case "surfParcelSimulatedPeriUrb":
		case "surfParcelSimulFailedPeriUrb":
			return "périurbaine";
		case "nbParcelSimulatedRural":
		case "nbParcelSimulFailedRural":
		case "surfParcelSimulatedRural":
		case "surfParcelSimulFailedRural":
			return "rurale";
		case "nbParcelSimulatedCentre":
		case "nbParcelSimulFailedCentre":
		case "surfParcelSimulatedCentre":
		case "surfParcelSimulFailedCentre":
			return "centre";
		case "SDPTot":
			return "Surface de plancher totale";
		case "empriseTot":
			return "Emprise totale";
		case "HUpHectareDensity":
			return "Densité de logement par hectare";
		case "EmprisepHectareDensity":
			return "Densité de l'emprise des bâtiments par hectare";
		case "SDPpHectareDensity":
			return "Densité de la surface de plancher par hectare";
		case "eval":
			return "Valeur d'intérêt de la parcelle à être urbanisée selon les évaluations de MUP-City";
		case "simuledFromOriginal":
		case "failedFromOriginal":		
			return "pas de modification";
		case "simuledFromDensification":
		case "failedFromDensification":
			return "densification";
		case "failedFromTotalRecomp":
		case "simuledFromTotalRecomp":
			return "recomposition totale d'une zone";
		case "failedFromZoneCut":
		case "simuledFromZoneCut":
			return "découapge en fonction du zonage";
		}
		throw new FileNotFoundException("name not found");
	}

	public static double round(Double value, int place) {
		return new BigDecimal(value).setScale(place, RoundingMode.HALF_UP).doubleValue();
	}

	public File getRootFile() {
		return rootFile;
	}

	public File getIndicFolder() {
		return indicFile;
	}

	public void setIndicFile(File indicFile) {
		this.indicFile = indicFile;
	}

	public File getMapStyle() {
		return mapStyle;
	}

	public void setMapStyle(File mapStyle) {
		this.mapStyle = mapStyle;
	}

	public File getMapDepotFolder() {
		return mapDepotFolder;
	}

	public void setMapDepotFolder(File mapDepotFile) {
		this.mapDepotFolder = mapDepotFile;
	}

	public File getGraphDepotFolder() {
		return graphDepotFolder;
	}

	public void setGraphDepotFolder(File graphDepotFile) {
		this.graphDepotFolder = graphDepotFile;
	}

	public File getParcelDepotGenFile() {
		return parcelDepotGenFile;
	}

	public void setParcelDepotGenFile(File parcelDepotGenFile) {
		this.parcelDepotGenFile = parcelDepotGenFile;
	}

	public File getSimPLUDepotGenFile() {
		return simPLUDepotGenFile;
	}

	public void setSimPLUDepotGenFile(File simPLUDepotGenFile) {
		this.simPLUDepotGenFile = simPLUDepotGenFile;
	}

	public File getMupOutputFile() {
		return mupOutputFile;
	}

	public void setMupOutputFile(File mupOutputFile) {
		this.mupOutputFile = mupOutputFile;
	}

	public File getParamFolder() {
		return paramFolder;
	}

	public void setParamFolder(File paramFolder) {
		this.paramFolder = paramFolder;
	}
}
