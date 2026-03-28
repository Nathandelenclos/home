# Home Plugin (Spigot)

Plugin Spigot/Paper pour gérer:
- des homes personnels (par joueur)
- des warps globaux (communs au serveur)

Le plugin est conçu sans gestion de permissions: toutes les commandes sont accessibles aux joueurs.

## Fonctionnalités

- Création de homes personnels
- Téléportation vers un home personnel
- Liste des homes personnels
- Suppression d'un home personnel
- Création de warps globaux
- Téléportation vers un warp global
- Liste des warps globaux
- Suppression d'un warp global
- Persistance des données après redémarrage serveur
- Compatibilité de migration des anciennes positions enregistrées

## Pré-requis

- Java 21
- Serveur Spigot/Paper compatible API 1.21

## Build local

Depuis la racine du projet:

```bash
mvn clean package
```

Le JAR est généré dans le dossier target.

## Installation

1. Compiler le plugin (ou récupérer le JAR depuis une Release GitHub).
2. Copier le fichier JAR dans le dossier plugins de ton serveur.
3. Démarrer ou redémarrer le serveur.

## Commandes

| Commande | Description | Exemple |
|---|---|---|
| /sethome <nom> | Enregistre un home personnel | /sethome base |
| /home | Affiche la liste de tes homes | /home |
| /home <nom> | Téléporte vers un home personnel | /home base |
| /delhome <nom> | Supprime un home personnel | /delhome base |
| /setwarp <nom> | Enregistre un warp global | /setwarp spawn |
| /setwarps <nom> | Alias de /setwarp | /setwarps spawn |
| /warps | Affiche la liste des warps | /warps |
| /warp <nom> | Téléporte vers un warp global | /warp spawn |
| /delwarp <nom> | Supprime un warp global | /delwarp spawn |

## Persistance des données

Les homes et warps sont sauvegardés automatiquement dans la configuration du plugin:
- lors des commandes de création/suppression
- à l'arrêt du plugin

Les données restent disponibles après redémarrage du serveur.

## Structure technique

Architecture inspirée DDD + hexagonale:

- Domaine
- Services applicatifs
- Ports (repositories)
- Adaptateurs infrastructure (YAML/Bukkit)
- Adaptateur de présentation (commandes Bukkit)

## Qualité

Tests unitaires inclus pour:
- services Home et Warp
- value object de normalisation des noms

Lancer les tests:

```bash
mvn clean test
```

## CI/CD GitHub Actions

Le workflow GitHub Actions fourni:
- build le plugin avec Maven
- exécute les tests
- publie le JAR dans GitHub Releases

Déclenchement:
- push d'un tag commençant par v (ex: v1.0.0)
- exécution manuelle via workflow_dispatch

## Publier une release

1. Commit et push sur la branche principale.
2. Créer et pousser un tag, par exemple:

```bash
git tag v1.0.0
git push origin v1.0.0
```

3. Le workflow crée/alimente la Release GitHub et y joint le JAR.

## Notes

- Aucune permission Bukkit n'est exigée.
- Le plugin est prévu pour être autonome comme plugin unique de téléportation.
