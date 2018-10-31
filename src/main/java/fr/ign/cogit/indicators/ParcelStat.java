package fr.ign.cogit.indicators;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;

import fr.ign.cogit.util.GetFromGeom;
import fr.ign.parameters.Parameters;

public class ParcelStat extends Indicators{
	
	File parcelFile;
	File rootFile;
	File geoFile ; 
	
	public ParcelStat(Parameters p,File parcelFile) throws IOException {
		super(p);
		this.parcelFile = parcelFile;
		rootFile = new File (p.getString("rootFile"));
		geoFile = new File (rootFile,"dataGeo");
//		ShapefileDataStore parcelSDS = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
//		SimpleFeatureCollection batiFeatures = parcelSDS.getFeatureSource().getFeatures();
		isParcelReallyBuilt();
	}

	public static void main(String[] args) throws Exception {
		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/scenar0MCIgn");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parametreTechnique.xml"));
		lF.add(new File(rootParam, "parametreScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		File parcelFile = new File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/teststp/variant0/parcelGenExport.shp");
		
		ParcelStat parc = new ParcelStat(p,parcelFile);
		parc.run();		
				
	}
	
	public void run() {
	}

	public void isParcelReallyBuilt() throws IOException, NoSuchAuthorityCodeException, FactoryException{
		File simuBuildFiles = new File(rootFile, "SimPLUDepot" + "/"+parcelFile.getParentFile().getParentFile().getName()+"/"+parcelFile.getParentFile().getName());
		
		ShapefileDataStore parcelSDS = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureIterator parcelFeaturesIt = parcelSDS.getFeatureSource().getFeatures().features();
			
		try {
			while (parcelFeaturesIt.hasNext()) {
				SimpleFeature feature = parcelFeaturesIt.next();
				for (File f : simuBuildFiles.listFiles()) {
					if (f.getName().startsWith("out-parcel")&&f.getName().endsWith(".shp")) {
						String inseeNum = f.getName().split("_")[1].split(".")[0];
						
					}
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelFeaturesIt.close();
		}
		
	}
	
private void getSurface() throws IOException {
	ShapefileDataStore shpDSCells = new ShapefileDataStore(parcelFile.toURI().toURL());
	SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

}
}
