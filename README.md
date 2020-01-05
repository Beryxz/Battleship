# Battleship

> Battleship Game \[Java\]

## Description

Implementation of the [Battleship](https://en.wikipedia.org/wiki/Battleship_(game)) game in Java.

Works with 2 players on a 10x10 Grid with 7 ships:

| Length | Amount |
|:------:|:------:|
|1|2|
|2|2|
|3|1|
|4|1|
|5|1|

Ships can't overlap or be adjacent.

On a side note, Client GUI has permanent Dark Mode.

## Requirements

This project uses openjdk 8 with the bundled openjfk 8

(JavaFX is required only by the client GUI)

## Run

By default the server listen for connections on the port `12345`

### Client

```bash
java -jar battleship-client-[CLIENT_VERSION].jar
```

### Server

```bash
java -jar battleship-server-[CLIENT_VERSION].jar
```

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

All `X`'s and `Y`'s coordinates are in the range `1-10`.

As soon as a player sank all the opponent ships, the game ends.

### Ships

Ships are in the format `XXYYHLL`

- `xx` Column
- `yy` Row
- `[HV]` Orientation
- `LL` Length

### Shoot

Shoots are in the format `SHOOT_XXYY`

- `xx` Column
- `yy` Row

### Description

Upon connecting to the server, `OPPONENT_WAIT` is sent and client must wait until another player is matched.
When an opponent is found `OPPONENT_FOUND` is then received by the client.

`SEND_GRID` informs the client that server is ready to receive the ships layout. Client should respond with all 7 ships joined by '_' character (the order doesn't matter).

If the grid complies with the requirements, `GRID_OK` is sent.
Otherwise `GRID_ERR` is sent and server asks for a new grid.

When both players send a valid grid, `GAME_START` is sent and the game begins.

A player turn start's with `TURN_START` and ends with `TURN_END`.
After a client shoot in his turn, the server may respond with:

|Response|Description|
|--------|-----------|
|HIT|A ship was hit but not sank.|
|OCEAN|No ships were hit.|
|SANK(_XXYY){1,n}|A ship has been sunk. The cells of the sank ship are joined by '_' and returned next to the response. E.g. SANK_0101_0102|
|DUPLICATE|This cell has already been shot. Try another one.|

In case of HIT, OCEAN and SANK_..., the same response is sent to the other player. In addition, for the HIT and OCEAN responses, the cell shot is added next to response e.g. HIT_0101.

After a shoot, if a player sank all the opponent ships, `WIN` and `LOST_...` are sent to the corresponding players and the game ends.

`LOST_XXYY_...` message contains all the remaining ships cells.

if during the match or during the placement of the ships, the opponent disconnects, the server ends the game and send `WIN_OPPONENT_DC` to the player.

### Heartbeat

Managed by the Server classes: `HeartbeatManager`, `HeartbeatClient`.

Server requires that the client is constantly up. To ensure client availability, if nothing is received after the amount of time defined in `disconnectTimeout` (2s by default) the client is considered unavailable and is disconnected from the server.

To keep a client alive a constant sending of `PING` messagges before the disconnect timeout runs out, is required.
