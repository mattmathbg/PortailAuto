# Guide de Contribution — PortailAuto

Merci de l'intérêt que vous portez au projet **PortailAuto** ! Ce guide détaille les conventions de développement et la démarche pour proposer des améliorations.

---

## 🛠️ Environnement de Développement

- **IDE** : [Android Studio](https://developer.android.com/studio) (Koala, Ladybug ou version supérieure)
- **JDK** : Java Development Kit 17
- **SDK Android** : Compile SDK 35, Min SDK 26 (Android 8.0 Oreo+)
- **Langage** : Kotlin 2.0+ (Jetpack Compose, Coroutines, DataStore)

---

## 🚀 Workflow de Contribution

1. **Fork** le dépôt sur GitHub.
2. Créez une branche dédiée à votre fonctionnalité ou correction :
   ```bash
   git checkout -b feature/ma-nouvelle-fonctionnalite
   # ou
   git checkout -b fix/resolution-bug
   ```
3. Effectuez vos modifications en respectant les standards de style Kotlin (Android Kotlin Style Guide).
4. Testez vos changements :
   - Mode Dry-Run in-app
   - Scripts ADB dans `scripts/`
   - Simulation GPX dans l'émulateur
5. Commitez avec des messages clairs suivant la convention [Conventional Commits](https://www.conventionalcommits.org/) :
   - `feat: ...` pour une nouvelle fonctionnalité
   - `fix: ...` pour un correctif
   - `docs: ...` pour la documentation
   - `refactor: ...` pour du remaniement sans changement fonctionnel
6. Pushez votre branche et ouvrez une **Pull Request**.

---

## 🛡️ Règles d'Ingénierie Clés du Projet

- **Zéro Foreground Service permanent** : Ne jamais ajouter de service de fond persistant pour du polling. Tout doit reposer sur des `PendingIntent` et le réveil matériel de Google Play Services.
- **Préservation de la mémoire en veille** : L'application doit maintenir **0 Mo de RAM consommée au repos**. Les récepteurs doivent s'exécuter via `goAsync()` et se clore sans conserver de threads actifs.
- **Résilience aux faux positifs** : Tout nouveau mécanisme de détection doit s'intégrer au pipeline sans affaiblir les verrous Wi-Fi (BSSID/SSID) et l'hystérésis (Cooldown DataStore).

---

## 📄 Licence

En contribuant à ce projet, vous acceptez que vos contributions soient placées sous la licence [MIT](LICENSE).
