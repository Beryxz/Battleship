# Battleship

> Battleship Game \[Java\]

## Requirements

This project uses openjdk 8.0.232 with the bundled openjfk 8.0.202

(JavaFX required only by the client GUI)

## Project structure

- `battleship-server` Server project
- `battleship-client` Client project
- `battlehsip-util` Utils required by both project to build

## Build

### Server

`requires battleship.util.*`

```bash
cd Battleship/battleship-server/
./gradlew fatJar
```

### Client

`requires battleship.util.*`

```bash
cd Battleship/battleship-client/
./gradlew fatJar
```

## Protocol definition

//TODO
