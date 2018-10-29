package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import fr.ign.cogit.geoxygene.sig3d.indicator.RoofAngle;
import fr.ign.parameters.Parameters;

public class SimuTool {

	public static Parameters getParamFile(List<Parameters> lP, String scenar) throws FileNotFoundException {
	
		for (Parameters p : lP) {
			if (p.getString("nom").equals(scenar)) {
				return p;
			}
		}
	throw new FileNotFoundException("pas de param file correspodnant");	
	}
	public static void deleteDirectoryStream(Path path) throws IOException {
		Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}
	
	public static File createScenarVariantFolders(File packFile, File rootFile, String name) {
		
		File varFile = packFile.getParentFile();
		File scenarFile = varFile.getParentFile();
		File newFile = new File(rootFile, name+"/"+scenarFile+"/"+varFile+"/"+packFile.getName() );
		newFile.mkdirs();
		return newFile;
	}
}
