# MedTracker

MVP Android local-first permettant de saisir les règles d'un traitement, consulter les prises du jour, enregistrer l'heure réelle séparément, la corriger ou l'annuler, et consulter l'historique. MedTracker restitue uniquement la configuration de l'utilisateur : il ne fournit aucun conseil médical.

## Architecture

- Application Kotlin, Jetpack Compose et Material 3, avec quatre destinations : Aujourd'hui, Traitements, Historique et Réglages.
- Room est l'unique stockage. `Medication`, `Treatment`, `ScheduleRule` et `IntakeEvent` sont persistés séparément ; `ScheduledDose` est calculé.
- `SchedulingEngine` est un moteur Kotlin pur. Les horaires fixes restent fixes ; le mode intervalle repart exclusivement du timestamp réel précédent et de l'intervalle saisi.
- `MedTrackerRepository` isole Room, le `MainViewModel` expose des `StateFlow`, et `ReminderScheduler` isole AlarmManager.
- Aucun backend, compte, analytics ou transfert de données.

## Prérequis et lancement

Android Studio récent, Android SDK 35, JDK 17 et **Gradle 8.11.1**. Le wrapper Gradle binaire n'est pas versionné dans ce dépôt afin que la contribution reste composée uniquement de fichiers texte : une version compatible de Gradle doit donc être installée localement et disponible dans le `PATH`. Ouvrir ensuite le dossier, laisser Gradle synchroniser, puis lancer la configuration `app` sur Android 8.0 (API 26) ou ultérieur.

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

L'APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`.

## Rappels

L'application demande la permission de notification seulement depuis Réglages. Elle utilise des alarmes locales non exactes compatibles avec les restrictions Android. Un refus n'affecte jamais les traitements ni l'historique. Après redémarrage, l'application conserve toutes les données Room et signale le besoin de replanifier les rappels.

## CI et APK

Le workflow **Android CI** installe explicitement Gradle 8.11.1 avec `gradle/actions/setup-gradle`, exécute lint, les tests unitaires et `assembleDebug`, puis publie `medtracker-debug-apk`. Dans GitHub, ouvrir l'exécution correspondante dans **Actions** et télécharger cet artifact.

## Limites MVP

Pas de synchronisation, compte, diagnostic, recommandations, interactions médicamenteuses ni suivi clinique. Les alarmes peuvent être retardées par Android. La sélection avancée de date et l'édition groupée de nombreuses règles restent volontairement simples dans ce premier MVP.
