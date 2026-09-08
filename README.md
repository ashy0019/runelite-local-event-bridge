# Local Event Bridge

A minimal RuneLite Plugin Hub bridge that publishes a fixed allowlist of gameplay observations to local companion applications over `127.0.0.1:41713`.

The bridge is intentionally one-way with respect to RuneLite gameplay: it observes RuneLite events and publishes local event/state frames. The local protocol has no command for clicking, typing, menu actions, movement, or other game control.

## Published capabilities

- experience changes
- chat classification/text
- hitpoints, prayer, and special-attack resources
- inventory occupancy
- poison/venom status
- loot aggregate value
- local-player death
- RuneLite notification focus-policy facts

## Local transport

The plugin connects only to IPv4 loopback (`127.0.0.1`) on port `41713`. Network I/O runs off the RuneLite client thread. Transient events are best-effort and are not replayed after disconnect; current state is coalesced and replayed after reconnect.

## Development

Run tests:

```bash
./gradlew clean test
```

Launch RuneLite in developer mode with the plugin loaded:

```bash
./gradlew run
```
