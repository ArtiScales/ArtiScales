package fr.ign.cogit.util;

import java.io.File;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.simplu3d.util.distribution.ZonePackager;

public class DataPreparator {

	
	public static void createPackages(File fileIn, File folderTemp, File folderOut) throws Exception {

		
		ZonePackager.ATTRIBUTE_SIMUL = "DoWeSimul";

		// Attributs pour reconstituer le IDPAR
		ZonePackager.ATTRIBUTE_DEPARTEMENT = "CODE_DEP";
		ZonePackager.ATTRIBUTE_COMMUNE = "CODE_COM";
		ZonePackager.ATTRIBUTE_PREFIXE = "COM_ABS";
		ZonePackager.ATTRIBUTE_SECTION = "SECTION";
		ZonePackager.ATTRIBUTE_NUMERO = "NUMERO";


		// Rayon autour duquel des parcelles sont ajoutées au contexte sans être
		// simulées.
		ZonePackager.CONTEXT_AREA = 1;

		// If we want to use a shapefile instead (data has to be in
		// Lambert93)
		IFeatureCollection<IFeature> parcelles = ShapefileReader.read(fileIn.getAbsolutePath());

		int numberOfParcels = 20;
		double areaMax = 5000;

		ZonePackager.createParcelGroupsAndExport(parcelles, numberOfParcels, areaMax, folderTemp.getAbsolutePath(), folderOut.getAbsolutePath(), true);


	}
	
}
