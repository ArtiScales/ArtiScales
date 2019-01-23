package fr.ign.cogit.util;

import java.io.File;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.simplu3d.util.distribution.ZonePackager;

public class DataPreparator {

	public static void main(String[] args) throws Exception {
		File fileIn = new File("/home/mickael/Bureau/Temp/parcel/parcel.shp");
		File folderTemp = new File("/tmp/tmp");
		File folderOut = new File("/home/mickael/Bureau/Temp/out/");

		createPackages(fileIn, folderTemp, folderOut,"25000");

	}

	public static void createPackages(File fileIn, File folderTemp, File folderOut, String codeCom) throws Exception {

		ZonePackager.ATTRIBUTE_SIMUL = "DoWeSimul";
		ZonePackager.ATTRIBUTE_SIMUL_TYPE = "String";

		// Attributs pour reconstituer le IDPAR
		ZonePackager.ATTRIBUTE_DEPARTEMENT = "CODE_DEP";
		ZonePackager.ATTRIBUTE_COMMUNE = "CODE_COM";
		ZonePackager.ATTRIBUTE_PREFIXE = "COM_ABS";
		ZonePackager.ATTRIBUTE_SECTION = "SECTION";
		ZonePackager.ATTRIBUTE_NUMERO = "NUMERO";

		// Rayon autour duquel des parcelles sont ajoutées au contexte sans être
		// simulées.
		ZonePackager.CONTEXT_AREA = 4;

		int numberOfParcels = 20;
		double areaMax = 5000;

		ZonePackager.createParcelGroupsAndExport(ShapefileReader.read(fileIn.getAbsolutePath()), numberOfParcels, areaMax, folderTemp.getAbsolutePath(),
				folderOut.getAbsolutePath(),codeCom, true);
	}

}
