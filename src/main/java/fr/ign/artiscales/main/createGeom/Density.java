package fr.ign.artiscales.main.createGeom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.artiscales.pm.parcelFunction.ParcelState;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;

public class Density {

	// public static void main(String[] args) throws Exception {
	// // String nameFieldLgt = "P12_LOG";
	// // String nameFieldCodeCsv = "COM";
	// // String nameInseeFileOut = "DEPCOM";
	// // File zoningFile = new File("/home/ubuntu/boulot/these/result2903/dataRegulation/zoning.shp");
	// // File nbLgtFile = new File("/home/ubuntu/boulot/these/result2903/dataGeo/base-ic-logement-2012.csv");
	// // File fileToAddInitialDensity = new File("/home/ubuntu/boulot/these/result2903/dataGeo/old/communities.shp");
	// // // filesToAddInitialDensity.add(new File("/home/ubuntu/boulot/these/result2903/dataGeo/communities.shp"));
	// // createCommunitiesWithInitialBrutDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
	// // new File(fileToAddInitialDensity.getParentFile(), fileToAddInitialDensity.getName().replace(".shp", "-densIni.shp")));
	// // fileToAddInitialDensity = new File("/home/ubuntu/boulot/these/result2903/dataGeo/communities.shp");
	// // // filesToAddInitialDensity.add(new File("/home/ubuntu/boulot/these/result2903/dataGeo/communities.shp"));
	// // createCommunitiesWithInitialBrutDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
	// // new File(fileToAddInitialDensity.getParentFile(), fileToAddInitialDensity.getName().replace(".shp", "-densIni.shp")));
	//
	// File parcelFile = new File("/home/ubuntu/boulot/these/result2903/ParcelSelectionDepot/DDense/base/parcelGenExport.shp");
	// File batiFile = new File("/home/ubuntu/boulot/these/result2903/SimPLUDepot/DDense/base/TotBatSimuFill.shp");
	// HashMap<String, Long> parcelMerged = mergeConstructedParcels(parcelFile, batiFile);
	// System.out.println(parcelMerged);
	// }

