# Puerto Rico — Rules Reference

> **Accuracy caveat:** this document is drafted from general knowledge as a
> starting reference for implementation work, not transcribed from the
> official rulebook. Structure and mechanics should be reliable, but treat
> every specific number below (costs, VP values, tile/colonist counts,
> trading house capacity, etc.) as **unverified** until checked against an
> official rulebook. Where a detail is genuinely uncertain, it's marked
> inline with "(confirm exact ...)" rather than stated as fact. Scope is the
> current/main edition of the base game only — no expansions.

## 1. Overview & Objective

Puerto Rico is an economic strategy board game designed by Andreas Seyfarth,
for 3–5 players (an official 2-player variant uses a neutral "phantom"
third player). Each player is a colonial plantation owner/governor,
developing plantations, producing and shipping goods, and constructing
buildings. Each round, players take turns selecting one of several **role**
cards; the chosen role grants the selecting player a small privilege and
triggers the same action for every player in turn order. The game ends when
one of several end conditions triggers; the player with the most victory
points (VPs) wins. Doubloons (money) barely factor into final score —
shipping and building are what earn VPs.

## 2. Components

- **Player boards** (one each): plantation/quarry area, building area,
  space to track colonists, doubloons, and stored goods.
- **Central board area**: role cards, ships (for shipping), the colonist
  ship + general colonist supply, the trading house, the VP chip supply,
  the doubloon supply, and the face-down plantation/quarry tile stack.
- **Plantation tiles**: five good types — corn, indigo, sugar, tobacco,
  coffee — plus quarry tiles (a building-cost discount instead of a good).
- **Building cards**: production buildings (process a raw plantation good
  into its sellable/shippable form; corn needs no processing) and
  "violet"/other buildings with special powers, across a range of costs.
- **Role cards** (7 total): Settler, Mayor, Builder, Craftsman, Trader,
  Captain, Prospector. Not all 7 are in play below 5 players — see
  Section 4.
- **Ships**: several ships of differing cargo capacity, each carrying only
  one good type at a time.
- **Colonists** (wooden discs): a limited shared supply, used to staff
  plantations, quarries, and building colonist-slots.
- **Doubloons** and **victory point chips**: separate limited supplies.

## 3. Setup

1. Give each player a player board.
2. Determine turn order; give each player a starting doubloon amount that
   increases with later turn-order position, to offset the disadvantage of
   picking roles later each round (confirm exact per-position amounts).
3. Give each player one colonist and one starting plantation tile already
   placed on their board, so turn one has something to work with (confirm
   exact starting tile — most likely indigo).
4. Shuffle the plantation/quarry tiles face-down; reveal a small face-up
   selection (count scales with player count).
5. Place colonists on the "colonist ship" space and in the general supply.
6. Set out role cards matching player count. The number of roles in play
   scales with player count — at fewer than 5 players, one or more roles
   are set aside face-down for the round; any doubloon that would be placed
   on a set-aside role accumulates on it until a round where it's back in
   play (confirm exact roles-in-play-per-player-count table).
7. Randomly choose the first round's Governor (the player who picks first;
   this role rotates each round).

## 4. Turn Structure & Roles

Each round, going in turn order starting from that round's Governor, every
player gets exactly one turn to pick an available role card:

1. The current picker selects one face-up role card, collecting any
   doubloon(s) sitting on it (accumulated from rounds it went unpicked).
2. Starting with the picker and proceeding in turn order, every player
   performs that role's action, if willing/able (most role actions are
   optional per-player; shipping under Captain is the exception — see
   below).
3. The card is turned face-down for the rest of the round (unavailable
   until next round).
4. Play passes to the next player in turn order, who picks the next
   available role, until every player has picked exactly once. Any role
   left unpicked at round end gains a doubloon and stays available next
   round.
5. The Governor marker passes to the next player in turn order and a new
   round begins with all role cards available again.

Role effects:

- **Settler**: selecting player gets a first-pick/extra privilege on
  plantation tiles this turn (confirm exact privilege — likely first choice
  and/or an exception to the quarry-limit rule below). Then, in turn order,
  each player may take one available plantation or quarry tile onto their
  board. Quarries may be limited to players below a certain plantation
  count, to slow early quarry-hoarding (confirm exact restriction).
