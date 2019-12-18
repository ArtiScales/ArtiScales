package fr.ign.cogit.util;

import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.simplu3d.util.distribution.ZonePackager;

public class DataPreparator {

	public static void main(String[] args) throws Exception {
		File fileIn = new File("base");
		File folderTemp = new File("tmp");
		File folderOut = new File("packages");
		createPackages(fileIn, folderTemp, folderOut);
//		createPackages(new File(folderTemp, "parcelGenExport.shp"), folderTemp, folderOut);
//		Path path = Paths.get(fileIn.toURI()).normalize().toAbsolutePath();
//		List<File> l = Files.walk(path, FileVisitOption.FOLLOW_LINKS).parallel()
//    .filter(f->f.toFile().getName().endsWith(".shp")).map(f->f.toFile()).collect(Collectors.toList());
//    Vectors.mergeVectFiles(l, new File(folderTemp, "parcelGenExport.shp"));
	}

  public static void createPackages(File fileIn, File folderTemp, File folderOut) throws Exception {
    if (fileIn.isDirectory()) {
      FT_FeatureCollection<IFeature> parcels = new FT_FeatureCollection<>();
      Path path = Paths.get(fileIn.toURI()).normalize().toAbsolutePath();
      Files.walk(path, FileVisitOption.FOLLOW_LINKS).parallel()
      .filter(f->f.toFile().getName().endsWith(".shp"))
      .forEach(f-> parcels.addAll(ShapefileReader.read(f.toFile().getAbsolutePath())));
      System.out.println("Parcels contains " + parcels.size());
      createPackages(parcels, folderTemp, folderOut);
    } else {
      createPackages(ShapefileReader.read(fileIn.getAbsolutePath()), folderTemp, folderOut);
    }
  }
	public static void createPackages(IFeatureCollection<IFeature> parcelles, File folderTemp, File folderOut) throws Exception {

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
		ZonePackager.createParcelGroupsAndExport(parcelles, numberOfParcels, areaMax,
				folderTemp.getAbsolutePath(), folderOut.getAbsolutePath(), "500", true);
	}

}
