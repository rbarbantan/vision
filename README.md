# Vision
Snaps a picture from the webcam and replaces all text with a different font.
Uses Google Vision API and Spring Boot

## Setup
Make sure you have set up you API ccredentials

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your-project-credentials.json
```

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/vision-1.0-SNAPSHOT.jar
```

##Expected output
<img alt="coffee" src="img/coffee.png?raw=true" width="640">