- **Mayor**: a new colonist is added to the colonist ship from the supply;
  the selecting player gets an extra colonist. Then, in turn order, each
  player places colonists from the general supply (and may recall/reassign
  colonists already on their board) onto plantation, quarry, and building
  slots. Colonists a player can't or doesn't place return to the general
  supply.
- **Builder**: selecting player gets a discount on the building they
  construct this turn. Then, in turn order, each player may buy and
  construct one building onto an open building space, paying its doubloon
  cost minus 1 per staffed quarry they own (confirm minimum-cost floor).
- **Craftsman**: selecting player gets one bonus unit of a good they
  produce this turn. Then, every player produces: each staffed plantation
  of a processed good type (indigo/sugar/tobacco/coffee) yields one unit
  only if there's also a staffed matching production building with an open
  slot; staffed corn plantations yield corn directly, no building needed.
  Production of a given good is capped if the bank runs out of that good's
  tokens.
- **Trader**: selecting player gets a bonus doubloon when they sell. Then,
  in turn order, each player may sell one good from storage to the trading
  house for doubloons (base price varies by good — coffee highest, corn
  lowest, confirm exact prices — boosted by Small/Large Market). The
  trading house holds a limited number of distinct good types at once; a
  type already present can't be sold again until cleared (the Office
  building lets its owner bypass this). Confirm exact trading house
  capacity and clearing mechanic.
- **Captain**: selecting player gets a bonus (likely a VP chip) tied to
  shipping this turn (confirm exact bonus). Then, in turn order, shipping
  is **mandatory** for any player holding a shippable good: load as many
  units of one good type as possible onto a ship (each ship carries only
  one good type at a time, up to its capacity); shipping earns VP chips,
  not doubloons — this is the game's primary VP engine. The Wharf building
  gives its owner a private ship outside the shared fleet; Harbor pays its
  owner a bonus doubloon per unit shipped. Goods that can't be shipped or
  stored (over warehouse capacity) are lost at round end.
- **Prospector**: only the selecting player is affected — they simply
  receive a doubloon bonus from the bank (confirm flat amount vs.
  variable). No action for other players.

## 5. Buildings

- Cost in doubloons, reduced by 1 per staffed quarry the buyer owns
  (confirm minimum-cost floor).
- Each building has one or more colonist slots; most need at least one
  staffed slot to function (produce goods / activate their power); a few
  "violet" buildings may work unstaffed (confirm which).
- Buildings are permanent once built — they count toward end-game building
  VPs regardless of current staffing, even though ongoing effects like
  production still require staffing.
- Building space per player board is limited (confirm exact slot count and
  whether large buildings have a separate sub-limit).

**Production buildings** (increasing cost/capacity tiers):
Small Indigo Plant, (Large) Indigo Plant, Small Sugar Mill, (Large) Sugar
Mill, Tobacco Storage, Coffee Roaster. (Corn needs no production building.)

**Violet / other buildings** (increasing cost/impact tiers):
Small Market, Hacienda, Construction Hut, Small Warehouse, Large Market,
Hospice, Office, Large Warehouse, Factory, University, Harbor, Wharf,
Guild Hall, Residence, Fortress, Custom House, City Hall.

Purpose per building (for implementation planning — exact costs/VP
values/slot counts need an authoritative source):
- **Small/Large Market** — bonus doubloons when selling.
- **Hacienda** — bonus plantation tile(s).
- **Construction Hut** — bonus/alternate plantation-tile acquisition.
- **Small/Large Warehouse** — extra goods storage (reduces production
  loss).
- **Hospice** — bonus colonist(s).
- **Office** — allows selling a good type already present at the trading
  house.
- **Factory** — bonus doubloons scaling with goods sold.
- **University** — bonus colonist when built.
- **Harbor** — bonus doubloon per unit shipped.
- **Wharf** — private shipping capacity independent of the shared ships.
- **Guild Hall** — end-game VP bonus from production buildings built.
- **Residence** — end-game VP bonus from filled plantation/quarry spaces.
- **Fortress** — end-game VP bonus from total colonists on the board.
- **Custom House** — end-game VP bonus from doubloons earned via shipping.
- **City Hall** — end-game VP bonus from non-production buildings built.

