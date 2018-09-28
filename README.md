# ArtiScales

[![Build Status](https://travis-ci.org/ArtiScales/ArtiScales.svg?branch=master)](https://travis-ci.org/ArtiScales/ArtiScales)

Run the selectionParcel, SimPLU and BuiltToHousehold models. 
MUP-City simulations are not plugged in yet \n
Creation of a statistic .csv resume in the output file and some buildings resume in each simPLU files.


# Datas 
can be found at ftp/donnees/couplage. His would be the rootfile with three attached folders like : \n
MUP-City outputs in folder /depotConfigSpat \n
Geogrpahic shapefiles in folder /donneeGeographiques \n
PLU zoning plans in /pluZoning \n
.csv file containing the SimPLU rules in /pluZoning/codes/predicate.csv

# TODOs
SelectParcels \n
le merge/refonte des parcelles ne marche pas \n
Réorganiser : factoriser le code de la classe « selection » (en faire des classes filles?) \n
Les evaluations de certaines parelles sont nulles : voir pourquoi \n

SimPLU \n
Problème sur la méthode prepareCachedGeometries: les batiments des parcelles adjacentes ne sont pas trouvés.
