# boss

Appli Android de nettoyage automatique, compilée par GitHub Actions.

## Installation
1. Pousse le projet sur GitHub (branche main).
2. Onglet Actions > "Build boss APK" > télécharge l'artefact `boss-apk`.
3. Installe app-debug.apk sur le téléphone (autoriser l'installation hors Play Store).
4. Ouvre boss et accorde, dans l'ordre : affichage par-dessus, statistiques d'utilisation, accès à tous les fichiers.
5. Appuie sur "Afficher la bulle". Un tap sur la bulle active (verte) ou désactive le nettoyage toutes les 5 min. Glisse-la pour la déplacer.

## Ce que fait le nettoyage
- Vide le cache de boss lui-même (Android interdit de vider celui des autres applis).
- Déplace vers BossQuarantine/<date>/ : fichiers .tmp/.temp/.class/.dex/.log/.crdownload/.part de plus d'1 h, et APK de plus de 3 jours dont l'appli est déjà installée.
- Purge la quarantaine après 7 jours.
- Ferme les applis en arrière-plan sauf les 3 dernières utilisées (et l'accueil).

## Limites réelles
- Android ne permet pas de forcer l'arrêt d'une appli visible sans root ou ADB.
- Termux est isolé : boss ne peut pas nettoyer son dossier ni le cache Gradle interne.
- Les doublons, PDF, vidéos et médias WhatsApp ne sont pas supprimés automatiquement : il faut une validation humaine.
- Les 5 minutes ne sont pas exactes : Android peut retarder l'alarme en veille.

## Modifier les règles
Tout est dans CleanerEngine.kt (listes d'extensions, dossiers, délais).
