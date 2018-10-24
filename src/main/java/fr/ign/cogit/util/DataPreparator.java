package fr.ign.cogit.util;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.simplu3d.util.distribution.ZonePackager;

public class DataPreparator {

	public static void main(String[] args) throws Exception {

		// Il faudra mettre le vrai attribut simul
		ZonePackager.ATTRIBUTE_SIMUL = "FEUILLE";

		// Attributs pour reconstituer le IDPAR
		ZonePackager.ATTRIBUTE_DEPARTEMENT = "CODE_DEP";
		ZonePackager.ATTRIBUTE_COMMUNE = "CODE_COM";
		ZonePackager.ATTRIBUTE_PREFIXE = "CODE_ARR";
		ZonePackager.ATTRIBUTE_SECTION = "SECTION";
		ZonePackager.ATTRIBUTE_NUMERO = "NUMERO";

		// Rayon autour duquel des parcelles sont ajoutées au contexte sans être
		// simulées.
		ZonePackager.CONTEXT_AREA = 0.5;

		String fileIn = "/home/mickael/Téléchargements/ArtiScales/dataGeo/parcelle.shp";

		// Folder where results are stored
		String folderOut = "/tmp/tmp/";
		String folderTemp = "/tmp/temporary/";

		// If we want to use a shapefile instead (data has to be in
		// Lambert93)
		IFeatureCollection<IFeature> parcelles = ShapefileReader.read(fileIn);

		int numberOfParcels = 20;
		double areaMax = 5000;

		ZonePackager.createParcelGroupsAndExport(parcelles, numberOfParcels, areaMax, folderTemp, folderOut);

	}
}
