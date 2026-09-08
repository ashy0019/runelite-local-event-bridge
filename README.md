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

## Security and privacy

- The destination is fixed in production code to IPv4 loopback (`127.0.0.1`) on port `41713`. There is no host, URL, or port configuration.
- The plugin does not make Internet requests, open browser links, launch external programs, use reflection/JNI/JNA, or read/write files.
- The bridge does not publish player names, account identifiers, positions, bank/equipment contents, raw inventory item IDs, NPC targets, menu entries, or world-state snapshots.

See [`PLUGIN_HUB_REVIEW.md`](PLUGIN_HUB_REVIEW.md) for the compact review surface and [`docs/local-event-protocol-v1.md`](docs/local-event-protocol-v1.md) for the wire contract.

## Development

Run tests:

```bash
./gradlew clean test
```

Launch RuneLite in developer mode with the plugin loaded:

```bash
./gradlew run
```
