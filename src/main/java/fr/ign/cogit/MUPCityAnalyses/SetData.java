package fr.ign.cogit.MUPCityAnalyses;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.IntersectUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.SelectParcels;
import fr.ign.cogit.SimPLUSimulator;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Chargeur;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Groupe;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;

public class SetData {

	/**
	 * Classe permettant le traitement automatique des données pour une simulation avec MUP-City. Pour une explication complémentaire, merci de se référer à mon travail de thèse et
	 * en particulier à l'annexe concernant les données. Le dossier "rootFileType" présent dans le dossier "src/main/ressource" du présent projet fournit une organisation basique
	 * des dossiers devant comporter les données de base. Pour l'instant, le géocodage doit être réalisé à la main. Il faut donc lancer le code du main jusqu'à la méthode
	 * SortAmenity1part, éfféctuer le géocodage manuel dans les fichiers tmp/sirene-loisir-geocoded.csv, puis lancer la deuxième partie du main depuis la méthode sortAmenity2part.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		File rootFile = new File("/home/mcolomb/donnee/autom/besancon");
		// temp folder
		File tempFile = new File(rootFile, "tmp");
		tempFile.mkdirs();

		// emprise File
		File empriseFile = createEmpriseFile(rootFile, new File(rootFile, "dataIn/admin.csv"));

		File NUFile = new File(rootFile, "/dataOut/NU/");
		NUFile.mkdirs();

		int[] nbDep = { 25, 39, 70 };

		// first part to turn on comment

		// //TODO this is not working
		// geocodeBan("https://api-adresse.data.gouv.fr/", "?q=8 bd du port&postcode=44380");
		// geocodeBan("http://nominatim.openstreetmap.org/search", "q=135+pilkington+avenue,+birmingham&format=xml&polygon=1&addressdetails=1");

		// // Bati
		// prepareBuild(rootFile, nbDep, empriseFile);
		// // Road
		// prepareRoad(rootFile, nbDep, empriseFile);
		//
		// // Hydro
		// prepareHydrography(rootFile, nbDep, empriseFile);
		//
		// // Vegetation
		// prepareVege(rootFile, nbDep, empriseFile);
		// // Amenities
		//
		// // sort the different amenities -first part (before the geocoding
		// sortAmenity1part(rootFile, empriseFile,nbDep);

		// Second part - switch here

		// sortAmenity2part(rootFile, empriseFile);
		//
		// // Train
		// prepareTrain(rootFile, nbDep, empriseFile);
		// // Zones Non Urbanisables
		// makeFullZoneNU(rootFile);
		// //
		// makePhysicNU(rootFile);

	}

	/**
	 * créée une emprise en fonction de la liste des villes contenus dans le fichier adminFile. Le fichier retouré est utilisé pour le découpage des shapefiles provenant de la
	 * BDTopo.
	 * 
	 * @param rootFile:
	 *            dossier principal ou sont entreposées les données
	 * @param adminFile:
	 *            fichier csv listant les villes de notre cas d'étude
	 * @return un shapefile contenant l'emprise de l'étude
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ParseException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File createEmpriseFile(File rootFile, File adminFile) throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {

		ShapefileDataStore geoFlaSDS = new ShapefileDataStore((new File(rootFile, "dataIn/geofla/commune.shp")).toURI().toURL());
		SimpleFeatureCollection geoFlaSFC = geoFlaSDS.getFeatureSource().getFeatures();
		CSVReader listVilleReader = new CSVReader(new FileReader(adminFile));
		DefaultFeatureCollection villeColl = new DefaultFeatureCollection();
		DefaultFeatureCollection emprise = new DefaultFeatureCollection();

		String[] villeNumber = listVilleReader.readNext();
		int numInsee = 0;
		for (int i = 0; i < villeNumber.length; i = i + 1) {
			if (villeNumber[i].contains("INSEE") || villeNumber[i].contains("insee")) {
				numInsee = i;
			}
		}

		for (String[] row : listVilleReader.readAll()) {
			SimpleFeatureIterator geoFlaIt = geoFlaSFC.features();
			try {
				while (geoFlaIt.hasNext()) {
					SimpleFeature feat = geoFlaIt.next();
					if (row[numInsee].equals(feat.getAttribute("INSEE_COM"))) {
						villeColl.add(feat);
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				geoFlaIt.close();
			}
		}

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("enveloppe");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		sfBuilder.add(SelectParcels.unionSFC(villeColl).buffer(3000).getEnvelope());
		SimpleFeature feature = sfBuilder.buildFeature("0");
		emprise.add(feature);

		listVilleReader.close();
		geoFlaSDS.dispose();

		return SelectParcels.exportSFC(emprise.collection(), new File(rootFile, "dataIn/emprise.shp"));
	}

	public static File mergeMultipleBdTopo(File rootFile, String nom, Integer[] listDep)
			throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {
		return mergeMultipleBdTopo(rootFile, nom, listDep, new File("no-enveloppe"));
	}

	public static File mergeMultipleBdTopo(File rootFile, String nom, Integer[] listDep, File empriseFile)
			throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {
		List<File> listFile = new ArrayList<>();
		for (int i = 0; i < listDep.length; i = i + 1) {
			File fileDep = new File(rootFile, String.valueOf(listDep[i]) + "/" + nom + ".shp");
			listFile.add(fileDep);
		}
		return mergeMultipleShp(rootFile, listFile, new File(rootFile, nom + ".shp"), empriseFile, true, false);
	}

	public static File mergeMultipleShp(File rootFile, List<File> listShp, File fileOut, File empriseFile, boolean keepAttributes, boolean discretize)
			throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {
		DefaultFeatureCollection merged = new DefaultFeatureCollection();
		int nId = 0;
		ShapefileDataStore dS = new ShapefileDataStore(listShp.get(0).toURI().toURL());
		SimpleFeatureCollection addeSFC = dS.getFeatureSource().getFeatures();

		int nbAttr = addeSFC.getSchema().getAttributeCount();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.init(addeSFC.getSchema());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		dS.dispose();

		// keep the attributes or not

		for (File f : listShp) {
			ShapefileDataStore dShape = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureCollection ttSFC = dShape.getFeatureSource().getFeatures();
			int nbAttrTemp = nbAttr;
			nbAttr = ttSFC.getSchema().getAttributeCount();
			if (nbAttr != nbAttrTemp) {
				keepAttributes = false;
				System.out.println("Not the same amount of attributes in the shapefile : Output won't have any attributes");
			}
		}

		for (File f : listShp) {
			ShapefileDataStore shpDS = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureIterator addSFCIt = shpDS.getFeatureSource().getFeatures().features();
			try {
				while (addSFCIt.hasNext()) {
					SimpleFeature feat = addSFCIt.next();
					Object[] attr = new Object[0];
					if (keepAttributes) {
						attr = new Object[feat.getAttributeCount() - 1];
						for (int h = 1; h < feat.getAttributeCount(); h = h + 1) {
							attr[h - 1] = feat.getAttribute(h);
						}
					}
					sfBuilder.add((Geometry) feat.getDefaultGeometry());
					SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(nId), attr);
					nId = nId + 1;
					merged.add(feature);
				}

			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				addSFCIt.close();
			}
			shpDS.dispose();
		}
		SimpleFeatureCollection output = merged.collection();
		if (empriseFile.exists()) {
			output = cropSFC(output, empriseFile);
		}
		if (discretize) {
			output = discretizeShp(output);
		}
		return SelectParcels.exportSFC(output, fileOut);
	}

	public static SimpleFeatureCollection discretizeShp(SimpleFeatureCollection input) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		DefaultFeatureCollection dfCuted = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder finalBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		finalBuilder.setName("road");
		finalBuilder.setCRS(sourceCRS);
		finalBuilder.add("the_geom", MultiPolygon.class);
		finalBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureType featureFinalType = finalBuilder.buildFeatureType();
		SimpleFeatureBuilder finalFeatureBuilder = new SimpleFeatureBuilder(featureFinalType);

		SpatialIndexFeatureCollection sifc = new SpatialIndexFeatureCollection(input);
		SimpleFeatureCollection gridFeatures = Grids.createSquareGrid(input.getBounds(), 500).getFeatures();
		SimpleFeatureIterator iterator = gridFeatures.features();
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		int finalId = 0;
		try {
			while (iterator.hasNext()) {
				SimpleFeature featureGrid = iterator.next();
				Geometry gridGeometry = (Geometry) featureGrid.getDefaultGeometry();
				BBOX boundsCheck = ff.bbox(ff.property("the_geom"), featureGrid.getBounds());
				SimpleFeatureIterator chosenFeatIterator = sifc.subCollection(boundsCheck).features();
				Geometry unionGeom = null;
				List<Geometry> list = new ArrayList<>();
				while (chosenFeatIterator.hasNext()) {
					SimpleFeature f = chosenFeatIterator.next();
					Geometry g = (Geometry) f.getDefaultGeometry();

					if (g.intersects(gridGeometry)) {

						list.add(g);
					}
				}
				Geometry coll = gridGeometry.getFactory().createGeometryCollection(list.toArray(new Geometry[list.size()]));
				try {
					Geometry y = coll.union();
					if (y.isValid())
						coll = y;
				} catch (Exception e) {
				}
				unionGeom = IntersectUtils.intersection(coll, gridGeometry);
				try {
					Geometry y = unionGeom.buffer(0);
					if (y.isValid()) {
						unionGeom = y;
					}
				} catch (Exception e) {
				}
				if (unionGeom != null) {
					finalFeatureBuilder.add(unionGeom);
					SimpleFeature feature = finalFeatureBuilder.buildFeature(String.valueOf(finalId++));
					dfCuted.add(feature);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			iterator.close();
		}
		return dfCuted.collection();
	}

	public static Geometry union(Geometry g1, Geometry g2) {
		if (g1 instanceof GeometryCollection) {
			if (g2 instanceof GeometryCollection) {
				return union((GeometryCollection) g1, (GeometryCollection) g2);
			} else {
				List<Geometry> ret = union((GeometryCollection) g1, g2);
				return g1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
			}
		} else {
			if (g2 instanceof GeometryCollection) {
				List<Geometry> ret = union((GeometryCollection) g2, g1);
				return g1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
			} else {
				return g1.intersection(g2);
			}
		}
	}

	private static List<Geometry> union(GeometryCollection gc, Geometry g) {
		List<Geometry> ret = new ArrayList<Geometry>();
		final int size = gc.getNumGeometries();
		for (int i = 0; i < size; i++) {
			Geometry g1 = (Geometry) gc.getGeometryN(i);
			collect(g1.union(g), ret);
		}
		return ret;
	}

	/**
	 * Helper method for {@link #union(Geometry, Geometry) union(Geometry, Geometry)}
	 */
	private static GeometryCollection union(GeometryCollection gc1, GeometryCollection gc2) {
		List<Geometry> ret = new ArrayList<Geometry>();
		final int size = gc1.getNumGeometries();
		for (int i = 0; i < size; i++) {
			Geometry g1 = (Geometry) gc1.getGeometryN(i);
			List<Geometry> partial = union(gc2, g1);
			ret.addAll(partial);
		}
		return gc1.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(ret));
	}

	/**
	 * Adds into the <TT>collector</TT> the Geometry <TT>g</TT>, or, if <TT>g</TT> is a GeometryCollection, every geometry in it.
	 *
	 * @param g
	 *            the Geometry (or GeometryCollection to unroll)
	 * @param collector
	 *            the Collection where the Geometries will be added into
	 */
	private static void collect(Geometry g, List<Geometry> collector) {
		if (g instanceof GeometryCollection) {
			GeometryCollection gc = (GeometryCollection) g;
			for (int i = 0; i < gc.getNumGeometries(); i++) {
				Geometry loop = gc.getGeometryN(i);
				if (!loop.isEmpty())
					collector.add(loop);
			}
		} else {
			if (!g.isEmpty())
				collector.add(g);
		}
	}

	public static SimpleFeatureCollection cropSFC(SimpleFeatureCollection inSFC, File empriseFile) throws MalformedURLException, IOException {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		ShapefileDataStore envSDS = new ShapefileDataStore(empriseFile.toURI().toURL());
		ReferencedEnvelope env = (envSDS.getFeatureSource().getFeatures()).getBounds();
		String geometryPropertyName = inSFC.getSchema().getGeometryDescriptor().getLocalName();
		Filter filter = ff.bbox(ff.property(geometryPropertyName), env);
		envSDS.dispose();
		return inSFC.subCollection(filter);
	}

	public static File extractSireneFromDB(File tempFile, int[] depList, String DB_URL, String USER, String PASS) throws SQLException, IOException {
		File sireneFile = new File(tempFile, "sirene-dep-test.csv");
		Connection conn = null;
		boolean firstLine = true;
		boolean append = false;
		FileWriter writer = new FileWriter(sireneFile, append);
		append = true;
		try {
			Class.forName("org.postgresql.Driver"); // leve une exception si le pilote Postgresql n’est pas installe
			conn = DriverManager.getConnection(DB_URL, USER, PASS); // ouvre une connexion vers la base de donnees data
			Statement state = conn.createStatement();
			// L’objet ResultSet contient le resultat de la requete SQL
			for (int dep : depList) {
				String req = "SELECT * FROM sirn WHERE depet='" + dep + "'";
				ResultSet result = state.executeQuery(req); // execution d’une requete SQL
				ResultSetMetaData resultMeta = result.getMetaData();
				int colCount = resultMeta.getColumnCount();
				if (firstLine) {
					for (int i = 1; i <= colCount; i++) {
						writer.append(resultMeta.getColumnName(i));
						writer.append(";");
					}
					writer.append("\n");
				}
				while (result.next()) {
					for (int i = 1; i <= colCount; i++) {
						writer.append(result.getString(i));
						writer.append(";");
					}
					writer.append("\n");
				}
				firstLine = false;
				result.close();
			}
			state.close();
		} catch (Exception e) {
			e.printStackTrace(); // pour gerer les erreurs (pas de pilote, base inexistante, etc.)
		} finally {

			if (conn != null) {

				conn.close(); // toujours fermer les differentes ressources quand il n’y en as plus besoin
			}

		}
		writer.close();
		return sireneFile;
	}
