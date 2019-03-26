package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.DataUtilities;
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.map.MapRenderer;
import fr.ign.cogit.map.theseMC.SurfParcelFailedMap;
import fr.ign.cogit.map.theseMC.SurfParcelSimulatedMap;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;

public class ParcelStat extends Indicators {

	File parcelOGFile;
	int nbParcelIgnored, nbParcelSimulated, nbParcelSimulFailed;
	double surfParcelIgnored, surfParcelSimulated, surfParcelSimulFailed, surfaceSDPParcelle, surfaceEmpriseParcelle;
	SimpleFeatureCollection preciseParcelCollection;
	String firstLine;

	public ParcelStat(SimpluParametersJSON p, File rootFile, String scenarName, String variantName) throws Exception {
		super(p, rootFile, scenarName, variantName);
		super.indicFile = new File(rootFile, "indic/parcelStat/" + scenarName + "/" + variantName);
		super.indicFile.mkdirs();
		if (!variantName.equals("")) {
			super.mapDepotFile = new File(indicFile, "mapDepot");
			super.mapDepotFile.mkdir();
		}
		parcelOGFile = FromGeom.getParcels(new File(rootFile, "dataGeo"));
		firstLine = "INSEE,nb_parcel_simulated,nb_parcel_simu_failed,surf_parcel_ignored,surf_parcel_simulated,surf_parcel_simulFailed,surface_SDP_parcelle,surface_emprise_parcelle";
	}