	/**
	 * compute the brut densities (containing all the extra space of parcels - road and common space)
	 * 
	 * @param areaSFC
	 *            : the zoning plan
	 * @param nameFiledInseeZoning
	 *            : name of the field containing the INSEE number for the zoning file
	 * @param nbHUFile
	 *            : file containing the initial number of HousingUnits
	 * @param nameFieldInseeCsv
	 *            : name of the field containing the INSEE number for the file containing the initial housing units
	 * @param nameFieldHUCSv
	 *            : name of the field containing
	 * 
	 * @return a collection with as key the communities, represented with the INSEE number, and as value the initial density
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static HashMap<String, Double> computeDensities(SimpleFeatureCollection areaSFC, String nameFiledInseeZoning, File nbHUFile,
			String nameFieldInseeCsv, String nameFieldHUCsv) throws NumberFormatException, IOException {
		HashMap<String, Double> iniDensVal = new HashMap<String, Double>();
		SimpleFeatureIterator zonings = areaSFC.features();
		// for every cities
		while (zonings.hasNext()) {
			SimpleFeature zoning = zonings.next();
			String insee = (String) zoning.getAttribute(nameFiledInseeZoning);
			CSVReader csvR = new CSVReader(new FileReader(nbHUFile));
			String[] fline = csvR.readNext();
			double obj = 0;
			double dens = 0;
			int field = 0;
			int code = 0;
			for (int i = 0; i < fline.length; i++) {
				if (fline[i].equals(nameFieldHUCsv)) {
					field = i;
				}
				if (fline[i].equals(nameFieldInseeCsv)) {
					code = i;
				}
			}
			for (String[] line : csvR.readAll()) {
				if (line[code].equals(insee)) {
					obj = obj + Double.valueOf(line[field].replace(",", "."));
				}
			}
			dens = obj / (((Geometry) zoning.getDefaultGeometry()).getArea() / 10000);
			iniDensVal.put(insee, dens);
			csvR.close();
		}
		zonings.close();
		return iniDensVal;
	}

	public static HashMap<String, Double> computeDensities(HashMap<String, Long> listSurfPerCommunity, String nameFiledInseeZoning, File nbHUFile,
			String nameFieldInseeCsv, String nameFieldHUCsv) throws NumberFormatException, IOException {
		HashMap<String, Double> iniDensVal = new HashMap<String, Double>();
		// for every cities
		for (String insee : listSurfPerCommunity.keySet()) {
			CSVReader csvR = new CSVReader(new FileReader(nbHUFile));
			String[] fline = csvR.readNext();
			double obj = 0;
			double dens = 0;
			int field = 0;
			int code = 0;
			for (int i = 0; i < fline.length; i++) {
				if (fline[i].equals(nameFieldHUCsv)) {
					field = i;
				}
				if (fline[i].equals(nameFieldInseeCsv)) {
					code = i;
				}
			}
			for (String[] line : csvR.readAll()) {
				if (line[code].equals(insee)) {
					obj = obj + Double.valueOf(line[field].replace(",", "."));
				}
			}
			dens = obj / (listSurfPerCommunity.get(insee) / 10000);
			iniDensVal.put(insee, dens);
			csvR.close();
		}
		return iniDensVal;
	}

	// public static HashMap<String, Double> computeNetDensities(SimpleFeatureCollection parcelsSFC, String nameFiledInseeParcel, File nbHUFile, String nameFieldInseeCsv,
	// String nameFieldHUCSV) {
	// HashMap<String, Double> iniDensVal = new HashMap<String, Double>();
	// SimpleFeatureIterator parcels = parcelsSFC.features();
	// // for every cities
	// while (parcels.hasNext()) {
	// SimpleFeature parcel = parcels.next();
	// String insee = (String) parcel.getAttribute(nameFiledInseeParcel);
	// CSVReader csvR = new CSVReader(new FileReader(nbHUFile));
	// String[] fline = csvR.readNext();
	// double obj = 0;
	// double dens = 0;
	// int field = 0;
	// int code = 0;
	// for (int i = 0; i < fline.length; i++) {
	// if (fline[i].equals(nameFieldHUCsv)) {
	// field = i;
	// }
	// if (fline[i].equals(nameFieldInseeCsv)) {
	// code = i;
	// }
	// }
	// for (String[] line : csvR.readAll()) {
	// if (line[code].equals(insee)) {
	// obj = obj + Double.valueOf(line[field].replace(",", "."));
	// }
	// }
	// dens = obj / (((Geometry) parcel.getDefaultGeometry()).getArea() / 10000);
	// iniDensVal.put(insee, dens);
	// csvR.close();
	// }
	// parcels.close();
	// return iniDensVal;
	//
	// return null;
	// }

	/**
	 * readymade class to create a communities shapeFile with a new field of the initial brut density
	 * 
	 * @param nameFieldLgt
	 * @param nameFieldCodeCsv
	 * @param nameInseeFileOut
	 * @param parcelFile
	 * @param nbLgtFile
	 * @param fileToAddInitialDensity
	 * @param outFile
	 * @return
	 * @throws Exception
	 */
	public static File createCommunitiesWithInitialNetDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File parcelFile,
			File batiFile, File nbLgtFile, File fileToAddInitialDensity, File outFile) throws Exception {
		String[] nameFileds = { "CODE_DEP", "CODE_COM" };
		return createCommunitiesWithNetDensity(nbLgtFile, nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, parcelFile, nameFileds, batiFile,
				"densNetIni", fileToAddInitialDensity, outFile);
	}

	/**
	 * readymade class to create a communities shapeFile with a new field of brut density containing the initial and the simulated number of housing units
	 * 
	 * @param nameFieldLgt
	 * @param nameFieldCodeCsv
	 * @param nameInseeFileOut
	 * @param parcelFile
	 * @param nbLgtFile
	 * @param fileToAddInitialDensity
	 * @param outFile
	 * @return
	 * @throws Exception
	 */
	public static File createCommunitiesWithNewNetDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File parcelFile,
			File batiFile, File nbLgtFile, File fileToAddInitialDensity, File outFile) throws Exception {
		String[] nameFields = { "INSEE" };
		return createCommunitiesWithNetDensity(nbLgtFile, nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, parcelFile, nameFields, batiFile,
				"densNetNew", fileToAddInitialDensity, outFile);
	}

	/**
	 * readymade class to create a communities shapeFile with a new field of the initial brut density
	 * 
	 * @param nameFieldLgt
	 * @param nameFieldCodeCsv
	 * @param nameInseeFileOut
	 * @param zoningFile
	 * @param nbLgtFile
	 * @param fileToAddInitialDensity
	 * @param outFile
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File createCommunitiesWithInitialBrutDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File zoningFile,
			File nbLgtFile, File fileToAddInitialDensity, File outFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> zoneName = new ArrayList<String>();
		zoneName.add("U");
		zoneName.add("ZC");
		return createCommunitiesWithBrutDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
				zoneName, "densBrtIni", outFile);
	}

	/**
	 * readymade class to create a communities shapeFile with a new field of brut density containing the initial and the simulated number of housing units
	 * 
	 * @param nameFieldLgt
	 * @param nameFieldCodeCsv
	 * @param nameInseeFileOut
	 * @param zoningFile
	 * @param nbLgtFile
	 * @param fileToAddInitialDensity
	 * @param outFile
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File createCommunitiesWithNewBrutDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File zoningFile,
			File nbLgtFile, File fileToAddInitialDensity, File outFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> zoneName = new ArrayList<String>();
		zoneName.add("U");
		zoneName.add("AU");
		zoneName.add("ZC");
		return createCommunitiesWithBrutDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
				zoneName, "densBrtNew", outFile);
	}

	/**
	 * readymade class to create a communities shapeFile with a new field of brut density
	 * 
	 * @param nameFieldLgt
	 * @param nameFieldCodeCsv
	 * @param nameInseeFileOut
	 * @param zoningFile
	 * @param nbLgtFile
	 * @param fileToAddInitialDensity
	 * @param outFile
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File createCommunitiesWithBrutDensity(String nameFieldHU, String nameFieldInseeCsv, String nameInseeFileOut, File zoningFile,
			File nbHUFile, File fileToAddInitialDensity, List<String> zoneName, String typeOfDensFieldName, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

		// step1 : isolate constructible zones and merge them into a single SimpleFeature
		SimpleFeatureCollection zones = mergeConstructedZone(zoningFile, zoneName);
		// step2 calculate the density
		HashMap<String, Double> iniDensVal = computeDensities(zones, "INSEE", nbHUFile, nameFieldInseeCsv, nameFieldHU);

		// step 3 : affect the density of housing units per hectare to a list of administrative file (could either be communities or Iris)

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore inSDS = new ShapefileDataStore(fileToAddInitialDensity.toURI().toURL());
		SimpleFeatureIterator inIt = inSDS.getFeatureSource().getFeatures().features();

		SimpleFeatureType schema = inSDS.getSchema();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName(schema.getName());
		b.setSuperType((SimpleFeatureType) schema.getSuper());
		b.addAll(schema.getAttributeDescriptors());
		b.add(typeOfDensFieldName, Double.class);
		SimpleFeatureType nSchema = b.buildFeatureType();
		while (inIt.hasNext()) {
			SimpleFeature feat = inIt.next();
			SimpleFeature featFin = DataUtilities.reType(nSchema, feat);
			if (iniDensVal.containsKey(feat.getAttribute(nameInseeFileOut))) {

				featFin.setAttribute(typeOfDensFieldName, iniDensVal.get(feat.getAttribute(nameInseeFileOut)));
				result.add(featFin);
			}
		}
		inIt.close();
		return Collec.exportSFC(result, outFile);
	}

	/**
	 * readymade class to create a communities shapeFile with a new field of brut density
	 * 
	 * @param nameFieldLgt
	 * @param nameFieldCodeCsv
	 * @param nameInseeFileOut
	 * @param parcelFile
	 * @param nbLgtFile
	 * @param fileToAddDensity
	 * @param outFile
	 * @return
	 * @throws Exception
	 */
	public static File createCommunitiesWithNetDensity(File nbHUFile, String nameFieldHU, String nameFieldInseeCsv, String nameInseeFileOut,
			File parcelFile, String[] parcelFiledInsee, File batiFile, String typeOfDensFieldName, File fileToAddDensity, File outFile)
			throws Exception {

		// step1 : isolate constructed parcels and merge them into a single SimpleFeature
		HashMap<String, Long> surfPerCommunity = mergeConstructedParcels(parcelFile, parcelFiledInsee, batiFile);
		// step2 calculate the density
		HashMap<String, Double> iniDensVal = computeDensities(surfPerCommunity, "INSEE", nbHUFile, nameFieldInseeCsv, nameFieldHU);
		// step 3 : affect the density of housing units per hectare to a list of administrative file (could either be communities or Iris)
		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore inSDS = new ShapefileDataStore(fileToAddDensity.toURI().toURL());
		SimpleFeatureIterator inIt = inSDS.getFeatureSource().getFeatures().features();
		SimpleFeatureType schema = inSDS.getSchema();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName(schema.getName());
		b.setSuperType((SimpleFeatureType) schema.getSuper());
		b.addAll(schema.getAttributeDescriptors());
		b.add(typeOfDensFieldName, Double.class);
		b.add("DifDObj" + typeOfDensFieldName.subSequence(7, 8), Double.class);
		SimpleFeatureType nSchema = b.buildFeatureType();
		while (inIt.hasNext()) {
			SimpleFeature feat = inIt.next();
			SimpleFeature featFin = DataUtilities.reType(nSchema, feat);
			if (iniDensVal.containsKey(featFin.getAttribute(nameInseeFileOut))) {
				featFin.setAttribute(typeOfDensFieldName, iniDensVal.get(feat.getAttribute(nameInseeFileOut)));
				featFin.setAttribute("DifDObj" + typeOfDensFieldName.subSequence(7, 8),
						((int) feat.getAttribute("objDens") - iniDensVal.get(feat.getAttribute(nameInseeFileOut))));
				result.add(featFin);
			}
		}
		inIt.close();
		return Collec.exportSFC(result, outFile);
	}

	/**
	 * Merge the area of parcel that contains a building
	 * 
	 * not select buildings with a type (it means they are not for housing purposes) and parcels that are 30 times wider than the buildings inside them
	 * 
	 * @param parcelFile
	 * @param fieldInsee
	 * @param batiFile
	 * @return a collection with a sum of area for each community's INSEE code (could be a simpleFeatureCollection too)
	 * @throws IOException
	 * @throws Exception
	 */
	public static HashMap<String, Long> mergeConstructedParcels(File parcelFile, String[] fieldInsee, File batiFile) throws IOException, Exception {
		return mergeConstructedParcels(parcelFile, fieldInsee, batiFile, 30.0);
	}

	public static HashMap<String, Long> mergeConstructedParcels(File parcelFile, String[] fieldInsee, File batiFile, Double ratioParcelOnBuilding)
			throws IOException, Exception {

		HashMap<String, Long> surfPerCommunity = new HashMap<String, Long>();

		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureIterator parcelIt = parcelSDS.getFeatureSource().getFeatures().features();

		ShapefileDataStore batiSDS = new ShapefileDataStore(batiFile.toURI().toURL());
		SimpleFeatureCollection batiSFC = batiSDS.getFeatureSource().getFeatures();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filterBuildingNature = ff.like(ff.property("NATURE"), "");

		String geometryCellPropertyName = batiSFC.getSchema().getGeometryDescriptor().getLocalName();

		// DefaultFeatureCollection tmp = new DefaultFeatureCollection();

		while (parcelIt.hasNext()) {
			SimpleFeature feat = parcelIt.next();
			String insee = "";
			for (String field : fieldInsee) {
				insee = insee + (String) feat.getAttribute(field);
			}
			Filter f = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(feat.getDefaultGeometry()));
			SimpleFeatureCollection batiSorted = batiSFC.subCollection(f).subCollection(filterBuildingNature);
			if (ParcelState.isAlreadyBuilt(batiSorted, feat, -1.0)) {
				// ratio between the area of parcels and building
				double areaBuildings = 0.0;
				SimpleFeatureIterator itBuild = batiSorted.features();
				while (itBuild.hasNext()) {
					areaBuildings = areaBuildings + ((Geometry) itBuild.next().getDefaultGeometry()).getArea();
				}
				itBuild.close();
				if ((((Geometry) feat.getDefaultGeometry()).getArea()) / areaBuildings < ratioParcelOnBuilding) {

					// tmp.add(feat);
					if (surfPerCommunity.containsKey(insee)) {
						Long surfTmp = surfPerCommunity.remove(insee);
						surfTmp = surfTmp + (long) ((Geometry) feat.getDefaultGeometry()).getArea();
						surfPerCommunity.put(insee, surfTmp);
					} else {
						surfPerCommunity.put(insee, (long) ((Geometry) feat.getDefaultGeometry()).getArea());
					}
				}
			}
		}
		parcelIt.close();
		parcelSDS.dispose();
		// Vectors.exportSFC(tmp, new File("/tmp/exportedParcels.shp"));

		return surfPerCommunity;
	}

	/**
	 * Merge all the zones with a main libelle code that are potentially containing housing units
	 * 
	 * Overload with defaults sub classes of zones U and AU that contains industries, activites, equipments, are excluded, which are :
	 * <ul>
	 * <li>X</li>
	 * <li>Y</li>
	 * <li>Z</li>
	 * <li>E</li>
	 * <li>L</li>
	 * </ul>
	 * 
	 * @param zoningFile
	 *            : File containing the zoning plan
	 * @param typeOfZoneToAdd
	 *            : main libelle to add in the constructed zone
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static SimpleFeatureCollection mergeConstructedZone(File zoningFile, String typeOfZoneToAdd)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> string = new ArrayList<String>();
		string.add(typeOfZoneToAdd);
		return mergeConstructedZone(zoningFile, string);
	}

	/**
	 * Merge all the zones with main libelle codes that are potentially containing housing units
	 * 
	 * Overload with defaults sub classes of zones U and AU that contains industries, activites, equipments, are excluded, which are :
	 * <ul>
	 * <li>X</li>
	 * <li>Y</li>
	 * <li>Z</li>
	 * <li>E</li>
	 * <li>L</li>
	 * </ul>
	 * 
	 * @param zoningFile
	 *            : File containing the zoning plan
	 * @param typeOfZoneToAdd
	 *            : main libelles to add in the constructed zone
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static SimpleFeatureCollection mergeConstructedZone(File zoningFile, List<String> typeOfZoneToAdd)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

		String[] tmpTab = { "X", "Y", "Z", "E", "L" };
		return mergeConstructedZone(zoningFile, typeOfZoneToAdd, Arrays.asList(tmpTab));
	}

	/**
	 * Merge all the zones with main libelle codes that are potentially containing housing units and exclude sub_libelle that cannot contain housing units
	 * 
	 * @param zoningFile
	 *            : File containing the zoning plan
	 * @param typeOfZoneToAdd
	 *            : main libelles to add in the constructed zone
	 * @param libelleToExclude
	 *            : list of sub-libellle to exclude
	 * 
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static SimpleFeatureCollection mergeConstructedZone(File zoningFile, List<String> typeOfZoneToAdd, List<String> libelleToExclude)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		ShapefileDataStore zoningSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureIterator zoneIt = zoningSDS.getFeatureSource().getFeatures().features();
		HashMap<String, SimpleFeatureCollection> list = new HashMap<String, SimpleFeatureCollection>();
		while (zoneIt.hasNext()) {
			SimpleFeature feat = zoneIt.next();
			if (typeOfZoneToAdd.contains(feat.getAttribute("TYPEZONE"))
					&& !libelleLookLike(libelleToExclude, (String) feat.getAttribute("LIBELLE"))) {

				String insee = (String) feat.getAttribute("INSEE");
				if (list.containsKey(insee)) {
					DefaultFeatureCollection tmp = new DefaultFeatureCollection();
					tmp.addAll(list.remove(insee));
					tmp.add(feat);
					list.put(insee, tmp.collection());
				} else {
					DefaultFeatureCollection add = new DefaultFeatureCollection();
					add.add(feat);
					list.put(insee, add.collection());
				}
			}
		}
		zoneIt.close();
		zoningSDS.dispose();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("INSEE", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		DefaultFeatureCollection zones = new DefaultFeatureCollection();
		for (String com : list.keySet()) {
			builder.set("the_geom", Geom.unionSFC(list.get(com)));
			builder.set("INSEE", com);
			zones.add(builder.buildFeature(null));
		}
		// Vectors.exportSFC(zones, new File("/tmp/mergeZone.shp"));
		return zones;
	}

	/**
	 * Simple tool to announce is the libelle looks like one in the list Non-dependent to the case
	 * 
	 * @param list
	 *            : list of libelle
	 * @param libelle
	 *            : main libelle
	 * @return true if main libelle is inside the list
	 */
	private static boolean libelleLookLike(List<String> list, String libelle) {
		boolean result = false;
		for (String s : list) {
			if (libelle.toUpperCase().contains(s.toUpperCase()) && !libelle.toUpperCase().startsWith("ZC")) {
				return true;
			}
		}
		return result;
	}
}
