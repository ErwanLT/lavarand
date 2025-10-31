# Lava Lamp Entropy Generator

Ce projet implémente un générateur de nombres pseudo-aléatoires (DRBG - Deterministic Random Bit Generator) qui utilise une simulation de lampe à lave comme source d'entropie. L'objectif est de démontrer comment des phénomènes visuels et chaotiques peuvent être exploités pour renforcer la qualité de la génération de nombres aléatoires.

## Fonctionnalités

*   **Simulation de Lampe à Lave Virtuelle** : Une simulation graphique de blobs se déplaçant et interagissant dans un environnement de lampe à lave.
*   **Collecte d'Entropie Visuelle** : Des captures d'écran de la simulation sont transformées en données brutes, puis "blanchies" (hashed) pour servir de source d'entropie.
*   **Mélange d'Entropie** : L'entropie visuelle est mélangée avec du bruit système (jitter, `SecureRandom`) pour garantir un non-déterminisme accru.
*   **Générateur de Nombres Aléatoires Cryptographique (HMAC-DRBG)** : Un DRBG basé sur HMAC-SHA256 est utilisé pour générer des séquences de bits aléatoires de haute qualité, ré-ensemencé périodiquement avec l'entropie collectée.
*   **Interface Graphique (GUI)** : Une application Swing (`LavaLampApp`) pour visualiser la simulation en temps réel et observer la génération continue d'entropie.
*   **Interface en Ligne de Commande (CLI)** : Une application console (`MainVirtualLava`) pour générer des blocs de nombres aléatoires sans interface visuelle, utile pour les scripts ou les tests.
*   **Sauvegarde de Snapshots** : La simulation peut sauvegarder périodiquement des images PNG des captures d'écran utilisées pour l'entropie, permettant une inspection visuelle.

## Comment ça marche ?

Le cœur du système repose sur la classe `LavaLampEntropyGenerator`. Elle orchestre la simulation de la lampe à lave, extrait des données de l'image générée par le simulateur (`VirtualLavaEntropy`), mélange ces données avec du bruit additionnel, puis utilise le tout pour ré-ensemencer un `HmacDRBG`. Ce DRBG est ensuite utilisé pour produire des octets aléatoires.

## Structure du Projet

Le projet est organisé en plusieurs packages pour une meilleure modularité :

*   `fr.eletutour.lavalamp.app` : Contient les points d'entrée de l'application (GUI et CLI).
*   `fr.eletutour.lavalamp.simulation` : Gère la logique de la simulation de la lampe à lave.
*   `fr.eletutour.lavalamp.entropy` : S'occupe de la collecte, du traitement et de la gestion de l'entropie.
*   `fr.eletutour.lavalamp.crypto` : Contient l'implémentation du générateur HMAC-DRBG.
*   `fr.eletutour.lavalamp.util` : Fournit des fonctions utilitaires (hachage SHA256, calcul d'entropie de Shannon, etc.).

## Démarrage Rapide

### Prérequis

*   Java Development Kit (JDK) 25
*   Apache Maven

### Construction du Projet

Pour compiler le projet, naviguez jusqu'à la racine du projet et exécutez :

```bash
mvn clean install
```

### Exécuter l'Application GUI

Pour lancer l'application graphique de la lampe à lave :

```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.lavalamp.app.LavaLampApp"
```

### Exécuter l'Application CLI

Pour exécuter la version en ligne de commande qui génère des nombres aléatoires :

```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.lavalamp.app.MainVirtualLava"
```

## Snapshots

L'application sauvegarde périodiquement des images PNG des captures d'écran utilisées pour l'entropie. Ces fichiers sont nommés `snapshot_YYYYMMDD_HHmmss_SSS.png` et sont créés dans le répertoire racine du projet. Vous pouvez les consulter pour voir l'aspect de la simulation au moment de la collecte d'entropie.

## Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.