/**
 * trie les aménités - de SIRENE contenue dans une base de données locale - et de BPE contenue dans le fichier dataIn/sireneBPE/BPE-tot.csv
 * @param rootFile : dossier principal
 * @param empriseFile : shp de l'emprise générale (auto-généré)
 * @param nbDep : liste des départements à prendre en compte
 * @throws Exception
 */
	public static void sortAmenity1part(File rootFile, File empriseFile, int[] nbDep) throws Exception {

		File pointSireneIn = extractSireneFromDB(new File(rootFile, "tmp"), nbDep, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres");
		;

		// tiré du GÉOFLA et jointuré avec la poste pour avoir les codes postaux
		File listVille = new File(rootFile, "dataIn/admin.csv");
		File csvServicesSirene = new File(rootFile, "tmp/siren-Services.csv");
		File csvLoisirsSirene = new File(rootFile, "tmp/siren-Loisirs.csv");

		if (csvLoisirsSirene.exists()) {
			Files.delete(csvLoisirsSirene.toPath());
		}
		if (csvServicesSirene.exists()) {
			Files.delete(csvServicesSirene.toPath());
		}

		CSVReader csvSirenePruned = new CSVReader(new FileReader(preselecGeocode(pointSireneIn, listVille)));
		CSVWriter csvServiceW = new CSVWriter(new FileWriter(csvServicesSirene, true));
		CSVWriter csvLoisirW = new CSVWriter(new FileWriter(csvLoisirsSirene, true));
		String[] firstLine = csvSirenePruned.readNext();
		String[] newFirstLine = new String[firstLine.length + 2];
		for (int k = 0; k < firstLine.length; k = k + 1) {
			newFirstLine[k] = firstLine[k];
		}
		newFirstLine[firstLine.length] = "TYPE";
		newFirstLine[firstLine.length + 1] = "LEVEL";
		csvLoisirW.writeNext(newFirstLine);
		csvServiceW.writeNext(newFirstLine);
		// for the sirene file
		for (String[] row : csvSirenePruned.readAll()) {
			String[] result = new String[102];
			String[] resultOut = sortCatAmen(row[43], row[73]);
			if (!(resultOut[0] == null)) {
				for (int i = 0; i < 100; i = i + 1) {
					result[i] = row[i];
				}
				result[100] = resultOut[1];
				result[101] = resultOut[2];
				switch (resultOut[0]) {
				case "service":
					csvServiceW.writeNext(result);
					break;
				case "loisir":
					csvLoisirW.writeNext(result);
					break;
				}
			}
		}

		csvSirenePruned.close();
		csvServiceW.close();
		csvLoisirW.close();

		// for the BPE file
		File pointBPEIn = new File(rootFile, "dataIn/sireneBPE/BPE-tot.csv");
		File csvServicesBPE = new File(rootFile, "tmp/BPE-Services.csv");
		File csvLoisirsBPE = new File(rootFile, "tmp/BPE-Loisirs.csv");
		File csvTrainsBPE = new File(rootFile, "tmp/BPE-Trains.csv");

		if (csvLoisirsBPE.exists()) {
			Files.delete(csvLoisirsBPE.toPath());
		}
		if (csvServicesBPE.exists()) {
			Files.delete(csvServicesBPE.toPath());
		}
		if (csvTrainsBPE.exists()) {
			Files.delete(csvTrainsBPE.toPath());
		}

		CSVReader csvBPE = new CSVReader(new FileReader(pointBPEIn));
		CSVWriter csvServiceBPE = new CSVWriter(new FileWriter(csvServicesBPE, true));
		CSVWriter csvLoisirBPE = new CSVWriter(new FileWriter(csvLoisirsBPE, true));
		CSVWriter csvTrainBPE = new CSVWriter(new FileWriter(csvTrainsBPE, true));
		String[] firstLineBPE = csvBPE.readNext();
		String[] newFirstLineBPE = new String[firstLineBPE.length + 2];
		for (int k = 0; k < firstLineBPE.length; k = k + 1) {
			newFirstLineBPE[k] = firstLineBPE[k];
		}
		newFirstLineBPE[firstLineBPE.length] = "TYPE";
		newFirstLineBPE[firstLineBPE.length + 1] = "LEVEL";
		csvLoisirBPE.writeNext(newFirstLineBPE);
		csvServiceBPE.writeNext(newFirstLineBPE);
		String[] trainStr = { "NATURE", "X", "Y" };
		csvTrainBPE.writeNext(trainStr);
		ShapefileDataStore envSDS = new ShapefileDataStore(empriseFile.toURI().toURL());
		ReferencedEnvelope env = (envSDS.getFeatureSource().getFeatures()).getBounds();

		for (String[] row : csvBPE.readAll()) {
			String[] result = new String[11];
			if (!(row[6].isEmpty())) {
				Double x = Double.parseDouble((row[6].split(";"))[0]);
				Double y = Double.parseDouble((row[7].split(";"))[0]);
				if (x < env.getMaxX() && x > env.getMinX() && y < env.getMaxY() && y > env.getMinY()) {
					String[] resultOut = sortCatAmen(row[5], null);
					if (!(resultOut[0] == null)) {
						for (int i = 0; i < 9; i = i + 1) {
							result[i] = row[i];
						}
						result[9] = resultOut[1];
						result[10] = resultOut[2];
						String[] resTrain = { result[1], String.valueOf(x), String.valueOf(y) };

						switch (resultOut[0]) {
						case "service":
							csvServiceBPE.writeNext(result);
							break;
						case "loisir":
							csvLoisirBPE.writeNext(result);
							break;
						case "train":
							csvTrainBPE.writeNext(resTrain);
						}
					}
				}
			}
		}
		envSDS.dispose();
		csvBPE.close();
		csvServiceBPE.close();
		csvLoisirBPE.close();
		csvTrainBPE.close();

		createPointFromCsv(csvServicesBPE, new File(rootFile, "tmp/BPE-Services.shp"), empriseFile, true);
		createPointFromCsv(csvLoisirsBPE, new File(rootFile, "tmp/BPE-Loisirs.shp"), empriseFile, false);
		createPointFromCsv(csvTrainsBPE, new File(rootFile, "dataOut/trainAutom.shp"), empriseFile, false);

		// je n'ai pour l'instant pas automatisé le géocodage - trop de temps et vieux problème de proxy
		// geocodeBan(targetURL, urlParameters)

		// TODO
		// geocodeBan()
		// File loisirTemp1 = createPointFromCsv(csvLoisirs, pointLoisirs, false);
		// pointServices = createPointFromCsv(csvServices, pointServices, true);
	}

	public static void sortAmenity2part(File rootFile, File empriseFile) throws Exception {

		File pointServices = new File(rootFile, "dataOut/serviceAutom.shp");
		File pointLoisirs = new File(rootFile, "dataOut/loisirAutom.shp");
		System.out.println("services");
		List<File> listServices = new ArrayList<>();
		listServices.add(createPointFromCsv(new File(rootFile, "tmp/sirene-service-geocoded.csv"), new File(rootFile, "tmp/Sirene-Services.shp"), empriseFile, true));
		listServices.add(new File(rootFile, "tmp/BPE-Services.shp"));

		mergeMultipleShp(rootFile, listServices, pointServices, empriseFile, true, false);

		List<File> listLoisirs = new ArrayList<>();
		System.out.println("loisirs");
		listLoisirs.add(new File(rootFile, "tmp/BPE-Loisirs.shp"));

		listLoisirs.add(createPointFromCsv(new File(rootFile, "tmp/sirene-loisir-geocoded.csv"), new File(rootFile, "tmp/Sirene-Loisirs.shp"), empriseFile, false));

		listLoisirs.add(loisirProcessing(new File(rootFile, "dataIn/vege/ZONE_VEGETATION.shp"), new File(rootFile, "dataIn/route/CHEMIN.shp"),
				new File(rootFile, "dataOut/routeAutom.shp")));
		mergeMultipleShp(rootFile, listLoisirs, pointLoisirs, empriseFile, true, false);
		System.out.println("done");
	}

	/**
	 * Afin de calculer les points d'entrées aux foret, le protocole suivant est utilisé : Création d’un buffer de 10m autour de la végétation de type \textit{Bois, Forêt fermée de
	 * feuillus, Forêt fermée de conifères,Forêt ouverte,Forêt fermée mixte, Zone arborée} et de surface supérieure à un hectare. Sélection des couches chemins et routes en inclus
	 * dans la bounding box de chacune de ces entités végétations. Chaque intersection des couches linéaires chemin et routes donnera un point d'accès à la forêt. Si il sont
	 * compris dans l’emprise de la forêt, sélection du point selon avec un type et une fréquence d'utilisation dépendant de la surface de la foret (si 1Ha<surface<2Ha, fréquence
	 * quotidienne, si 2Ha<surface<100Ha, fréquence hebdomadaire, si surface>100Ha, fréquence mensuelle).
	 * 
	 * @param vegetFile
	 *            : shapefile extrait de la BDTopo contenant les couches de végétation
	 * @param routeFile
	 *            : shapefile extrait de la BDTopo contenant les tronçons routiers
	 * @param cheminFile
	 *            : shapefile extrait de la BDTopo contenant les chemins
	 * @return : shapefile contenant les points d'entrées aux forêts.
	 * @throws Exception
	 */
	public static File loisirProcessing(File vegetFile, File routeFile, File cheminFile) throws Exception {
		// Minimal area for a vegetation feature to be considered as an loisir resort is a half of an hectare
		int minArea = 10000;
		ShapefileDataStore vegetSDS = new ShapefileDataStore(vegetFile.toURI().toURL());
		SimpleFeatureCollection veget = vegetSDS.getFeatureSource().getFeatures();
		ShapefileDataStore routeSDS = new ShapefileDataStore(routeFile.toURI().toURL());
		SimpleFeatureCollection route = routeSDS.getFeatureSource().getFeatures();
		ShapefileDataStore cheminSDS = new ShapefileDataStore(cheminFile.toURI().toURL());
		SimpleFeatureCollection chemin = cheminSDS.getFeatureSource().getFeatures();

		DefaultFeatureCollection vegetDFC = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("road");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("TYPE", String.class);
		sfTypeBuilder.add("LEVEL", Integer.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		int i = 0;
		SimpleFeatureIterator featIt = veget.features();
		try {
			while (featIt.hasNext()) {
				SimpleFeature feat = featIt.next();
				Object[] attr = { "", 0 };
				// TODO dé-zeuber l'encodage (pas arrivé -- pas le temps)
				if (feat.getAttribute("NATURE").equals("Bois") || feat.getAttribute("NATURE").equals("ForÃªt fermÃ©e de feuillus")
						|| feat.getAttribute("NATURE").equals("ForÃªt fermÃ©e de conifÃ¨res") || feat.getAttribute("NATURE").equals("ForÃªt fermÃ©e mixte")
						|| feat.getAttribute("NATURE").equals("ForÃªt ouverte") || feat.getAttribute("NATURE").equals("Zone arborÃ©e")) {
					if (((Geometry) feat.getDefaultGeometry()).getArea() > minArea) {
						if (((Geometry) feat.getDefaultGeometry()).getArea() < 20000) {
							attr[0] = "espace_vert_f1";
							attr[1] = 1;
						} else if (((Geometry) feat.getDefaultGeometry()).getArea() < 1000000) {
							attr[0] = "espace_vert_f2";
							attr[1] = 2;
						} else {
							attr[0] = "espace_vert_f3";
							attr[1] = 3;
						}
						sfBuilder.add((Geometry) feat.getDefaultGeometry());
						SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
						vegetDFC.add(feature);
						i = i + 1;
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			featIt.close();
		}

		DefaultFeatureCollection loisirColl = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder PointSfTypeBuilder = new SimpleFeatureTypeBuilder();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

		PointSfTypeBuilder.setName("loisir");
		PointSfTypeBuilder.setCRS(sourceCRS);
		PointSfTypeBuilder.add("the_geom", Point.class);
		PointSfTypeBuilder.setDefaultGeometry("the_geom");
		PointSfTypeBuilder.add("TYPE", String.class);
		PointSfTypeBuilder.add("LEVEL", Integer.class);

		SimpleFeatureType pointFeatureType = PointSfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder pointSfBuilder = new SimpleFeatureBuilder(pointFeatureType);
		int cpt = 0;
		// selection of the intersection points into those zones
		int j = 0;
		SimpleFeatureIterator vegetIt = vegetDFC.features();
		try {
			while (vegetIt.hasNext()) {
				cpt = cpt + 1;
				SimpleFeature featForet = vegetIt.next();
				// snap of the wanted data
				SimpleFeatureCollection snapRoute = SimPLUSimulator.snapDatas(route, ((Geometry) featForet.getDefaultGeometry()).buffer(15));
				SimpleFeatureCollection snapChemin = SimPLUSimulator.snapDatas(chemin, ((Geometry) featForet.getDefaultGeometry()).buffer(15));
				SimpleFeatureIterator routeIt = snapRoute.features();
				try {
					while (routeIt.hasNext()) {
						SimpleFeature featRoute = routeIt.next();
						SimpleFeatureIterator itChemin = snapChemin.features();
						try {
							while (itChemin.hasNext()) {
								SimpleFeature featChemin = itChemin.next();
								Coordinate[] coord = ((Geometry) featChemin.getDefaultGeometry()).intersection((Geometry) featRoute.getDefaultGeometry()).getCoordinates();
								for (Coordinate co : coord) {
									Point point = geometryFactory.createPoint(co);
									if ((((Geometry) featForet.getDefaultGeometry()).buffer(15)).contains(point)) {
										pointSfBuilder.add(point);
										Object[] att = { featForet.getAttribute("TYPE"), featForet.getAttribute("LEVEL") };
										SimpleFeature feature = pointSfBuilder.buildFeature(String.valueOf(j), att);
										loisirColl.add(feature);
										j = j + 1;

									}
								}
							}
						} catch (Exception problem) {
							problem.printStackTrace();
						} finally {
							itChemin.close();
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					routeIt.close();
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			vegetIt.close();
		}
		routeSDS.dispose();
		cheminSDS.dispose();
		vegetSDS.dispose();

		return SelectParcels.exportSFC(loisirColl.collection(), new File(vegetFile.getParentFile().getParentFile().getParentFile(), "tmp/loisir2.shp"));
	}

	public static File createPointFromCsv(File fileIn, File FileOut, File empriseFile, boolean service)
			throws IOException, NoSuchAuthorityCodeException, FactoryException, ParseException, MismatchedDimensionException, TransformException {

		boolean wkt = false;
		boolean mercador = false;

		WKTReader wktReader = new WKTReader();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		DefaultFeatureCollection coll = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder PointSfTypeBuilder = new SimpleFeatureTypeBuilder();
		PointSfTypeBuilder.setName("loisir");
		if (service) {
			PointSfTypeBuilder.setName("service");
		}
		PointSfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		PointSfTypeBuilder.add("the_geom", Point.class);
		PointSfTypeBuilder.setDefaultGeometry("the_geom");
		PointSfTypeBuilder.add("TYPE", String.class);
		PointSfTypeBuilder.add("LEVEL", Integer.class);

		Object[] attr = { 0, "" };

		SimpleFeatureType pointFeatureType = PointSfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder pointSfBuilder = new SimpleFeatureBuilder(pointFeatureType);

		CSVReader ptCsv = new CSVReader(new FileReader(fileIn));

		// we'll get the column number for geometry and attribute

		// case geocoded return lat/long
		CoordinateReferenceSystem epsg4326 = CRS.decode("EPSG:4326");
		CoordinateReferenceSystem epsg2154 = CRS.decode("EPSG:2154");
		MathTransform transform = CRS.findMathTransform(epsg4326, epsg2154, true);

		String[] firstLine = ptCsv.readNext();
		int mercLat = 0;
		int mercLng = 0;
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.equals("Lat")) {
				mercLat = i;
				mercador = true;
			}

			if (field.equals("Lng")) {
				mercLng = i;
			}
		}

		// case the geometry is a WKT String
		int nColWKT = 0;

		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.contains("WKT")) {
				nColWKT = i;
				wkt = true;
			}
		}
		// case it's X and Y coordinates
		int nColX = 0;
		int nColY = 0;
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.equals("X")) {
				nColX = i;
			}
			if (field.equals("Y")) {
				nColY = i;
			}
		}

		// case it's geocoded from the BPE database

		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.contains("lambert_x")) {
				nColX = i;

			}
			if (field.contains("lambert_y")) {
				nColY = i;

			}
		}

		// Number of the Type column
		int nColType = 0;
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.equals("TYPE")) {
				nColType = i;
			}
		}

		// Number of the Level column
		int nColLevel = 0;
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.equals("LEVEL")) {
				nColLevel = i;
			}
		}
		// System.out.println("level colonne : " + nColLevel);
		// System.out.println("type colonne : " + nColType);
		// System.out.println("y colonne : " + nColY);
		// System.out.println("x colonne : " + nColX);

		int i = 0;
		for (String[] row : ptCsv.readAll()) {
			if (wkt) {
				pointSfBuilder.add(wktReader.read(row[nColWKT]));
			}
			if (mercador) {
				DirectPosition2D ptSrc = new DirectPosition2D(Double.valueOf(row[mercLat]), Double.valueOf(row[mercLng]));
				DirectPosition2D ptDst = new DirectPosition2D();
				transform.transform(ptSrc, ptDst);
				Point point = geometryFactory.createPoint(new Coordinate(ptDst.x, ptDst.y));
				pointSfBuilder.add(point);
			} else {
				Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf((row[nColX])), Double.valueOf(row[nColY])));
				pointSfBuilder.add(point);
			}
			attr[0] = row[nColType];
			attr[1] = row[nColLevel];
			SimpleFeature feature = pointSfBuilder.buildFeature(String.valueOf(i), attr);
			coll.add(feature);
			i = i + 1;

		}
		ptCsv.close();
		SelectParcels.exportSFC(coll.collection(), new File("/home/mcolomb/tmp/tmp.shp"));
		return SelectParcels.exportSFC(cropSFC(coll.collection(), empriseFile), FileOut);
	}

	public static String geocodeBan(String targetURL, String urlParameters) throws IOException {
		// TODO faire que le géocodage soit automatique (la BAN ne réponds pas - surement la faute du proxy)
		HttpURLConnection connection = null;
		try {
			// Create connection
			URL url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "api");
			connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
			// connection.setRequestProperty("Content-Language", "fr-FR");
			System.out.println("here");
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			System.out.println("there");
			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.close();
			System.out.println("and everywhere");
			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			System.out.println(rd);
			StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

	}

	public static File setSpeed(File fileIn, File fileOut) throws Exception {
		ShapefileDataStore routesSDS = new ShapefileDataStore(fileIn.toURI().toURL());
		SimpleFeatureCollection routes = routesSDS.getFeatureSource().getFeatures();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("road");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiLineString.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("Speed", Integer.class);
		sfTypeBuilder.add("nature", String.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		DefaultFeatureCollection roadDFC = new DefaultFeatureCollection();
		int i = 0;
		SimpleFeatureIterator routeIt = routes.features();
		try {
			while (routeIt.hasNext()) {
				SimpleFeature feat = routeIt.next();
				Object[] attr = { 0, 0 };
				// TODO fix encodage
				switch (((String) feat.getAttribute("NATURE")).replaceAll("Ã©", "e")) {
				case "Autoroute":
					attr[0] = 130;
					attr[1] = "Autoroute";
					break;
				case "Quasi-autoroute":
					attr[0] = 110;
					attr[1] = "Quasi-autoroute";
					break;
				case "Bretelle":
					attr[0] = 50;
					attr[1] = "Bretelle";
					break;
				default:
					switch ((String) feat.getAttribute("CL_ADMIN")) {
					case "Autre":
						attr[0] = 40;
						attr[1] = "Autre";
						break;
					default:
						attr[0] = 80;
						attr[1] = feat.getAttribute("CL_ADMIN");
					}
				}
				// la ligne ci dessous devrait logiquement être dans la méthode prepareRaod, mais la flemme de recharger l'obj gt pour trier les attributs ; c'est bien ici aussi.
				if (!((String) feat.getAttribute("NATURE")).equals("Route empierrÃ©e")) {
					sfBuilder.add((Geometry) feat.getDefaultGeometry());
					SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
					roadDFC.add(feature);
					i = i + 1;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			routeIt.close();
		}
		routesSDS.dispose();
		return SelectParcels.exportSFC(roadDFC.collection(), fileOut);
	}

	/**
	 * 
	 * @param amen
	 *            : the "" field from the Sirene file
	 * @param forMore
	 *            : the "" type from the Sirene file
	 * @return
	 */
	public static String[] sortCatAmen(String amen, String forMore) {

		// return the shapefile it belongs to, the type and the level of visit frequency

		String[] classement = new String[3];
		switch (amen) {

		// CAS PARTICULIER ADMINISTRATION

		case "Administration publique générale":
			switch (forMore) {
			case "Enseignement primaire":
				classement[0] = "service";
				classement[1] = "ecole";
				classement[2] = "1";
			case "Enseignement pré-primaire":
				classement[0] = "service";
				classement[1] = "ecole";
				classement[2] = "1";
			case "Enseignement secondaire général":
				classement[0] = "service";
				classement[1] = "ecole";
				classement[2] = "1";
			case "Enseignement secondaire technique ou professionnel":
				classement[0] = "service";
				classement[1] = "ecole";
				classement[2] = "1";
			case "Enseignement supérieur":
				classement[0] = "service";
				classement[1] = "ecole";
				classement[2] = "1";
			}

			// SERVICES QUOTIDIENS

		case "Autres intermédiaires du commerce en denrées, boissons et tabac":
			classement[2] = " 1";
			classement[1] = "tabac";
			classement[0] = "service";
			break;
		case "Boulangerie et boulangerie-pâtisserie":
			classement[2] = " 1";
			classement[1] = "boulangerie";
			classement[0] = "service";
			break;
		case "Commerce d'alimentation générale":
			classement[2] = " 1";
			classement[1] = "superette";
			classement[0] = "service";
			break;
		case "Commerce de détail alimentaire sur éventaires et marchés":
			classement[2] = " 1";
			classement[1] = "superette";
			classement[0] = "service";
			break;
		case "Comm. détail viandes & produits à base de viande (magas. spéc.)":
			classement[2] = " 1";
			classement[1] = "boucherie";
			classement[0] = "service";
			break;
		case "Charcuterie":
			classement[2] = " 1";
			classement[1] = "boucherie";
			classement[0] = "service";
			break;
		case "Comm. détail poissons crustacés & mollusques (magasin spécialisé)":
			classement[2] = " 1";
			classement[1] = "boucherie";
			classement[0] = "service";
			break;
		case "Commerce de détail de journaux et papeterie en magasin spécialisé":
			classement[2] = " 1";
			classement[1] = "tabac";
			classement[0] = "service";
			break;
		case "Supérette":
			classement[2] = " 1";
			classement[1] = "superette";
			classement[0] = "service";
			break;

		// SERVICES HEBDOMADAIRES
		case "Activité des médecins généralistes":
			classement[2] = " 2";
			classement[1] = "medecin";
			classement[0] = "service";
			break;
		case "Activ. poste dans le cadre d'une obligation de service universel":
			classement[2] = " 2";
			classement[1] = "poste";
			classement[0] = "service";
			break;
		case "Autres activités de poste et de courrier":
			classement[2] = " 2";
			classement[1] = "poste";
			classement[0] = "service";
			break;
		case "Activités des centres de culture physique":
			classement[2] = " 2";
			classement[1] = "fitness";
			classement[0] = "service";
			break;
		case "Autres commerces de détail alimentaires en magasin spécialisé":
			classement[2] = " 2";
			classement[1] = "autre_alim";
			classement[0] = "service";
			break;
		case "Commerce de détail produits pharmaceutiques (magasin spécialisé)":
			classement[2] = " 2";
			classement[1] = "pharmacie";
			classement[0] = "service";
			break;
		case "Débits de boissons":
			classement[2] = " 2";
			classement[1] = "bar";
			classement[0] = "service";
			break;
		case "Hypermarchés":
			classement[2] = " 2";
			classement[1] = "supermarche";
			classement[0] = "service";
			break;
		case "Supermarché":
			classement[2] = " 2";
			classement[1] = "supermarche";
			classement[0] = "service";
			break;
		case "Restauration de type rapide":
			classement[2] = " 2";
			classement[1] = "restaurant";
			classement[0] = "service";
			break;
		case "Restauration collective sous contrat":
			classement[2] = " 2";
			classement[1] = "restaurant";
			classement[0] = "service";
			break;
		// case "Restauration traditionnelle":
		// classement[2] = " 2";
		// classement[1] = "restaurant";
		// classement[0] = "service";
		// break;

		case "F305":// Conservatoire
			classement[2] = " 2";
			classement[1] = "conservatoire";
			classement[0] = "service";
			break;

		// SERVICES MENSUELS
		case "Activités hospitalières":
			classement[2] = " 3";
			classement[1] = "hopital";
			classement[0] = "service";
			break;
		case "Gestion sites monuments historiques & attractions tourist. simil.":
			classement[2] = " 3";
			classement[1] = "equipement_culturel";
			classement[0] = "service";
			break;
		// case "Pratique dentaire":
		// classement[2] = " 3";
		// classement[1] = "specialiste";
		// classement[0] = "service";
		// break;

		case "Autres activités récréatives et de loisirs":
			classement[2] = " 3";
			classement[1] = "autre_equipement";
			classement[0] = "service";
			break;
		case "Organisation de jeux de hasard et d'argent":
			classement[2] = " 3";
			classement[1] = "autre_equipement";
			classement[0] = "service";
			break;
		case "Projection de films cinématographiques":
			classement[2] = " 3";
			classement[1] = "equipement_culturel";
			classement[0] = "service";
			break;
		case "Gestion de salles de spectacles":
			classement[2] = " 3";
			classement[1] = "equipement_culturel";
			classement[0] = "service";
			break;
		case "F302": // Théâtre
			classement[2] = " 3";
			classement[1] = "equipement_culturel";
			classement[0] = "service";
			break;
		case "F303": // Cinéma
			classement[2] = " 3";
			classement[1] = "equipement_culturel";
			classement[0] = "service";
			break;
		case "F304": // Musée
			classement[2] = " 3";
			classement[1] = "equipement_culturel";
			classement[0] = "service";
			break;

		// LOISIRS QUOTIDIENS

		case "F111": // Plateaux et terrains de jeux extérieurs
			classement[2] = "1";
			classement[1] = "jeux";
			classement[0] = "loisir";
			break;

		// boulodrome

		// LOISIRS HEBDO

		case "F101": // Bassin de natation
			classement[2] = "2";
			classement[1] = "piscine";
			classement[0] = "loisir";
			break;
		case "F102": // Boulodrome
			classement[2] = "2";
			classement[1] = "boulodrome";
			classement[0] = "loisir";
			break;
		case "F103": // Tennis
			classement[2] = "2";
			classement[1] = "tennis";
			classement[0] = "loisir";
			break;
		case "F104": // Équipement de cyclisme
			classement[2] = "2";
			classement[1] = "cyclisme";
			classement[0] = "loisir";
			break;
		case "F106": // Centre équestre
			classement[2] = "2";
			classement[1] = "equitation";
			classement[0] = "loisir";
			break;
		case "F107":// Athlétisme
			classement[2] = "2";
			classement[1] = "stade";
			classement[0] = "loisir";
			break;
		case "F109":// Parcours sportif/santé
			classement[2] = "2";
			classement[1] = "parcours";
			classement[0] = "loisir";
			break;
		case "Activités de clubs de sports":
			classement[2] = "2";
			classement[1] = "club-sport";
			classement[0] = "loisir";
			break;
		case "F118": // Sports nautiques
			classement[2] = "2";
			classement[1] = "piscine";
			classement[0] = "loisir";
			break;
		// case "F111":
		// switch (forMore){
		//
		// }
		// break;
		case "F112": // Salles spécialisées
			classement[2] = "2";
			classement[1] = "salle";
			classement[0] = "loisir";
			break;
		// case "F116":
		// classement[2] = "2";
		// classement[1] = "salle";
		// classement[0] = "loisir";
		// break;
		case "F113": // Terrain de grands jeux
			classement[2] = "3";
			classement[1] = "base-loisir";
			classement[0] = "multi-sport";
			break;
		case "F114": // Salles de combat
			classement[2] = "2";
			classement[1] = "dojo";
			classement[0] = "loisir";
			break;
		case "F117": // Roller-Skate-Vélo bicross ou freestyle
			classement[2] = "2";
			classement[1] = "skatepark";
			classement[0] = "loisir";
			break;
		case "F121": // Salles multisports (gymnase)
			classement[2] = "2";
			classement[1] = "gymnase";
			classement[0] = "loisir";
			break;
		// Loisirs Mensuels

		case "F201": // Baignade aménagée
			classement[2] = "2";
			classement[1] = "base-loisir";
			classement[0] = "loisir";
			break;
		case "F202": // Port de plaisance - Mouillage
			classement[2] = "2";
			classement[1] = "base-loisir";
			classement[0] = "loisir";
			break;
		// trains

		case "E103": // Gare avec desserte train à grande vitesse (TAGV)
			classement[2] = "";
			classement[1] = "LGV";
			classement[0] = "train";
			break;
		case "E106": // Gare sans desserte train à grande vitesse (TAGV)
			classement[2] = "";
			classement[1] = "normal";
			classement[0] = "train";
			break;

		}
		return classement;
	}

	/**
	 * Classe servant à
	 * 
	 * @param pointIn
	 * @param pointVille
	 * @return
	 * @throws IOException
	 */
	public static File preselecGeocode(File pointIn, File pointVille) throws IOException {
		File pointOut = new File(pointIn.getParentFile().getParentFile().getParentFile(), "tmp/sirenTri.csv");
		if (pointOut.exists()) {
			Files.delete(pointOut.toPath());
		}
		CSVReader csvVille = new CSVReader(new FileReader(pointVille));
		CSVReader csvAm = new CSVReader(new FileReader(pointIn));
		List<String[]> listVille = csvVille.readAll();
		List<String[]> listAm = csvAm.readAll();

		// collection pour éliminer les doublons
		ArrayList<String> deleteDouble = new ArrayList<>();

		int numCodPostSiren = 0;
		String[] firstLine = listAm.get(0);
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];

			if (field.equals("codpos")) {
				numCodPostSiren = i;
			}
		}
		int numCodPost = 0;
		firstLine = listVille.get(0);
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.contains("Code_postal")) {
				numCodPost = i;
			}
		}

		CSVWriter csv2copy = new CSVWriter(new FileWriter(pointOut, false));
		csv2copy.writeNext((String[]) listAm.get(0));
		for (String[] row : listVille) {
			String codePost = row[numCodPost];
			if (!deleteDouble.contains(codePost)) {
				for (String[] rOw : listAm) {
					if (codePost.toUpperCase().equals(rOw[numCodPostSiren].toUpperCase())) {
						csv2copy.writeNext(rOw);
					}
				}
			}
			deleteDouble.add(codePost);
		}
		csv2copy.close();
		csvVille.close();
		csvAm.close();
		return pointOut;
	}

	public static void prepareBuild(File rootFile, Integer[] nbDep, File empriseFile) throws Exception {
		File rootBuildFile = new File(rootFile, "dataIn/bati");
		File finalBuildFile = new File(rootFile, "dataOut/batimentAutom.shp");

		// merge and create the right road shapefile
		String[] listNom = { "BATI_INDIFFERENCIE", "BATI_REMARQUABLE", "BATI_INDUSTRIEL", "CIMETIERE", "PISTE_AERODROME", "RESERVOIR", "TERRAIN_SPORT", "CONSTRUCTION_LEGERE" };

		for (String s : listNom) {
			mergeMultipleBdTopo(rootBuildFile, s, nbDep, empriseFile);
		}

		// Final build file
		List<File> listShpFinal = new ArrayList<>();
		listShpFinal.add(new File(rootBuildFile, "BATI_INDIFFERENCIE.shp"));
		listShpFinal.add(new File(rootBuildFile, "BATI_REMARQUABLE.shp"));
		listShpFinal.add(new File(rootBuildFile, "BATI_INDUSTRIEL.shp"));
		mergeMultipleShp(rootBuildFile, listShpFinal, finalBuildFile, new File(""), true, false);

		// create the Non-urbanizable shapefile

		List<File> listShpNu = new ArrayList<>();
		listShpNu.add(new File(rootBuildFile, "CIMETIERE.shp"));
		listShpNu.add(new File(rootBuildFile, "CONSTRUCTION_LEGERE.shp"));
		listShpNu.add(new File(rootBuildFile, "PISTE_AERODROME.shp"));
		listShpNu.add(new File(rootBuildFile, "RESERVOIR.shp"));
		listShpNu.add(new File(rootBuildFile, "TERRAIN_SPORT.shp"));
		mergeMultipleShp(rootBuildFile, listShpNu, new File(rootFile, "dataOut/NU/artificial.shp"), new File(""), false, false);
	}

	public static void prepareHydrography(File rootFile, Integer[] nbDep, File empriseFile)
			throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {
		File rootHydroFile = new File(rootFile, "dataIn/hydro");

		for (File f : rootHydroFile.listFiles()) {
			if (f.getName().contains("SURFACE_EAU")) {
				Files.copy(f.toPath(), (new File(rootFile, "dataOut/NU/" + f.getName()).toPath()), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	public static void prepareVege(File rootFile, Integer[] nbDep, File empriseFile)
			throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {
		File rootVegeFile = new File(rootFile, "dataIn/vege");
		mergeMultipleBdTopo(rootVegeFile, "ZONE_VEGETATION", nbDep, empriseFile);

	}

	public static void prepareTrain(File rootFile, Integer[] nbDep, File empriseFile) throws Exception {

		File rootTrainFile = new File(rootFile, "dataIn/train");

		// merge and create the right road shapefile
		String[] listNom = { "TRONCON_VOIE_FERREE", "AIRE_TRIAGE" };
		for (String s : listNom) {
			mergeMultipleBdTopo(rootTrainFile, s, nbDep, empriseFile);
		}

		// create the Non-urbanizable shapefile

		ShapefileDataStore trainSDS = new ShapefileDataStore((new File(rootTrainFile, "TRONCON_VOIE_FERREE.shp")).toURI().toURL());
		SimpleFeatureCollection trainSFC = trainSDS.getFeatureSource().getFeatures();
		DefaultFeatureCollection bufferTrain = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("trainBuffer");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		int i = 0;

		SimpleFeatureIterator trainIt = trainSFC.features();
		try {
			while (trainIt.hasNext()) {

				SimpleFeature feat = trainIt.next();

				if (((String) feat.getAttribute("NATURE")).contains("LGV")) {
					Geometry extraFeat = ((Geometry) feat.getDefaultGeometry()).buffer(10);
					sfBuilder.add(extraFeat);
				} else {
					Geometry extraFeat = ((Geometry) feat.getDefaultGeometry()).buffer(7.5);
					sfBuilder.add(extraFeat);
				}
				bufferTrain.add(sfBuilder.buildFeature(String.valueOf(i)));

				i++;
			}

		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			trainIt.close();
		}

		ShapefileDataStore trainTriSDS = new ShapefileDataStore((new File(rootTrainFile, "AIRE_TRIAGE.shp")).toURI().toURL());
		SimpleFeatureCollection trainAT_SFC = trainTriSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator tratATIt = trainAT_SFC.features();
		try {
			while (tratATIt.hasNext()) {
				SimpleFeature feat = tratATIt.next();
				Geometry extraFeat = ((Geometry) feat.getDefaultGeometry());
				sfBuilder.add(extraFeat);
				bufferTrain.add(sfBuilder.buildFeature(String.valueOf(i)));
				i++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			tratATIt.close();
		}
		trainSDS.dispose();
		trainTriSDS.dispose();
		SelectParcels.exportSFC(bufferTrain.collection(), new File(rootFile, "dataOut/NU/bufferTrain.shp"));
	}

	public static void prepareRoad(File rootFile, Integer[] nbDep, File empriseFile) throws Exception {
		File rootRoadFile = new File(rootFile, "dataIn/route");
		File finalRoadFile = new File(rootFile, "dataOut/routeAutom.shp");
		// merge and create the right road shapefile
		String[] listNom = { "ROUTE_PRIMAIRE", "ROUTE_SECONDAIRE", "CHEMIN" };

		for (String s : listNom) {
			mergeMultipleBdTopo(rootRoadFile, s, nbDep, empriseFile);
		}
		List<File> listShp = new ArrayList<>();
		listShp.add(new File(rootRoadFile, "ROUTE_PRIMAIRE.shp"));
		listShp.add(new File(rootRoadFile, "ROUTE_SECONDAIRE.shp"));
		File tmpRoadFile = new File(rootFile, "tmp/route.shp");
		File routeMerged = mergeMultipleShp(rootRoadFile, listShp, tmpRoadFile, new File(""), true, false);
		File tmpTmpRoadFile = new File(rootFile, "tmp/route2Temp.shp");

		setSpeed(routeMerged, tmpTmpRoadFile);

		// delete the segments which are not linked to the main road network -- uses of geox tool coz I failed with geotools graphs. Conectors between objects still broken

		IFeatureCollection<IFeature> featColl = ShapefileReader.read(tmpTmpRoadFile.toString());

		CarteTopo cT = new CarteTopo("Network");
		double tolerance = 0.0;
		Chargeur.importAsEdges(featColl, cT, "", null, "", null, null, tolerance);

		Groupe gr = cT.getPopGroupes().nouvelElement();
		gr.setListeArcs(cT.getListeArcs());
		gr.setListeFaces(cT.getListeFaces());
		gr.setListeNoeuds(cT.getListeNoeuds());

		// on récupère les différents groupes
		List<Groupe> lG = gr.decomposeConnexes();
		Groupe zeGroupe = Collections.max(lG, Comparator.comparingInt(g -> g.getListeArcs().size()));
		IFeatureCollection<IFeature> featC = new FT_FeatureCollection<>();
		for (Arc a : zeGroupe.getListeArcs()) {
			featC.add(a.getCorrespondant(0));
		}

		ShapefileWriter.write(featC, finalRoadFile.toString(), CRS.decode("EPSG:2154"));

		// create the Non-urbanizable shapefile

		ShapefileDataStore routesSDS = new ShapefileDataStore((routeMerged).toURI().toURL());
		SimpleFeatureCollection routesSFC = routesSDS.getFeatureSource().getFeatures();
		DefaultFeatureCollection bufferRoute = new DefaultFeatureCollection();
		DefaultFeatureCollection bufferRouteExtra = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("roadBuffer");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		int i = 0;
		SimpleFeatureIterator routesIt = routesSFC.features();
		try {
			while (routesIt.hasNext()) {
				SimpleFeature feat = routesIt.next();
				Geometry newFeat = ((Geometry) feat.getDefaultGeometry()).buffer((double) feat.getAttribute("LARGEUR"));
				sfBuilder.add(newFeat);
				bufferRoute.add(sfBuilder.buildFeature(String.valueOf(i)));
				if (((String) feat.getAttribute("NATURE")).contains("Autoroute")) {
					Geometry extraFeat = ((Geometry) feat.getDefaultGeometry()).buffer(100);
					sfBuilder.add(extraFeat);
					bufferRouteExtra.add(sfBuilder.buildFeature(String.valueOf(i)));
				}
				// TODO regler l'encodage
				if (((String) feat.getAttribute("NATURE")).contains("Bretelle") || ((String) feat.getAttribute("NATURE")).contains("Route Ã  2 chaussÃ©es")
						|| ((String) feat.getAttribute("NATURE")).contains("Quasi-autoroute")) {
					Geometry extraFeat = ((Geometry) feat.getDefaultGeometry()).buffer(75);
					sfBuilder.add(extraFeat);
					bufferRouteExtra.add(sfBuilder.buildFeature(String.valueOf(i)));
				}
				i = i + 1;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			routesIt.close();
		}

		SelectParcels.exportSFC(bufferRoute.collection(), new File(rootFile, "dataOut/NU/bufferRoute.shp"));

		SelectParcels.exportSFC(bufferRouteExtra.collection(), new File(rootFile, "dataOut/NU/bufferExtraRoute.shp"));
		routesSDS.dispose();

	}

	public static void makeFullZoneNU(File rootFile) throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {
		File rootFileNU = new File(rootFile, "dataOut/NU/");
		List<File> listFullNU = new ArrayList<File>();
		listFullNU.add(new File(rootFileNU, "bufferRoute.shp"));
		listFullNU.add(new File(rootFileNU, "bufferExtraRoute.shp"));
		listFullNU.add(new File(rootFileNU, "SURFACE_EAU.shp"));
		listFullNU.add(new File(rootFileNU, "artificial.shp"));
		listFullNU.add(new File(rootFileNU, "bufferTrain.shp"));

		File filesRegles = new File(rootFile, "dataIn/NU");

		for (File f : filesRegles.listFiles()) {
			if (f.getName().endsWith(".shp")) {
				listFullNU.add(f);
			}
		}
		mergeMultipleShp(rootFileNU, listFullNU, new File(rootFileNU.getParentFile(), "nonUrbaAutom.shp"), new File(""), false, true);
	}

	public static void makePhysicNU(File rootFile) throws MalformedURLException, IOException, ParseException, NoSuchAuthorityCodeException, FactoryException {
		File rootFileNU = new File(rootFile, "dataOut/NU/");
		List<File> listFullNU = new ArrayList<File>();
		listFullNU.add(new File(rootFileNU, "bufferRoute.shp"));
		listFullNU.add(new File(rootFileNU, "SURFACE_EAU.shp"));
		listFullNU.add(new File(rootFileNU, "artificial.shp"));
		listFullNU.add(new File(rootFileNU, "bufferTrain.shp"));
		mergeMultipleShp(rootFileNU, listFullNU, new File(rootFileNU.getParentFile(), "nonUrbaPhyAutom.shp"), new File(""), false, true);
	}
}
