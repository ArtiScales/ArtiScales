# Regle-art-74 - Distance minimum des constructions par rapport aux limites séparatives, exprimée par rapport à la hauteur du bâtiment

## Modèle de phrase

> La hauteur doit être inférieure à {{B1_ART_74}} fois la distance aux limites séparatives latérales et de fond de la parcelle.

## Paramètres

### B1_ART_74

Coefficient multiplicateur désignant la hauteur maximale du bâtiment par rapport aux limites séparatives latérales et de fond de parcelle.

## Explications

{{B1_ART_74}} représente la pente du plan sous lequel le bâtiment doit se trouver.

![Image montrant la limitation de la hauteur maximale en fonction du coefficient {{B1_ART_74}} ](img/IAUIDF/IAUIDF-004.png)

## Implémentation

La vérification de la distance s'effectue dans la classe PredicateIAUIDF. Si le bâtiment doit être collé sur un des bords de la parcelle (cf [RENNES-003.md](RENNES-003.md)), alors la limite à laquel le bâtiment est collé n'est pas prise en compte.
