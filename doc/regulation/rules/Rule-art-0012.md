#  Regle-art-12 -  Règle de stationnement

## Modèle de phrase

> La création de nouveaux logements imposent la création d'un certain nombre de places de parking. Ce nombre est fixé par la valeur **{{ART_12}}** et peut employer différentes méthodes de calcul.

## Paramètres

### ART_12

Désigne comment est calculé le nombre de places de parkings sur la parcelle. Les valeurs possibles sont :
- **1** : un stationnement par logement ;
- **2** : deux stationnements par logement ;
- **1m60_2** : un stationnement pour par logement en dessous de 60m2, 2 pour les logements plus spacieux ;
- **0l2_2** : un stationnement par logement si le bâtiment ne contiens qu'un seul logement, 2 pour les bâtiments contenant plus de logements ;
- **1x50** : un stationnement par tranche de 50m² de logement. 
## Explications

Suivant la valeur de **{{ART_12}}** un ratio **r** = (places de parking) / (logements)  est déterminé.

- **r** = 1 si  **{{ART_12}}** = **1**  ou **{{ART_12}}** = **1 + 2**
- **r** = 2 si  **{{ART_12}}** = **2**

On doit s'assurer alors que la surface de la parcelle doit pouvoir contenir la surface construite et la surface des places de parking nécessaires. La surface d'une place de parking est définie dans le fichier **parametreScenario.xml** à travers la valeur **areaParkingLot**. Le nombre de logements est obtenu à partir de la surface de plancher du bâtiment divisé par la taille moyenne d'un logement (valeur **housingUnitSize**).
Si les logements sont de type maison isolée ou pavillon de lotissement, le nombre de logement est automatiquement de 1.  
Ainsi, vérifier le respect de cette règle revient à vérifier si l'expression suivante est vraie :

> **surfaceParcelle** > **surfaceBatie** +  ((int) **surfacePlancher**/ **housingUnitSize**) x **r** x **areaParkingLot**

## Implémentation

La règle est implémentée dans la classe **CommonPredicateArtiScales** et le ratio des places de parkings est défini par la méthode abstraite  **getArt12Value();**. Dans le cas où plusieurs règlements intersectent la parcelle, seul le ratio r  le plus élevé est conservé.
