# Utilisez une image de base qui inclut Maven et le JDK
FROM maven:3.8.4-openjdk-17 as builder

# Définissez le répertoire de travail dans le conteneur
WORKDIR /app

# Copiez le fichier pom.xml et le code source dans le conteneur
COPY pom.xml .
COPY src ./src

# Exécutez Maven pour construire le fichier .jar de votre application
RUN mvn clean package
