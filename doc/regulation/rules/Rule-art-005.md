# Rule-art-005 - Minimum parcellaire - Surface minimale d'une parcelle constructible

## Modèle de phrase

> La parcelle est constructible si sa surface dépasse {{ART_5}} m².

## Paramètres

### {{ART_5}}

Désigne la surface minimale à partir de laquelle la parcelle est constructible. Valeur positive.
- La valeur 99 signifie que la règle n'est pas définie


## Implémentation

La vérification s'effectue en amont de la simulation lors de l'instanciation de la classe Predicte (MultiplePredicateArtiScales ou PredicateArtiScales) qui vérifie le respect des règles. Si la parcelle concernée par ces règles a une aire inférieure à la valeur de **{{ART_5}}** alors la simulation n'est pas effectuée.
