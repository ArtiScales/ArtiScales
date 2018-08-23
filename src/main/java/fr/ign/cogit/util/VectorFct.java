package fr.ign.cogit.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.GTFunctions.Vectors;

public class VectorFct {
	
	
	public static void main(String[] args) throws Exception {
		mergeBatis(new File("/home/yo/Documents/these/ArtiScales/output/Stability-dataAutomPhy-CM20.0-S0.0-GP_915948.0_6677337.0--N6_St_Moy_ahpx_seed_9015629222324914404-evalAnal-20.0/25495/ZoningAllowed/simu0/"));
		
	}

	/**
	 * Merge all the shapefile of a folder (made for simPLU buildings) into one shapefile
	 * @param file2MergeIn : list of files containing the shapefiles
	 * @return : file where everything is saved (here whith a building name)
	 * @throws Exception
	 */
	public static File mergeBatis(List<File> file2MergeIn) throws Exception {
		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
		for (File f : file2MergeIn) {
			
				ShapefileDataStore SDSParcel = new ShapefileDataStore(f.toURI().toURL());
				SimpleFeatureIterator parcelIt = SDSParcel.getFeatureSource().getFeatures().features();
				try {
					while (parcelIt.hasNext()) {
						newParcel.add(parcelIt.next());
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
				SDSParcel.dispose();
			}
		
		File out = new File(file2MergeIn.get(0).getParentFile(), "TotBatSimuFill.shp");
		if (!newParcel.isEmpty()) {
			Vectors.exportSFC(newParcel.collection(), out);
		}
		return out;
	}
	
	/**
	 * Merge all the shapefile of a folder (made for simPLU buildings) into one shapefile
	 * @param file2MergeIn : folder containing the shapefiles
	 * @return : file where everything is saved (here whith a building name)
	 * @throws Exception
	 */
	public static File mergeBatis(File file2MergeIn) throws Exception {
		List<File> listBatiFile = new ArrayList<File>();
		for (File f : file2MergeIn.listFiles()) {
			if (f.getName().endsWith(".shp")&&f.getName().startsWith("out")) {
				listBatiFile.add(f);
			}
		}
		return mergeBatis(listBatiFile);
	}
}
