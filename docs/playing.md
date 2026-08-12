# Playing a game — one human against two AIs

The shortest path from a clone to sitting at a table. For the rules of
*Puerto Rico* itself see [game-rules.md](game-rules.md).

## Start the server

```bash
./mvnw -pl puerto-rico-web spring-boot:run
```

The Spring Boot app serves the frontend from its own jar, so this one command
is the whole application — open <http://localhost:8080>.

First run only, or after pulling changes to another module:

```bash
./mvnw clean install
```

**Working on the frontend?** Run `npm run dev` in `puerto-rico-frontend`
alongside the server above and use <http://localhost:5173> instead — the Vite
dev server proxies `/api` to port 8080 and hot-reloads.

## Take a seat

1. **Create game.** A new table appears in the list as `OPEN`.
2. **Type your name**, then **Take a seat**. A badge confirms which seat you
   hold (`You are seat 0`).
3. **Seat a random AI**, twice. Three seats is the minimum a game needs, and
   the base game seats five at most.
4. **Start game.**

Your seat is remembered in this browser, per game. Reloading mid-game, or
reopening the game's URL later, puts you back in your seat rather than
turning you into a spectator.

## Play

When it is your turn a panel appears above the board:

> **Your turn — Choose a role**

Everything in it is clickable, and everything clickable is legal — the options
come from the server's own list of legal moves, so there is nothing to get
wrong. Click one and the board moves on; the AIs take their turns on their
own.

What you will be asked, phase by phase:

| Phase | What you click |
| --- | --- |
| Role selection | A role card. Doubloons piled on a card are yours if you take it. |
| Settler | A face-up plantation, a quarry, or "take no plantation" |
| Mayor | A tile or building with a free circle, then "Done placing colonists" |
| Builder | A building card — the coin on it is what you pay, discounts applied |
| Craftsman | The extra barrel your privilege earns you |
| Trader | A good to sell, with what it pays |
| Captain | Which ship to load, or your Wharf |
| Captain storage | Which goods to keep when the ships sail; the rest spoil |

The board below the panel shows the role track, the ships, the face-up
plantations, the trading house and the shared supplies, then every player's
island and city. Your own board is outlined and marked **You**; whoever is
acting is highlighted in gold. The log at the bottom says what everyone did.

Play continues until the game ends — a city filled, the colonist supply
exhausted, or the victory-point chips run out — and the final standings
appear on the board.

## Watching instead

Seat three AIs and no human, then **Start game**: the board plays itself.
Anyone can also watch a game in progress from the lobby list, or by opening
the game's `?game=<id>` URL — spectators see the same board without the
action panel.

## If something looks stuck

- **"Live updates disconnected — reconnecting."** The event stream dropped.
  It reconnects on its own and re-reads the board; nothing is lost.
- **The panel says it's your turn but you don't recognise the game.** Check
  the seat named in the top bar — one browser can hold a seat at several
  tables.
- **Nothing happens after a click.** A refused move is reported in the panel
  in red. If there is no message, the move was accepted and the board is
  waiting on another seat.
- **Games disappear.** Everything is held in memory: restarting the server
  ends every game in progress, and finished tables are reclaimed after 30
  minutes.
