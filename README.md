# Structure du projet
Voici comment organiser votre projet Scala pour Docker :

```
mon-appli-scala/
│
├── src/
│   └── main/
│       └── scala/
│           ├── ConsommationStreaming.scala
│           ├── KafkaStreaming.scala
│           ├── SparkBigData.scala
│           └── TwitterKafkaStreaming.scala
│
├── Dockerfile
├── .dockerignore
├── pom.xml
└── README.md
```

### Fichier `.dockerignore`
Créez un fichier `.dockerignore` dans le répertoire racine de votre projet pour exclure les fichiers et dossiers non nécessaires de votre contexte de construction Docker :

```
target/
.idea/
.vscode/
*.class
*.log
.DS_Store
```

### Fichier `Dockerfile`
Créez un `Dockerfile` dans le répertoire racine de votre projet qui va définir comment construire votre image Docker. Voici un exemple basique :

```dockerfile
# Utilisez une image de base qui inclut Maven et le JDK
FROM maven:3.8.4-openjdk-17 as builder

# Définissez le répertoire de travail dans le conteneur
WORKDIR /app

# Copiez le fichier pom.xml et le code source dans le conteneur
COPY pom.xml .
COPY src ./src

# Exécutez Maven pour construire le fichier .jar de votre application
RUN mvn clean package

# Vous pouvez ensuite utiliser une image Spark pour exécuter le .jar ou le copier à partir de là
```

### Construction de l'image Docker
Ouvrez un terminal et naviguez jusqu'au répertoire racine de votre projet (`mon-appli-scala/`). Exécutez la commande suivante pour construire votre image Docker :

```bash
docker build -t mon-appli-scala .
```

Cette commande va construire une nouvelle image Docker appelée `mon-appli-scala` en utilisant le `Dockerfile` que vous avez créé.

### Exécution avec Docker Compose
Ensuite, pour exécuter votre application avec Spark et Kafka, vous pouvez utiliser `docker-compose`. Voici un exemple de fichier `docker-compose.yml` :

```yaml
version: '3'
services:
  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"

  kafka:
    image: wurstmeister/kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: kafka
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    depends_on:
      - zookeeper

  spark:
    image: bitnami/spark
    ports:
      - "8080:8080"
    environment:
      SPARK_MODE: master
      SPARK_RPC_AUTHENTICATION_ENABLED: "no"
      SPARK_RPC_ENCRYPTION_ENABLED: "no"
      SPARK_LOCAL_STORAGE_ENCRYPTION_ENABLED: "no"
      SPARK_SSL_ENABLED: "no"
    depends_on:
      - kafka
      - zookeeper

  monappli:
    image: mon-appli-scala
    depends_on:
      - spark
      - kafka
      - zookeeper
    environment:
      BOOTSTRAP_SERVERS: "kafka:9092"
      ZOOKEEPER: "zookeeper:2181"
    # Plus d'options d'environnement si nécessaire
```

Notez que dans le service `monappli`, `BOOTSTRAP_SERVERS` et `ZOOKEEPER` sont définis en fonction des noms des services dans `docker-compose.yml`.

Lancez votre environnement avec la commande suivante :

```bash
docker-compose up
```

Cela va démarrer Zookeeper, Kafka, Spark, et votre application Scala, tous dans des conteneurs séparés mais sur le même réseau Docker.

Assurez-vous que les paramètres de configuration dans votre code Scala correspondent aux noms des services et aux ports définis dans `docker-compose.yml`.

Si vous avez besoin d'exécuter des commandes spécifiques pour votre application Scala (comme soumettre un job Spark avec `spark-submit`), vous pouvez les ajouter à la section `command` de votre service `monappli` dans `docker-compose.yml`.

