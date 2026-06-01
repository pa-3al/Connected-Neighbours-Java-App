# Application Desktop Voisinea

Cette application est destinée aux administrateurs de la plateforme Voisinea.

Cette application fait partie du Projet Annuel de 3ème année Architecture des logiciels, Année 2025-2026.

Développé par :

- Erwan LUCE-GUEDON
- Racim ADJIRI
- Rémy THIBAUT

## Configuration et installation

Pour faire tourner cette application, la version Java 21 est obligatoire.

### Installation

```sh
git clone git@github.com:pa-3al/Connected-Neighbours-Java-App.git
cd Connected-Neighbours-Java-App
```

### Nettoyage du projet

```sh
mvn clean
```

### Lancement de l'application

```sh
mvn javafx:run
```

### Configuration du fichier de properties

```txt
app.auth.admin.login2faPath=/admin/auth/login-2fa
app.auth.admin.loginPath=/admin/auth/login
app.auth.admin.ssoAuthorizePath=/admin/auth/desktop/sso
app.auth.baseUrl= # URL du backend
app.auth.bypass.enabled=false
app.auth.bypass.token=local-dev-token
app.auth.sso.timeout=180
app.auth.timeout=15
app.db.password= # Utilisateur SQLite
app.db.url=jdbc:sqlite:./data/neighborhood.db
app.db.user= # Mot de passe SQLite
app.plugins.path=plugins
app.sync.db.url= # URL de connexion à la base de données
app.sync.interval=60
app.update.checkUrl= # URL de check de des dernières versions
app.update.timeout=30
```
