# Rule-form-001 - Limitation du nombre de boîtes

## Définition

>  La configuration bâtie simulée est limitée à **{{nbCuboid}}** boîtes

## Paramètres

**{{nbCuboid}}** :  le nombre maximal de boîtes  autorisées pour une configuration est fixé à **{{nbCuboid}}**. Cette valeur est stockée dans le fichier XML des paramètres de scénarios (parametreScenario.xml).

## Explications

L'objectif de cette règle est de limiter le nombre de boîtes afin de simuler différentes complexités de bâtiments. Pendant la simulation, lorsque le système propose d'ajouter une boîte pour dépasser la valeur **{{nbCuboid}}**, la configuration est rejetée.
