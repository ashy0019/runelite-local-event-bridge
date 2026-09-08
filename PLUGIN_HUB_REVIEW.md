# Plugin Hub review surface

This document summarizes the behavior of Local Event Bridge for code review. It is descriptive only; the Java source remains authoritative.

## Purpose

The plugin converts a fixed allowlist of RuneLite observations into a small, source-neutral event protocol for companion applications running on the same computer.

## Network behavior

- Outbound TCP client only.
- Destination is fixed to `127.0.0.1:41713` in production code.
- No hostname lookup, LAN address, Internet URL, user-configurable host, or user-configurable port.
- RuneLite startup does not wait for a consumer. Connection/reconnect work is performed on a daemon executor.
- Transient events are not buffered across disconnects. State snapshots are coalesced to their latest value.

The four-argument `LocalEventPublisher` constructor that accepts a port is package-private and exists only so protocol tests can bind an ephemeral local port. Production plugin code can only use the constructor that targets `LoopbackEndpoint.DEFAULT_PORT`.

## Source-to-consumer messages

The source can send only:

- `hello`
- `event`
- `state`
- `reset`

The consumer can return only:

- `hello_ack`
- `error`

There is no protocol message for mouse/keyboard input, menu invocation, movement, interaction, chat input, prayer switching, or any other RuneLite/game action.

## Published data

The v1 capability allowlist contains:

- experience changes: skill identifier, XP delta/current values, level transition
- chat: classification plus message text
- resources: hitpoints, prayer, special attack current/maximum values
- inventory: filled-slot count and capacity only
- status: poison/venom state
- loot: stack count and aggregate value only
- local-player death observation
- notification focus-policy facts

The bridge does **not** publish player/account names, credentials, coordinates, bank contents, equipment, raw inventory item IDs, other-player state, NPC targeting, menu entries, or scene/world snapshots.

## Local machine behavior

The plugin does not:

- execute external programs
- dynamically load or download code
- use reflection, JNI, JNA, Unsafe, or native-memory access
- read or write files
- make HTTP/HTTPS requests
- register overlays or modify menus/widgets
- inject mouse or keyboard input

## Dependencies

The production source uses RuneLite-provided APIs and injected Gson. The repository adds no production runtime dependency beyond RuneLite. `runelite-plugin.properties` uses `build=standard`.

## Compatibility identifiers

Protocol v1 currently retains the historical wire identifiers `hapticscape-local-source` and `hapticscape-local-events` so existing local consumers continue to interoperate. They are protocol strings only; this repository contains no HapticScape application code or dependency. A future protocol version can rename them with an explicit compatibility transition.
