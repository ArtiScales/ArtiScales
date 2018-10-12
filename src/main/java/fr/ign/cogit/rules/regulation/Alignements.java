package fr.ign.cogit.rules.regulation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.ParcelBoundarySide;

public class Alignements {

	public boolean hasAlignement = false;

	private BasicPropertyUnit currentBPU;

	public Alignements(List<ArtiScalesRegulation> allRegulation, BasicPropertyUnit currentBPU) {
		// Currently we only take the first
		ArtiScalesRegulation regulation = allRegulation.get(0);
		this.currentBPU = currentBPU;

		if (regulation.getArt_71() == 1 || regulation.getArt_71() == 2) {
			hasAlignement = true;

		}

	}

	public IGeometry[] getRightSide() {
		return getSide(ParcelBoundarySide.RIGHT);

	}

	public IGeometry[] getLeftSide() {
		return getSide(ParcelBoundarySide.LEFT);

	}

	private IGeometry[] getSide(ParcelBoundarySide side) {

		List<IGeometry> lGeom = new ArrayList<>();

		for (CadastralParcel cO : currentBPU.getCadastralParcels()) {
			lGeom.addAll(cO.getBoundariesBySide(side).stream().map(x -> x.getGeom()).collect(Collectors.toList()));
		}

		IGeometry[] geometryArray = new IGeometry[lGeom.size()];
		geometryArray = lGeom.toArray(geometryArray);
		
		
		return  geometryArray;
	}

	public boolean getHasAlignement() {
		// TODO Auto-generated method stub
		return hasAlignement;
	}

}