## 6. Plantations, Quarries & Goods Production

- Plantation tiles (corn, indigo, sugar, tobacco, coffee) and quarry tiles
  are acquired via the Settler role and staffed with a colonist (via Mayor)
  to become productive.
- A staffed quarry reduces the doubloon cost of buildings its owner
  constructs; it does not produce a good.
- A staffed plantation of a processed good type only yields that good (via
  Craftsman) if the owner also has a staffed matching production building
  with an open slot. Corn is the exception: a staffed corn plantation
  yields corn directly, no building required.
- Each good has a limited bank supply; production of that good is capped
  for the turn if the bank runs out.
- Produced goods sit in storage until sold or shipped; base storage
  capacity is small and expandable via Small/Large Warehouse. Anything
  produced beyond capacity that isn't sold/shipped the same round is lost.

## 7. Colonists

- A shared limited supply. Enter play via the colonist ship (refilled each
  round from the general supply) and the Mayor role.
- Placed on plantations, quarries, and building slots to activate them; can
  be recalled and reassigned during a player's Mayor turn.
- The shared supply depleting is one of the end-game triggers (Section 9),
  so colonist-heavy strategies affect game pacing.

## 8. Selling & Shipping

- **Selling** (Trader role): convert stored goods to doubloons at the
  trading house. Base price varies by good type, boosted by Small/Large
  Market. The trading house's limited slots restrict how many distinct good
  types can be present before a type must clear (Office bypasses this for
  its owner).
- **Shipping** (Captain role, mandatory for players holding a shippable
  good): load goods onto the shared ships (one good type per ship at a
  time, up to capacity) to earn VP chips — the primary VP engine in the
  game, more so than building. Wharf bypasses the shared ships entirely for
  its owner; Harbor pays a doubloon bonus per unit shipped.
- Goods that can't be sold or shipped and exceed storage capacity are lost
  at round end.

## 9. Victory Point Scoring

Two sources, summed at game end:
1. **Ongoing VP chips**, banked during play — mainly from shipping
   (Captain), plus occasional role-selection bonuses. Drawn from a limited
   shared supply; that supply running out is one of the end-game triggers.
2. **End-game building VPs**, tallied only when the game ends: each built
   building has a printed VP value, plus Guild Hall / Residence / Fortress
   / Custom House / City Hall grant additional bonus VPs computed from
   board state (production buildings built, filled plantation spaces,
   total colonists, shipping doubloons earned, and non-production
   buildings built, respectively).

Leftover doubloons and unsold/unshipped goods do not count toward final
score.

## 10. End-Game Trigger

The current round finishes, then the game ends immediately (before a new
round starts) if, at any point during that round, any **one** of these
becomes true:
1. The VP chip supply is exhausted (can't pay out a chip that's owed).
2. The colonist supply (ship + general supply combined) is exhausted.
3. Any one player has filled every building space on their board.

Whichever triggers first ends the game after the current round completes;
final scoring (Section 9) is then tallied for every player, and the
highest total wins.

## 11. Glossary

- **Doubloon** — the game's currency; spent on buildings, otherwise mostly
  irrelevant to final score.
- **Colonist** — worker piece; staffs plantations/quarries/buildings to
  activate them.
- **Plantation** — a tile producing a raw good once staffed; a quarry
  produces a building-cost discount instead.
- **Production building** — converts a raw plantation good into its
  sellable/shippable form (every good except corn needs one).
- **Violet / other building** — a non-production building granting a
  special power and/or end-game bonus VPs.
- **Victory point (VP) chip** — the primary scoring currency, earned mainly
  through shipping.
- **Governor** — the round's starting picker; rotates each round.
- **Role card** — one of the 7 action cards chosen exactly once per player
  per round.

## Sources

_Record the exact rulebook edition/printing/PDF consulted, and the date
verified, once this document has been checked against an official source._
