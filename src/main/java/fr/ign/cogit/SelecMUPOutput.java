package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.thema.mupcity.analyse.MergeRasterResultAndBati;

import com.google.common.io.Files;

public class SelecMUPOutput {
	File rootFile; 
	
	public SelecMUPOutput(File rootfile){
		rootFile = rootfile;
	}
	public static List<File> run(File rootfile) throws IOException {
		//automatic vectorization of the MUP-City outputs
		SelecMUPOutput smo = new SelecMUPOutput(rootfile);
		return smo.run();
	}

public List<File> run() throws IOException{
		File MupOutputFolder = new File(rootFile, "depotConfigSpat");
		File output = new File(rootFile, "output");
		ArrayList<File> listMupOutput = new ArrayList<File>();
		for (File rasterOutputFolder : MupOutputFolder.listFiles()) {
			if (rasterOutputFolder.getName().endsWith(".tif")) {
				File outputMup = new File(output, rasterOutputFolder.getName().replace(".tif", ""));
				outputMup.mkdirs();
				listMupOutput.add(outputMup);
				File outputMupRaster = new File(outputMup, rasterOutputFolder.getName());
				Files.copy(rasterOutputFolder, outputMupRaster);
				//MergeRasterResultAndBati.VectorizeMupOutput(outputMupRaster);
			}
		}
		return listMupOutput;
	}

}
