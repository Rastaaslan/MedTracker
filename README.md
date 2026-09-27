# MedTracker

Application Android locale de suivi des prises de médicaments.

## Objectif MVP

MedTracker permet de saisir un traitement tel qu'il est prescrit, d'afficher les prises prévues de la journée, de confirmer une prise réelle (par exemple « prise du matin »), d'enregistrer l'heure réelle et de rappeler les prises prévues.

Le produit ne doit jamais inventer, corriger ou recommander une posologie. Il suit uniquement les informations saisies par l'utilisateur.

## Principes

- Android en priorité.
- Fonctionnement local/offline.
- Aucune création de compte nécessaire pour le MVP.
- Données de santé conservées sur l'appareil.
- Notifications locales.
- Historique des prises.
- Horaires fixes et intervalle minimal configuré par l'utilisateur.
- Possibilité de corriger l'heure réelle d'une prise.
- Aucune recommandation médicale ou interprétation clinique.

## Confidentialité

Ce dépôt est public. **Ne jamais committer de véritables ordonnances, noms de patients, traitements personnels, données exportées de l'application ou autres données de santé.** Les fixtures et captures de tests doivent utiliser exclusivement des données fictives.

Voir `AGENTS.md` et `docs/MVP_SPEC.md` avant toute implémentation.
