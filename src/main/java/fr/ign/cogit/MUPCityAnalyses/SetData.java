package fr.ign.cogit.MUPCityAnalyses;

import java.io.BufferedReader;
import java.io.Console;
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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.SelectParcels;
import fr.ign.cogit.SimPLUSimulator;

public class SetData {

	/**
	 * Sort a geocoded csv file into different kind of amenities for a MUP-City simulation
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// General

		File rootFile = new File("/home/mcolomb/donnee/autom/");
		File testFile = new File(rootFile, "tmp");
		testFile.mkdirs();

		File NUFile = new File(rootFile, "/dataOut/NU/");
		NUFile.mkdirs();

		Integer[] nbDep = { 39, 25, 70 };

		// geocodeBan("https://api-adresse.data.gouv.fr/", "?q=8 bd du port&postcode=44380");
		// geocodeBan("http://nominatim.openstreetmap.org/search", "q=135+pilkington+avenue,+birmingham&format=xml&polygon=1&addressdetails=1");

		// Amenities

		sortAmenity(rootFile);

		// Bati
		prepareBuild(rootFile, nbDep);
		// // Road
		prepareRoad(rootFile, nbDep);

		// Hydro

		prepareHydrography(rootFile, nbDep);

		// Train

		prepareTrain(rootFile, nbDep);

		// Zones Non Urbanisables

		makeFullZoneNU(rootFile);

		makePhysicNU(rootFile);

		// mergeMultipleBdTopo(new File("/home/mcolomb/donnee/autom/dataIn/route"), "CHEMIN", nbDep,
		// new File("/home/mcolomb/informatique/MUP/explo/emprise/data/emprise_finale+bordure.shp"));
		// // setSpeed(new File("/home/mcolomb/donnee/autom/nouveau-jeu/route/route.shp"));
	}

	public static File mergeMultipleBdTopo(File rootFile, String nom, Integer[] listDep) throws MalformedURLException, IOException, ParseException {
		return mergeMultipleBdTopo(rootFile, nom, listDep, new File("no-enveloppe"));
	}

	public static File mergeMultipleBdTopo(File rootFile, String nom, Integer[] listDep, File enveloppe) throws MalformedURLException, IOException, ParseException {
		List<File> listFile = new ArrayList<>();
		for (int i = 0; i < listDep.length; i = i + 1) {
			File fileDep = new File(rootFile, String.valueOf(listDep[i]) + "/" + nom + ".shp");
			listFile.add(fileDep);
		}
		return mergeMultipleShp(rootFile, listFile, new File(rootFile, nom + ".shp"), enveloppe, true);
	}

	public static File mergeMultipleShp(File rootFile, List<File> listShp, File fileOut, File enveloppe, boolean keepAttributes)
			throws MalformedURLException, IOException, ParseException {
		WKTReader wktReader = new WKTReader();
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
			SimpleFeatureCollection addSFC = shpDS.getFeatureSource().getFeatures();

			for (Object obj : addSFC.toArray()) {
				SimpleFeature feat = (SimpleFeature) obj;
				Object[] attr = new Object[0];
				if (keepAttributes) {
					attr = new Object[feat.getAttributeCount() - 1];
					for (int h = 1; h < feat.getAttributeCount(); h = h + 1) {
						attr[h - 1] = feat.getAttribute(h);
					}
				}
				sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));
				SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(nId), attr);
				nId = nId + 1;
				merged.add(feature);
			}
			shpDS.dispose();
		}
		System.out.println(merged.size());
		SimpleFeatureCollection output = merged.collection();

		if (enveloppe.exists()) {
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
			ReferencedEnvelope env = ((new ShapefileDataStore(enveloppe.toURI().toURL())).getFeatureSource().getFeatures()).getBounds();
			String geometryPropertyName = merged.getSchema().getGeometryDescriptor().getLocalName();
			Filter filter = ff.bbox(ff.property(geometryPropertyName), env);
			output = merged.subCollection(filter);
		}

		return SelectParcels.exportSFC(output, fileOut);
	}

	public static void sortAmenity(File rootFile) throws Exception {
		// TODO automatiser l'extraction de la base de donnée SIRENE
		File pointIn = new File(rootFile, "dataIn/sirene/siren-dept.csv");

		// tiré du GÉOFLA et jointuré avec la poste pour avoir les codes postaux
		File listVille = new File(rootFile, "dataIn/admin.csv");
		File csvServices = new File(rootFile, "tmp/siren-Services.csv");
		File csvLoisirs = new File(rootFile, "tmp/siren-Loisirs.csv");
		File pointServices = new File(rootFile, "dataOut/service.shp");
		File pointLoisirs = new File(rootFile, "dataOut/loisir.shp");

		if (csvLoisirs.exists()) {
			Files.delete(csvLoisirs.toPath());
		}
		if (csvServices.exists()) {
			Files.delete(csvServices.toPath());
		}

		CSVReader csvPruned = new CSVReader(new FileReader(preselecGeocode(pointIn, listVille)));
		CSVWriter csvServiceW = new CSVWriter(new FileWriter(csvServices, true));
		CSVWriter csvLoisirW = new CSVWriter(new FileWriter(csvLoisirs, true));
		String[] firstLine = csvPruned.readNext();
		String[] newFirstLine = new String[firstLine.length + 2];
		for (int k = 0; k < firstLine.length; k = k + 1) {
			newFirstLine[k] = firstLine[k];
		}
		newFirstLine[firstLine.length] = "type";
		newFirstLine[firstLine.length + 1] = "level";
		csvLoisirW.writeNext(newFirstLine);
		csvServiceW.writeNext(newFirstLine);

		for (String[] row : csvPruned.readAll()) {
			String[] result = new String[102];
			String[] resultSort = sortCatAmen(row[43], row[73]);
			if (!(resultSort[0] == null)) {
				for (int i = 0; i < 100; i = i + 1) {
					result[i] = row[i];
				}
				result[100] = resultSort[1];
				result[101] = resultSort[2];
				switch (resultSort[0]) {
				case "service":
					csvServiceW.writeNext(result);
					break;
				case "loisir":
					csvLoisirW.writeNext(result);
					break;
				}
			}
		}
		csvPruned.close();
		csvServiceW.close();
		csvLoisirW.close();
		// je n'ai pour l'instant pas automatisé le géocodage - trop de temps et vieux problème de proxy
		// geocodeBan(targetURL, urlParameters)

		// TODO
		// geocodeBan()
		// File loisirTemp1 = createPointFromCsv(csvLoisirs, pointLoisirs, false);
		// pointServices = createPointFromCsv(csvServices, pointServices, true);

		createPointFromCsv(new File(rootFile, "dataIn/sirene/service-geocode.csv"), pointServices, true);
		File loisirTemp1 = createPointFromCsv(new File(rootFile, "dataIn/sirene/loisir-geocode.csv"), new File(rootFile, "tmp/loisir1.shp"), false);

		File loisirTemp2 = loisirProcessing(new File(rootFile, "dataIn/vege/vegetation.shp"), new File(rootFile, "dataIn/route/chemin.shp"),
				new File(rootFile, "dataIn/route/route.shp"));

		DefaultFeatureCollection loisirDFC = new DefaultFeatureCollection();
		loisirDFC.addAll((new ShapefileDataStore(loisirTemp1.toURI().toURL())).getFeatureSource().getFeatures());
		System.out.println((new ShapefileDataStore(loisirTemp1.toURI().toURL())).getFeatureSource().getFeatures().getSchema());
		loisirDFC.addAll((new ShapefileDataStore(loisirTemp2.toURI().toURL())).getFeatureSource().getFeatures());
		System.out.println((new ShapefileDataStore(loisirTemp2.toURI().toURL())).getFeatureSource().getFeatures().getSchema());
		SelectParcels.exportSFC(loisirDFC.collection(), pointLoisirs);

	}

	public static File loisirProcessing(File vegetFile, File routeFile, File cheminFile) throws Exception {
		// Minimal area for a vegetation feature to be considered as an loisir resort is a half of an hectare
		int minArea = 10000;

		SimpleFeatureCollection veget = (new ShapefileDataStore(vegetFile.toURI().toURL())).getFeatureSource().getFeatures();
		SimpleFeatureCollection route = (new ShapefileDataStore(routeFile.toURI().toURL())).getFeatureSource().getFeatures();
		SimpleFeatureCollection chemin = (new ShapefileDataStore(cheminFile.toURI().toURL())).getFeatureSource().getFeatures();

		DefaultFeatureCollection vegetDFC = new DefaultFeatureCollection();

		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("road");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("level", Integer.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		int i = 0;
		for (Object obj : veget.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
			Object[] attr = { 0 };
			// TODO dé-zeuber l'encodage (pas arrivé -- pas le temps)
			if (feat.getAttribute("NATURE").equals("Bois") || feat.getAttribute("NATURE").equals("ForÃªt fermÃ©e de feuillus")
					|| feat.getAttribute("NATURE").equals("ForÃªt fermÃ©e de conifÃ¨res") || feat.getAttribute("NATURE").equals("ForÃªt fermÃ©e mixte")
					|| feat.getAttribute("NATURE").equals("ForÃªt ouverte")) {
				if (((Geometry) feat.getDefaultGeometry()).getArea() > minArea) {
					if (((Geometry) feat.getDefaultGeometry()).getArea() < 20000) {
						attr[0] = 1;
					} else if (((Geometry) feat.getDefaultGeometry()).getArea() < 1000000) {
						attr[0] = 2;
					} else {
						attr[0] = 3;
					}
					sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));
					SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
					vegetDFC.add(feature);
					i = i + 1;
				}
			}
		}

		DefaultFeatureCollection loisirColl = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder PointSfTypeBuilder = new SimpleFeatureTypeBuilder();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

		PointSfTypeBuilder.setName("loisir");
		PointSfTypeBuilder.setCRS(sourceCRS);
		PointSfTypeBuilder.add("the_geom", Point.class);
		PointSfTypeBuilder.setDefaultGeometry("the_geom");
		PointSfTypeBuilder.add("level", Integer.class);

		SimpleFeatureType pointFeatureType = PointSfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder pointSfBuilder = new SimpleFeatureBuilder(pointFeatureType);
		int cpt = 0;
		// selection of the intersection points into those zones
		int j = 0;
		for (Object obj : vegetDFC.toArray()) {
			cpt = cpt + 1;

			SimpleFeature featForet = (SimpleFeature) obj;
			// snap of the wanted data
			SimpleFeatureCollection snapRoute = SimPLUSimulator.snapDatas(route, ((Geometry) featForet.getDefaultGeometry()).buffer(15));
			SimpleFeatureCollection snapChemin = SimPLUSimulator.snapDatas(chemin, ((Geometry) featForet.getDefaultGeometry()).buffer(15));

			for (Object obje : snapRoute.toArray()) {
				SimpleFeature featRoute = (SimpleFeature) obje;
				for (Object objet : snapChemin.toArray()) {
					SimpleFeature featChemin = (SimpleFeature) objet;
					Coordinate[] coord = ((Geometry) featChemin.getDefaultGeometry()).intersection((Geometry) featRoute.getDefaultGeometry()).getCoordinates();
					for (Coordinate co : coord) {
						Point point = geometryFactory.createPoint(co);
						if ((((Geometry) featForet.getDefaultGeometry()).buffer(15)).contains(point)) {
							pointSfBuilder.add(point);
							Object[] att = { featForet.getAttribute("level") };
							SimpleFeature feature = pointSfBuilder.buildFeature(String.valueOf(j), att);
							loisirColl.add(feature);
							j = j + 1;

						}
					}
				}
			}
		}

		return SelectParcels.exportSFC(loisirColl.collection(), new File(vegetFile.getParentFile().getParentFile().getParentFile(), "tmp/loisir2.shp"));
	}

	public static File createPointFromCsv(File fileIn, File FileOut, boolean service) throws IOException, NoSuchAuthorityCodeException, FactoryException, ParseException {

		boolean wkt = false;

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
		PointSfTypeBuilder.add("type", String.class);
		PointSfTypeBuilder.add("level", Integer.class);

		Object[] attr = { 0, "" };

		SimpleFeatureType pointFeatureType = PointSfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder pointSfBuilder = new SimpleFeatureBuilder(pointFeatureType);

		CSVReader ptCsv = new CSVReader(new FileReader(fileIn));

		// we'll get the column with the WKT info
		int nColWKT = 0;
		String[] firstLine = ptCsv.readNext();
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.contains("WKT")) {
				nColWKT = i;
				wkt = true;
			}
		}
		int nColX = 0;
		int nColY = 0;
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field.contains("X")) {
				nColX = i;
				wkt = false;
			}
			if (field.contains("Y")) {
				nColY = i;
			}
		}

		int nColType = 0;
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field == "type") {
				nColType = i;
			}
		}
		int nColLevel = 0;
		for (int i = 0; i < firstLine.length; i = i + 1) {
			String field = firstLine[i];
			if (field == "level") {
				nColLevel = i;
			}
		}
		System.out.println("nColX :" + nColX);
		System.out.println("nColY :" + nColY);
		int i = 0;
		for (String[] row : ptCsv.readAll()) {
			if (wkt) {
				pointSfBuilder.add(wktReader.read(row[nColWKT]));
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
		return SelectParcels.exportSFC(coll.collection(), FileOut);
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

		SimpleFeatureCollection routes = (new ShapefileDataStore(fileIn.toURI().toURL())).getFeatureSource().getFeatures();

		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("road");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiLineString.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("speed", Integer.class);
		sfTypeBuilder.add("nature", String.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		DefaultFeatureCollection roadDFC = new DefaultFeatureCollection();
		int i = 0;

		for (Object obj : routes.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
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
				attr[0] = 60;
				attr[1] = "Bretelle";
				break;
			case "Route empierree":
				attr[0] = 15;
				attr[1] = "Route empierrée";
				break;
			default:
				switch ((String) feat.getAttribute("CL_ADMIN")) {
				case "Autre":
					attr[0] = 50;
					attr[1] = "Autre";
					break;
				default:
					attr[0] = 90;
					attr[1] = feat.getAttribute("CL_ADMIN");
				}
			}
			sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));
			SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
			roadDFC.add(feature);
			i = i + 1;
		}
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

		case "Supérette":
			classement[2] = " 1";
			classement[1] = "superette";
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
		case "Boulangerie et boulangerie-pâtisserie":
			classement[2] = " 1";
			classement[1] = "boulangerie";
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
		case "Autres intermédiaires du commerce en denrées, boissons et tabac":
			classement[2] = " 1";
			classement[1] = "tabac";
			classement[0] = "service";
			break;
		case "Commerce de détail de journaux et papeterie en magasin spécialisé":
			classement[2] = " 1";
			classement[1] = "tabac";
			classement[0] = "service";
			break;

		// SERVICES HEBDOMADAIRES
		case "Supermarché":
			classement[2] = " 2";
			classement[1] = "supermarche";
			classement[0] = "service";
			break;
		case "Hypermarchés":
			classement[2] = " 2";
			classement[1] = "supermarche";
			classement[0] = "service";
			break;
		case "Débits de boissons":
			classement[2] = " 2";
			classement[1] = "bar";
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
		case "Activité des médecins généralistes":
			classement[2] = " 2";
			classement[1] = "medecin";
			classement[0] = "service";
			break;
		case "Commerce de détail produits pharmaceutiques (magasin spécialisé)":
			classement[2] = " 2";
			classement[1] = "pharmacie";
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

		// SERVICES MENSUELS
		case "Gestion sites monuments historiques & attractions tourist. simil.":
			classement[2] = " 3";
			classement[1] = "musee";
			classement[0] = "service";
			break;
		case "Pratique dentaire":
			classement[2] = " 3";
			classement[1] = "specialiste";
			classement[0] = "service";
			break;
		case "Activités hospitalières":
			classement[2] = " 3";
			classement[1] = "hopital";
			classement[0] = "service";
			break;
		case "Restauration traditionnelle":
			classement[2] = " 3";
			classement[1] = "restaurant";
			classement[0] = "service";
			break;
		case "Organisation de jeux de hasard et d'argent":
			classement[2] = " 3";
			classement[1] = "autre_service_loisir";
			classement[0] = "service";
			break;
		case "Projection de films cinématographiques":
			classement[2] = " 3";
			classement[1] = "equip-culturel";
			classement[0] = "service";
			break;
		case "Gestion de salles de spectacles":
			classement[2] = " 3";
			classement[1] = "equip-culturel";
			classement[0] = "service";
			break;

		// LOISIRS QUOTIDIENS
		case "":
			classement[2] = "";
			classement[1] = "club-sport";
			classement[0] = "loisir";
			break;

		// LOISIRS HEBDO
		case "Activités de clubs de sports":
			classement[2] = "2";
			classement[1] = "club-sport";
			classement[0] = "loisir";
			break;

		}
		return classement;
	}

	public static File preselecGeocode(File pointIn, File pointVille) throws IOException {
		File pointOut = new File(pointIn.getParentFile().getParentFile().getParentFile(), "tmp/siren-besacTri.csv");
		if (pointOut.exists()) {
			Files.delete(pointOut.toPath());
		}
		CSVReader csvVille = new CSVReader(new FileReader(pointVille));
		CSVReader csvAm = new CSVReader(new FileReader(pointIn));

		List<String[]> listVille = csvVille.readAll();
		List<String[]> listAm = csvAm.readAll();

		// collection pour éliminer les doublons
		ArrayList<String> deleteDouble = new ArrayList<>();

		CSVWriter csv2copy = new CSVWriter(new FileWriter(pointOut, false));
		csv2copy.writeNext((String[]) listAm.get(0));
		for (String[] row : listVille) {
			String codePost = row[17];
			if (!deleteDouble.contains(codePost)) {
				for (String[] rOw : listAm) {
					if (codePost.toUpperCase().equals(rOw[20].toUpperCase())) {
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

	public static void prepareBuild(File rootFile, Integer[] nbDep) throws Exception {
		File rootBuildFile = new File(rootFile, "dataIn/bati");
		File finalBuildFile = new File(rootFile, "dataOut/bati.shp");
		File empriseFile = new File("/home/mcolomb/informatique/MUP/explo/emprise/data/emprise_finale+bordure.shp");

		// merge and create the right road shapefile
		String[] listNom = { "BATI_INDIFFERENCIE", "BATI_REMARQUABLE", "BATI_INDUSTRIEL", "CIMETIERE", "PISTE_AERODROME", "RESERVOIR", "TERRAIN_SPORT" };

		for (String s : listNom) {
			mergeMultipleBdTopo(rootBuildFile, s, nbDep, empriseFile);
		}

		// Final build file
		List<File> listShpFinal = new ArrayList<>();
		listShpFinal.add(new File(rootBuildFile, "BATI_INDIFFERENCIE.shp"));
		listShpFinal.add(new File(rootBuildFile, "BATI_REMARQUABLE.shp"));
		listShpFinal.add(new File(rootBuildFile, "BATI_INDUSTRIEL.shp"));
		mergeMultipleShp(rootBuildFile, listShpFinal, finalBuildFile, new File(""), true);

		// create the Non-urbanizable shapefile

		List<File> listShpNu = new ArrayList<>();
		listShpNu.add(new File(rootBuildFile, "CIMETIERE.shp"));
		listShpNu.add(new File(rootBuildFile, "PISTE_AERODROME.shp"));
		listShpNu.add(new File(rootBuildFile, "RESERVOIR.shp"));
		listShpNu.add(new File(rootBuildFile, "TERRAIN_SPORT.shp"));
		mergeMultipleShp(rootBuildFile, listShpNu, new File(rootFile, "dataOut/NU/artificial.shp"), new File(""), false);
	}

	public static void prepareHydrography(File rootFile, Integer[] nbDep) throws MalformedURLException, IOException, ParseException {
		File rootHydroFile = new File(rootFile, "dataIn/hydro");
		File empriseFile = new File("/home/mcolomb/informatique/MUP/explo/emprise/data/emprise_finale+bordure.shp");
		
		Path pathHydro = mergeMultipleBdTopo(rootHydroFile, "SURFACE_EAU", nbDep, empriseFile).toPath();

		System.out.println("HYYYDOR ");
		
		for(File f :  rootHydroFile.listFiles() ){
			System.out.println(f);
			if (f.getName().contains("SURFACE_EAU")){
				System.out.println(pathHydro.getName(pathHydro.getNameCount()-1).toString());
				Files.copy(f.toPath(), (new File(rootFile, "dataOut/NU/"+f.getName()).toPath()),StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	public static void prepareTrain(File rootFile, Integer[] nbDep) throws Exception {

		File rootRoadFile = new File(rootFile, "dataIn/train");
		File empriseFile = new File("/home/mcolomb/informatique/MUP/explo/emprise/data/emprise_finale+bordure.shp");

		// merge and create the right road shapefile
		String[] listNom = { "TRONCON_VOIE_FERREE", "AIRE_TRIAGE" };
		for (String s : listNom) {
			mergeMultipleBdTopo(rootRoadFile, s, nbDep, empriseFile);
		}

		// create the Non-urbanizable shapefile

		SimpleFeatureCollection trainSFC = (new ShapefileDataStore((new File(rootRoadFile, "TRONCON_VOIE_FERREE.shp")).toURI().toURL())).getFeatureSource().getFeatures();
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
		for (Object obj : trainSFC.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;

			if (((String) feat.getAttribute("NATURE")).contains("LGV")) {
				Geometry extraFeat = ((Geometry) feat.getDefaultGeometry()).buffer(10);
				sfBuilder.add(extraFeat);
			} else {
				Geometry extraFeat = ((Geometry) feat.getDefaultGeometry()).buffer(7.5);
				sfBuilder.add(extraFeat);
			}
			bufferTrain.add(sfBuilder.buildFeature(String.valueOf(i)));

			i = i + 1;
		}
		SimpleFeatureCollection trainAT_SFC = (new ShapefileDataStore((new File(rootRoadFile, "AIRE_TRIAGE.shp")).toURI().toURL())).getFeatureSource().getFeatures();
		for (Object obj : trainAT_SFC.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
			Geometry extraFeat = ((Geometry) feat.getDefaultGeometry());
			sfBuilder.add(extraFeat);
			bufferTrain.add(sfBuilder.buildFeature(String.valueOf(i)));
			i = i + 1;
		}
		SelectParcels.exportSFC(bufferTrain.collection(), new File(rootFile, "dataOut/NU/bufferTrain.shp"));
	}

	public static void prepareRoad(File rootFile, Integer[] nbDep) throws Exception {
		File rootRoadFile = new File(rootFile, "dataIn/route");
		File finalRoadFile = new File(rootFile, "dataOut/route.shp");
		File empriseFile = new File("/home/mcolomb/informatique/MUP/explo/emprise/data/emprise_finale+bordure.shp");

		// merge and create the right road shapefile
		String[] listNom = { "ROUTE_PRIMAIRE", "ROUTE_SECONDAIRE", "CHEMIN" };

		for (String s : listNom) {
			mergeMultipleBdTopo(rootRoadFile, s, nbDep, empriseFile);
		}
		List<File> listShp = new ArrayList<>();
		listShp.add(new File(rootRoadFile, "ROUTE_PRIMAIRE.shp"));
		listShp.add(new File(rootRoadFile, "ROUTE_SECONDAIRE.shp"));
		File routeMerged = mergeMultipleShp(rootRoadFile, listShp, new File(rootFile, "tmp/route.shp"), new File(""), true);
		setSpeed(routeMerged, finalRoadFile);

		// create the Non-urbanizable shapefile

		SimpleFeatureCollection routesSFC = (new ShapefileDataStore(routeMerged.toURI().toURL())).getFeatureSource().getFeatures();
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
		for (Object obj : routesSFC.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
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

		SelectParcels.exportSFC(bufferRoute.collection(), new File(rootFile, "dataOut/NU/bufferRoute.shp"));

		SelectParcels.exportSFC(bufferRouteExtra.collection(), new File(rootFile, "dataOut/NU/bufferExtraRoute.shp"));

	}

	public static void makeFullZoneNU(File rootFile) throws MalformedURLException, IOException, ParseException {
		rootFile = new File(rootFile, "dataOut/NU/");
		List<File> listFullNU = new ArrayList<File>();
		listFullNU.add(new File(rootFile, "bufferRoute.shp"));
		listFullNU.add(new File(rootFile, "bufferExtraRoute.shp"));
		listFullNU.add(new File(rootFile, "SURFACE_EAU.shp"));
		listFullNU.add(new File(rootFile, "artificial.shp"));
		listFullNU.add(new File(rootFile, "bufferTrain.shp"));

		File filesRegles = new File("/home/mcolomb/donnee/autom/dataIn/NU");

		for (File f : filesRegles.listFiles()) {
			if (f.getName().endsWith(".shp")) {
				listFullNU.add(f);
			}
		}
		mergeMultipleShp(rootFile, listFullNU, new File(rootFile, "NUtot.shp"), new File(""), false);
	}

	public static void makePhysicNU(File rootFile) throws MalformedURLException, IOException, ParseException {
		rootFile = new File(rootFile, "dataOut/NU/");
		List<File> listFullNU = new ArrayList<File>();
		listFullNU.add(new File(rootFile, "bufferRoute.shp"));
		listFullNU.add(new File(rootFile, "SURFACE_EAU.shp"));
		listFullNU.add(new File(rootFile, "artificial.shp"));
		listFullNU.add(new File(rootFile, "bufferTrain.shp"));
		mergeMultipleShp(rootFile, listFullNU, new File(rootFile, "NUPhysic.shp"), new File(""), false);
	}
}
