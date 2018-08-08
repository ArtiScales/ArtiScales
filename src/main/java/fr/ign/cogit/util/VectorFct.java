package fr.ign.cogit.util;


import java.io.File;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.GTFunctions.Vectors;

public class VectorFct {
		public static File mergeBatis(File file2MergeIn) throws Exception {
			DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
			for (File f : file2MergeIn.listFiles()) {
				if (f.toString().contains(".shp")) {
					SimpleFeatureCollection parcelCollection = (new ShapefileDataStore(f.toURI().toURL())).getFeatureSource().getFeatures();
					for (Object obj : parcelCollection.toArray()) {
						SimpleFeature feat = (SimpleFeature) obj;
						newParcel.add(feat);
					}
				}
			}
			File out = new File(file2MergeIn, "TotBatSimuFill.shp");
			if (!newParcel.isEmpty()) {
				Vectors.exportSFC(newParcel.collection(), out);
			}
			return out;
		}
	}
