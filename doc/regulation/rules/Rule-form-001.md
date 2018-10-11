# Rule-form-001 - Limitation du nombre de boîtes

## Définition

>  La configuration bâtie simulée est limité à **{{nbCuboid}}** boîtes

## Paramètres

**{{nbCuboid}}** :  le nombre de boîtes maximales autorisée pour une configuration et est stocké dans le fichier XML des paramètres de scénarios (parametreScenario.xml).

## Explications

L'objectif de cette règle est de limiter le nombre de boîtes afin de simuler différentes complexités de bâtiments. Pendant la simulation, lorsque le système propose d'ajouter une boîte pour dépasser la valeur **{{nbCuboid}}**, la configuration est rejetée.
