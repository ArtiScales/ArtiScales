package fr.ign.cogit.util;

import java.io.File;
import java.util.Map;

import org.geotools.referencing.CRS;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.util.distribution.ZonePackager;

public class DataPreparator {

	public static void main(String[] args) throws Exception {

		// Il faudra mettre le vrai attribut simul
		ZonePackager.ATTRIBUTE_SIMUL = "FEUILLE";
		
		//Attributs pour reconstituer le IDPAR
		ZonePackager.ATTRIBUTE_DEPARTEMENT = "CODE_DEP";
		ZonePackager.ATTRIBUTE_COMMUNE = "CODE_COM";
		ZonePackager.ATTRIBUTE_PREFIXE = "CODE_ARR";
		ZonePackager.ATTRIBUTE_SECTION = "SECTION";
		ZonePackager.ATTRIBUTE_NUMERO = "NUMERO";
		
		
		//Rayon autour duquel des parcelles sont ajoutées au contexte sans être simulées.
		ZonePackager.CONTEXT_AREA = 0.5;
		
		
		String fileIn = "/home/mickael/Téléchargements/ArtiScales/dataGeo/parcelle.shp";

		// Folder where results are stored
		String folderOut = "/tmp/tmp/";

		(new File(folderOut)).mkdirs();

		// If we want to use a shapefile instead (data has to be in
		// Lambert93)
		IFeatureCollection<IFeature> collectionParcels = ShapefileReader.read(fileIn);

		int numberOfParcels = 20;
		double areaMax = 5000;

		// Creating the groups into a map
		Map<Integer, IFeatureCollection<IFeature>> map = ZonePackager.createParcelGroups(collectionParcels,
				numberOfParcels, areaMax);

		// Just checking if there are no double in the results
		long count = 0;
		for (Object s : map.keySet().toArray()) {
			long nbOfSimulatedParcel = map.get(s).getElements().stream()
					.filter(feat -> (feat.getGeom().area() < areaMax))
					.filter(feat -> (Boolean.parseBoolean(feat.getAttribute(ZonePackager.ATTRIBUTE_SIMUL).toString())))
					.count();

			System.out.println("For group : " + s + "  -  " + nbOfSimulatedParcel + "  entities");
			count = count + nbOfSimulatedParcel;
		}

		System.out.println("Number of features in map : " + count);

		// Creating the folder
		ZonePackager.exportFolder(map, folderOut);

		/////////////////////////////////////////////////////////////////////
		// This code is not useful for a final production and for simulation as
		///////////////////////////////////////////////////////////////////// it
		///////////////////////////////////////////////////////////////////// only
		///////////////////////////////////////////////////////////////////// proposes
		///////////////////////////////////////////////////////////////////// an
		///////////////////////////////////////////////////////////////////// aggregated
		///////////////////////////////////////////////////////////////////// export
		// WARNING !!!!!!!!!
		// Do not forget to remove the aggregated.shp from out folder
		// If you want to run simulation
		/////////////////////////////

		// Export with double to get a fast view of the folders
		IFeatureCollection<IFeature> exportWithDouble = new FT_FeatureCollection<>();
		for (Object s : map.keySet().toArray()) {
			exportWithDouble.addAll(map.get(s));
		}

		// Storing the agregated results (only for debug and to check if the
		// blocks are
		// correctly generated)
		ShapefileWriter.write(exportWithDouble, folderOut + "export_with_double.shp",
				CRS.decode(ZonePackager.SRID_END));

	}
}
