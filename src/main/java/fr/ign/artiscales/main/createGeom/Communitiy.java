package fr.ign.artiscales.main.createGeom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;


public class Communitiy {
	public static File makeCommunitiesFromParcels(File communitiesFile, File parcelFile, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		
		SimpleFeatureBuilder ft = Schemas.getASCommunitySchema();

		ShapefileDataStore shpDSParcels = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection featuresParcels = shpDSParcels.getFeatureSource().getFeatures();
		SimpleFeatureIterator it = featuresParcels.features();

		ShapefileDataStore shpDSCommunes = new ShapefileDataStore(communitiesFile.toURI().toURL());
		SimpleFeatureCollection featuresCommunes = shpDSCommunes.getFeatureSource().getFeatures();

		DefaultFeatureCollection dfC = new DefaultFeatureCollection();

		HashMap<String, List<Geometry>> result = new HashMap<String, List<Geometry>>();
		try {
			while (it.hasNext()) {
				SimpleFeature featAdd = it.next();
				String insee = ((String) featAdd.getAttribute("CODE_DEP")) + ((String) featAdd.getAttribute("CODE_COM"));
				List<Geometry> lG = new ArrayList<Geometry>();
				lG.add((Geometry) featAdd.getDefaultGeometry());
				if (result.containsKey(insee)) {
					List<Geometry> tmp = result.remove(insee);
					tmp.addAll(lG);
					result.put(insee, tmp);
				} else {
					result.put(insee, lG);
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		for (String insee : result.keySet()) {
			Geometry geom = Geom.unionGeom(result.get(insee));
			SimpleFeatureIterator itCom = featuresCommunes.features();
			// ft.set("the_geom", geom.buffer(20).buffer(-20));
			ft.set("the_geom", geom);

			try {
				while (itCom.hasNext()) {
					SimpleFeature featAdd = itCom.next();
					if (insee.equals(featAdd.getAttribute("DEPCOM"))) {
						ft.set("DEPCOM", featAdd.getAttribute("DEPCOM"));
						ft.set("NOM_COM", featAdd.getAttribute("NOM_COM"));
						ft.set("typo", featAdd.getAttribute("typo"));
						ft.set("surface", featAdd.getAttribute("surface"));
						ft.set("scot", featAdd.getAttribute("scot"));
						ft.set("log-icone", featAdd.getAttribute("log-icone"));
						dfC.add(ft.buildFeature(null));
						break;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				itCom.close();
			}
		}
		shpDSParcels.dispose();
		shpDSCommunes.dispose();
		return Collec.exportSFC(dfC.collection(), outFile);

	}
}
