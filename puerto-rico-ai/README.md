# puerto-rico-ai — AI Engine Plugins

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Houses the individual AI opponent implementations — starting simple (e.g.
random or greedy heuristic play), with room to add stronger engines later
without changing anything else in the system. Each engine fulfills the
same decision-making role, so the rest of the system treats every AI
interchangeably regardless of its internal sophistication. An engine's
only job is to produce a choice when asked — no game-state mutation, no
I/O.

AI integration is **in-process**: engines are Java plugins running in the
same JVM as everything else, not separate services talking over a network
protocol. That's a deliberate scoping decision, not a placeholder.

## Contracts

Implements the **Decision contract** defined by `puerto-rico-session`:

```java
public final class RandomAi implements Actor {
  public RandomAi(String name, long seed, Duration thinkTime);
}

public final class AiRegistry {
  public AiRegistry(Duration thinkTime);
  public List<AiEngineInfo> available();
  public Optional<Actor> create(String engineId, String displayName, long seed);
}
```

`AiRegistry` answers the discoverability requirement: `puerto-rico-web`
lists `available()` engines for `GET /api/ai/engines` and calls `create`
when seating one. Reads game state and legal-option types from
`puerto-rico-model`.

## Depends on

`puerto-rico-model`, `puerto-rico-session`.

## Design notes

**`RandomAi` shares its selector with `puerto-rico-model`'s own tests.**
The uniform-choice logic lives in `com.PRS.model.engine.RandomPlay` —
`RandomAi` is a thin `Actor` wrapper around it — so the model's fuzz tests
(`GameEngineContractTest`) and this module's seeded AI provably run the
same code, not two implementations that happen to agree today.

**`thinkTime` paces the game, not the selection.** A zero-delay AI resolves
a full game in milliseconds — fine for tests, useless for a spectator
trying to watch one. `RandomAi` schedules its answer via {@code
CompletableFuture.delayedExecutor} rather than sleeping the calling thread,
so a slow-thinking AI never blocks anything while it "thinks." Tests
construct engines with `Duration.ZERO`; `puerto-rico-web` seats them with a
few hundred milliseconds.

## Testing

TestNG with AssertJ assertions, matching `puerto-rico-model`,
`puerto-rico-session`, and `puerto-rico-lobby`.

| Test class | Covers |
|---|---|
| `RandomAiTest` | Every `decide` answer is among the offered options; the same seed replays an identical game; a different seed diverges; `thinkTime` delays completion without blocking the caller |
| `AiRegistryTest` | `available()` lists the random engine; `create` with a known id returns an actor carrying the given display name; an unknown id returns empty rather than throwing |

```bash
./mvnw -pl puerto-rico-ai -am verify
```
