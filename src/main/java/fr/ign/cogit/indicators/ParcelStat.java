package fr.ign.cogit.indicators;

import java.io.File;
import java.io.IOException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;

import fr.ign.parameters.Parameters;

public class ParcelStat extends Indicators{
	
	File parcelFile;
	public ParcelStat(Parameters p,File parcelFile) {
		super(p);
		this.parcelFile = parcelFile;
		
		
		
	}

	
private void getSurface() throws IOException {
	ShapefileDataStore shpDSCells = new ShapefileDataStore(parcelFile.toURI().toURL());
	SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

}
}