	public static void main(String[] args) throws Exception {
		File rootFile = new File("./result0308/");
		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "paramSet/DDense/parameterTechnic.xml"));
		lF.add(new File(rootParam, "paramSet/DDense/parameterScenario.xml"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);

		ParcelStat parc = new ParcelStat(p, rootFile, "DDense", "variante0");

		SimpleFeatureCollection parcelStatSHP = parc.markSimuledParcels();
		parc.caclulateStatParcel();
		parc.caclulateStatBatiParcel();
		parc.writeLine("AllZone", "ParcelStat");
		parc.toString();
		parc.setCountToZero();

		HashMap<String, SimpleFeatureCollection> commParcel = Vectors.divideSFCIntoPart(parcelStatSHP, "INSEE");

		for (String city : commParcel.keySet()) {
			System.out.println("ville " + city);
			parc.caclulateStatParcel(commParcel.get(city));
			parc.caclulateStatBatiParcel(commParcel.get(city));
			parc.writeLine(city, "ParcelStat");
			parc.toString();
			parc.setCountToZero();
		}
		File commStatFile = parc.joinStatToCommunities();

		List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();
		MapRenderer surfParcelSimulatedMap = new SurfParcelSimulatedMap(1000, 1000, new File(parc.rootFile, "mapStyle"), commStatFile,
				parc.mapDepotFile);
		allOfTheMaps.add(surfParcelSimulatedMap);
		MapRenderer surfParcelFailedMap = new SurfParcelFailedMap(1000, 1000, new File(parc.rootFile, "mapStyle"), commStatFile, parc.mapDepotFile);
		allOfTheMaps.add(surfParcelFailedMap);

		for (MapRenderer map : allOfTheMaps) {
			map.renderCityInfo();
			map.generateSVG();
		}
	}

	public File joinStatToCommunities() throws NoSuchAuthorityCodeException, IOException, FactoryException {
		ShapefileDataStore communitiesSDS = new ShapefileDataStore((new File(rootFile, "/dataGeo/old/communities.shp")).toURI().toURL());
		SimpleFeatureCollection communitiesOG = communitiesSDS.getFeatureSource().getFeatures();
		File result = joinStatToSFC(communitiesOG, new File(indicFile, "ParcelStat.csv"), new File(indicFile, "commStat.shp"));
		communitiesSDS.dispose();
		return result;
	}

	public File joinStatToSFC(SimpleFeatureCollection collec, File statFile, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("communities");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("nbParcSimu", Integer.class);
		sfTypeBuilder.add("nbParcFail", Integer.class);
		sfTypeBuilder.add("aParcSimu", Double.class);
		sfTypeBuilder.add("aParcFail", Double.class);
		sfTypeBuilder.add("aSDP", Double.class);
		sfTypeBuilder.add("aEmprise", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		SimpleFeatureIterator it = collec.features();

		try {
			while (it.hasNext()) {
				SimpleFeature ftBati = it.next();
				String insee = (String) ftBati.getAttribute("DEPCOM");
				CSVReader stat = new CSVReader(new FileReader(statFile), ',', '\0');
				String[] firstLine = stat.readNext();
				int inseeP = 0, nbParcSimuP = 0, nbParcFailP = 0, aParcSimuP = 0, aParcFailP = 0, surface_SDP_parcelleP = 0,
						surface_emprise_parcelleP = 0;
				for (int i = 0; i < firstLine.length; i++) {
					switch (firstLine[i]) {
					case "INSEE":
						inseeP = i;
						break;
					case "nb_parcel_simulated":
						nbParcSimuP = i;
						break;
					case "surf_parcel_ignored":
						nbParcFailP = i;
						break;
					case "surf_parcel_simulated":
						aParcSimuP = i;
						break;
					case "surf_parcel_simulFailed":
						aParcFailP = i;
						break;
					case "surface_SDP_parcelle":
						surface_SDP_parcelleP = i;
						break;
					case "surface_emprise_parcelle":
						surface_emprise_parcelleP = i;
						break;
					}
				}
				for (String[] l : stat.readAll()) {
					if (l[inseeP].equals(insee)) {
						builder.set("the_geom", ftBati.getDefaultGeometry());
						builder.set("INSEE", insee);
						builder.set("nbParcSimu", l[nbParcSimuP]);
						builder.set("nbParcFail", l[nbParcFailP]);
						builder.set("aParcSimu", Double.valueOf(l[aParcSimuP]));
						builder.set("aParcFail", Double.valueOf(l[aParcFailP]));
						builder.set("aSDP", Double.valueOf(l[surface_SDP_parcelleP]));
						builder.set("aEmprise", Double.valueOf(l[surface_emprise_parcelleP]));
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

	public String toString() {
		String result = "nbParcelIgnored : " + nbParcelIgnored + ", nbParcelSimulated : " + nbParcelSimulated + ", nbParcelSimulFailed : "
				+ nbParcelSimulFailed + ", surfParcelIgnored : " + surfParcelIgnored + ", surfParcelSimulated : " + surfParcelSimulated
				+ ", surfParcelSimulFailed : " + surfParcelSimulFailed + ", surfaceSDPParcelle : " + surfaceSDPParcelle
				+ ", surfaceEmpriseParcelle : " + surfaceEmpriseParcelle;
		System.out.println(result);
		return result;
	}

	public String writeLine(String geoEntity, String nameFile) throws IOException {
		String result = geoEntity + "," + nbParcelSimulated + "," + nbParcelSimulFailed + "," + surfParcelIgnored + "," + surfParcelSimulated + ","
				+ surfParcelSimulFailed + "," + surfaceSDPParcelle + "," + surfaceEmpriseParcelle;
		toGenCSV(nameFile, firstLine, result);
		return result;
	}

	public void caclulateStatBatiParcel() throws IOException {
		File parcelStatShapeFile = new File(indicFile, "parcelStatted.shp");
		if (!parcelStatShapeFile.exists()) {
			markSimuledParcels();
		}

		ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelStatShapeFile.toURI().toURL());
		SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
		caclulateStatBatiParcel(parcelSimuled);
		parcelSimuledSDS.dispose();

	}

	public void caclulateStatBatiParcel(SimpleFeatureCollection parcelSimuled) throws IOException {

		ShapefileDataStore batiSDS = new ShapefileDataStore(simPLUDepotGenFile.toURI().toURL());
		SimpleFeatureCollection batiColl = batiSDS.getFeatureSource().getFeatures();

		SimpleFeatureIterator itParcel = parcelSimuled.features();

		try {
			while (itParcel.hasNext()) {
				SimpleFeature ft = itParcel.next();
				if (((String) ft.getAttribute("DoWeSimul")).equals("simulated")) {
					SimpleFeatureIterator batiIt = Vectors.snapDatas(batiColl, (Geometry) ft.getDefaultGeometry()).features();
					try {
						while (batiIt.hasNext()) {
							SimpleFeature ftBati = batiIt.next();
							if (((Geometry) ftBati.getDefaultGeometry()).intersects((Geometry) ft.getDefaultGeometry())) {
								this.surfaceSDPParcelle = surfaceSDPParcelle + (double) ftBati.getAttribute("SDPShon");
								this.surfaceEmpriseParcelle = surfaceEmpriseParcelle + (double) ftBati.getAttribute("SurfaceSol");
							}
						}
					} catch (Exception problem) {
						problem.printStackTrace();
					} finally {
						batiIt.close();
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}

		batiSDS.dispose();
	}

	public void caclulateStatParcel() throws IOException {
		File parcelStatShapeFile = new File(indicFile, "parcelStatted.shp");
		if (!parcelStatShapeFile.exists()) {
			markSimuledParcels();
		}
		ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelStatShapeFile.toURI().toURL());
		SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
		caclulateStatParcel(parcelSimuled);
		parcelSimuledSDS.dispose();
	}

	public void caclulateStatParcel(SimpleFeatureCollection parcelSimuled) throws IOException {

		SimpleFeatureIterator itParcel = parcelSimuled.features();

		try {
			while (itParcel.hasNext()) {

				SimpleFeature ft = itParcel.next();
				switch ((String) ft.getAttribute("DoWeSimul")) {
				case "noSelection":
					surfParcelIgnored = surfParcelIgnored + ((Geometry) ft.getDefaultGeometry()).getArea();
					nbParcelIgnored++;
					break;
				case "simulated":
					surfParcelSimulated = surfParcelSimulated + ((Geometry) ft.getDefaultGeometry()).getArea();
					nbParcelSimulated++;
					break;
				case "simuFailed":
					surfParcelSimulFailed = surfParcelSimulFailed + ((Geometry) ft.getDefaultGeometry()).getArea();
					nbParcelSimulFailed++;
					break;
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}
	}

	/**
	 * mark the parcels that has been selected or not (noSelection) and where a building has been simulated (simulated) or not (simuFailed)
	 * 
	 * @return the newly marked parcel shapeFile
	 * @throws IOException
	 */
	public SimpleFeatureCollection markSimuledParcels() throws IOException {

		ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelDepotGenFile.toURI().toURL());
		SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator itParcel = parcelSimuled.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		try {
			while (itParcel.hasNext()) {
				SimpleFeature ft = itParcel.next();
				String field = "noSelection";
				if (isParcelReallySimulated(ft)) {
					field = "simulated";
				} else if (ft.getAttribute("DoWeSimul").equals("true")) {
					field = "simuFailed";
				}
				ft.setAttribute("DoWeSimul", field);
				result.add(ft);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}
		Vectors.exportSFC(result, new File(indicFile, "parcelStatted.shp"));

		parcelSimuledSDS.dispose();

		return DataUtilities.collection(result.collection());
	}

	/**
	 * this method aims to select the simuled parcels, the parcel that havent been selected and if no building have been simulated on the selected and/or cuted parcel, get the
	 * older ones. This is not finished nor working
	 * 
	 * @return
	 * @throws IOException
	 */
	public SimpleFeatureCollection reuniteParcelOGAndSimuled() throws IOException {
		DefaultFeatureCollection reuniteParcel = new DefaultFeatureCollection();

		ShapefileDataStore parcelOGSDS = new ShapefileDataStore(parcelOGFile.toURI().toURL());
		SimpleFeatureCollection parcelOG = parcelOGSDS.getFeatureSource().getFeatures();
		List<String> oGCode = ParcelFonction.getCodeParcels(parcelOG);

		ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelDepotGenFile.toURI().toURL());
		SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
		List<String> simuledCode = ParcelFonction.getCodeParcels(parcelSimuled);

		List<String> intactParcels = new ArrayList<String>();
		List<String> cuttedButIntactParcels = new ArrayList<String>();
		List<String> changedParcels = new ArrayList<String>();

		List<String> simuledParcels = new ArrayList<String>();

		for (String simuC : simuledCode) {
			if (oGCode.contains(simuC)) {
				intactParcels.add(simuC);
			} else {
				changedParcels.add(simuC);
			}
		}

		System.out.println("list made");
		changedP: for (String changedParcel : simuledCode) {
			SimpleFeatureIterator itParcel = parcelSimuled.features();
			try {
				while (itParcel.hasNext()) {
					SimpleFeature ft = itParcel.next();
					String codeTmp = (String) ft.getAttribute("CODE");
					if (codeTmp.equals(changedParcel)) {
						System.out.println("we in");
						// no construction has been simulated in this parcel
						if (isParcelReallySimulated(ft)) {
							simuledParcels.add(codeTmp);
						} else {
							cuttedButIntactParcels.add(codeTmp);
						}
						continue changedP;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				itParcel.close();
			}
		}
		System.out.println("isolated problematic parcels");
		DefaultFeatureCollection toMergeIftouch = new DefaultFeatureCollection();
		SimpleFeatureIterator itParcel = parcelSimuled.features();
		try {
			while (itParcel.hasNext()) {
				SimpleFeature f = itParcel.next();
				if (cuttedButIntactParcels.contains((String) f.getAttribute("CODE"))) {
					toMergeIftouch.add(f);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}

		Vectors.exportSFC(toMergeIftouch, new File("/tmp/toMergeIfTouch.shp"));

		SimpleFeatureIterator ItToMergeIftouch = toMergeIftouch.features();

		try {
			while (ItToMergeIftouch.hasNext()) {
				SimpleFeature f = ItToMergeIftouch.next();
				Geometry aggregate = mergeIfTouch((Geometry) f.getDefaultGeometry(), toMergeIftouch);

				// find attribute infos
				SimpleFeatureIterator getAttributeIt = Vectors.snapDatas(parcelOG, aggregate).features();
				try {
					while (getAttributeIt.hasNext()) {
						SimpleFeature model = getAttributeIt.next();
						if (((Geometry) model.getDefaultGeometry()).intersects(aggregate)) {
							SimpleFeatureBuilder sfbuild = FromGeom.setSFBParcelWithFeat(model);
							sfbuild.set(model.getFeatureType().getGeometryDescriptor().getName().toString(), aggregate);
							reuniteParcel.add(sfbuild.buildFeature(null));
							break;
						}

					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					getAttributeIt.close();
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			ItToMergeIftouch.close();
		}

		Vectors.exportSFC(reuniteParcel, new File("/tmp/unitedParcels.shp"));

		return toMergeIftouch;

		// maybe some created parcels are made of OG parcels. We then have to make some particular stuff
	}

	/**
	 * This method recursively add geometries to a solo one if they touch each other not sure this is working
	 * 
	 * @param geomIn
	 * @param df
	 * @return
	 * @throws IOException
	 */
	public Geometry mergeIfTouch(Geometry geomIn, DefaultFeatureCollection df) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		result.addAll(df.collection());

		SimpleFeatureIterator features = df.features();

		Geometry aggreg = geomIn;

		try {
			while (features.hasNext()) {
				SimpleFeature f = features.next();
				Geometry geomTemp = (((Geometry) f.getDefaultGeometry()));
				if (geomIn.intersects(geomTemp) && !geomIn.equals(geomTemp)) {
					result.remove(f);
					aggreg = Vectors.unionGeom(geomIn, geomTemp);
					aggreg = mergeIfTouch(aggreg, result);
					break;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			features.close();
		}
		return aggreg;

	}

	/**
	 * for each parcel, set the already existing field "IsBuild" if a new building has been simulated on this parcel
	 * 
	 * @throws Exception
	 */
	public boolean isParcelReallySimulated(SimpleFeature parcel) throws Exception {
		File simuBuildFiles = new File(super.rootFile + "/SimPLUDepot" + "/" + scenarName + "/" + variantName + "/TotBatSimuFill.shp");
		ShapefileDataStore batiSDS = new ShapefileDataStore(simuBuildFiles.toURI().toURL());
		SimpleFeatureCollection batiColl = batiSDS.getFeatureSource().getFeatures();

		Geometry parcelGeometry = (Geometry) parcel.getDefaultGeometry();

		SimpleFeatureCollection snapBatiCollec = Vectors.snapDatas(batiColl, parcelGeometry);
		SimpleFeatureIterator batiFeaturesIt = snapBatiCollec.features();
		try {
			while (batiFeaturesIt.hasNext()) {
				SimpleFeature bati = batiFeaturesIt.next();
				if (((Geometry) bati.getDefaultGeometry()).intersects(parcelGeometry)) {
					return true;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			batiFeaturesIt.close();
		}
		batiSDS.dispose();
		return false;
	}

	public void run() throws IOException, NoSuchAuthorityCodeException, FactoryException {

		Indicators.firstLineGen = true;

		for (String city : FromGeom.getInsee(parcelDepotGenFile)) {
			double surfSelect = 0;
			double surfSelectU = 0;
			double surfSelectAU = 0;
			double surfSelectNC = 0;

			double surfSimulated = 0;
			double surfSimulatedU = 0;
			double surfSimulatedAU = 0;
			double surfSimulatedNC = 0;
			ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelDepotGenFile.toURI().toURL());
			SimpleFeatureIterator parcelFeaturesIt = parcelSDS.getFeatureSource().getFeatures().features();
			try {
				while (parcelFeaturesIt.hasNext()) {
					SimpleFeature feature = parcelFeaturesIt.next();
					if (city.equals((String) feature.getAttribute("INSEE"))) {
						if (((String) feature.getAttribute("DoWeSimul")).equals("true")) {
							double area = ((Geometry) feature.getDefaultGeometry()).getArea();
							surfSelect = surfSelect + area;
							if ((boolean) feature.getAttribute("U")) {
								surfSelectU = surfSelectU + area;
							} else if ((boolean) feature.getAttribute("AU")) {
								surfSelectAU = surfSelectAU + area;
							} else if ((boolean) feature.getAttribute("NC")) {
								surfSelectNC = surfSelectNC + area;
							}
						}
						if ((boolean) feature.getAttribute("IsBuild")) {
							double area = ((Geometry) feature.getDefaultGeometry()).getArea();
							surfSimulated = surfSimulated + area;
							if ((boolean) feature.getAttribute("U")) {
								surfSimulatedU = surfSimulatedU + area;
							} else if ((boolean) feature.getAttribute("AU")) {
								surfSimulatedAU = surfSimulatedAU + area;
							} else if ((boolean) feature.getAttribute("NC")) {
								surfSimulatedNC = surfSimulatedNC + area;
							}
						}
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				parcelFeaturesIt.close();
			}
			parcelSDS.dispose();
			if (surfSelect > 0) {
				String line = city + "," + surfSelect + "," + surfSelectU + "," + surfSelectAU + "," + surfSelectNC + "," + surfSimulated + ","
						+ surfSimulatedU + "," + surfSimulatedAU + "," + surfSimulatedNC;
				System.out.println(line);
				toGenCSV("parcelStat", firstLine, line);
			}
		}
	}

	public void setCountToZero() {
		nbParcelIgnored = nbParcelSimulated = nbParcelSimulFailed = 0;
		surfParcelIgnored = surfParcelSimulated = surfParcelSimulFailed = surfaceSDPParcelle = surfaceEmpriseParcelle = 0;
	}
}
