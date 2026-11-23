# Changelog

## v0.9.3
- Restreint la navigation et les téléchargements à l’origine HTTPS exacte (hôte + port) du serveur configuré.
- Renforce le workflow GitHub Actions pour les événements `push` et `workflow_dispatch`.
- Ajoute une validation locale des ressources Android et de la configuration avant compilation.
- Incrémente la version Android à 0.9.3 (versionCode 4).

## v0.9.2
- Projet Android autonome et workflow de compilation APK.
- Vérification de signature et empreinte SHA-256 de l’APK généré.
