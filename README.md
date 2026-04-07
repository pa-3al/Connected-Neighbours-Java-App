# Java app connected nighbours

1. **Nettoyer le projet (clean)**
   ```bash
   mvn clean
   ```

2. **Exécuter les tests (test)**
   ```bash
   mvn test
   ```

3. **Exécuter l'application (exec:java)**
   ```bash
   mvn exec:java
   ```
   Cette commande exécute l'application Java si le plugin `exec-maven-plugin` est configuré.

4. **Nettoyer, compiler et packager en une seule commande**
   ```bash
   mvn clean package
   ```
   Cette commande combine les étapes de nettoyage, compilation et création de package.

