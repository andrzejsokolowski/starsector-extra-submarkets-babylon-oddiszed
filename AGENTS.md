# The Everything Shop

Andrzej's Starsector mod: the Gate of Babylon, a nebula hidden in hyperspace whose stations sell
everything (every hull, weapon, wing, AI core, special item, blueprint, modspec and Exotica item).
It grew out of the Babylon edition of Massacre's Extra Submarkets and was released as its own mod
in 1.0.0 (before that, versions 3.1 to 3.2.1 were "Extra Submarkets - Oddisz+Babylon ED"). These
instructions apply to this repository, including when it is opened without the parent mod
workspace. The workspace `AGENTS.md` one folder up governs Git identity and communication.

## Notion

Tasks and history: the mod's board in Andrzej's Notion, under **Starsector mods**
(`collection://4ddc9708-d3a5-4446-9241-ee894aae1f65`; ticket ids still start with ESM). Working
rules and game API notes: **Claude Information** (`collection://7bf9b1d6-b9e5-4b62-87f1-757bbaa51919`),
starting with "Start here: working on Starsector mods through Notion". Read the board before
working, give every task a ticket and keep it current. The Notion tools are deferred: load them
with ToolSearch first.

Update the board during the work, not only at release time. Before changing files, put the
message on its ticket: a new request gets a new ticket (In progress), and a problem Andrzej
reports on Needs Human Test work moves that ticket to Changes Requested with the notes quoted.
Before ending the turn, update the ticket's body (What was done, How, Commits, Test list) and
its Status, Version and End date. Read the board with `notion-query-data-sources` mode `view`
on `view://3fd20ca7-5d20-8183-beb2-000c7c3d5397` or `notion-fetch` a ticket. SQL and rows mode share a
small quota that runs out. The workspace `AGENTS.md` has the full routine.

## Essentials

- id `the_everything_shop`, Java package `theeverythingshop`, ids prefixed `tes_`. Build with
  `python build.py`: it compiles `jars/TheEverythingShop.jar` with `javac --release 17` against the
  game and the library mods in `D:\Games\StarSector\mods` (MagicLib, LazyLib, LunaLib, Nexerelin,
  and Exotica Technologies as compile-only), then stages `TheEverythingShop/` and writes
  `TheEverythingShop-<version>.zip` in the root. `--jar-only` stops after the jar.
- Release metadata: `mod_info.json`, `the_everything_shop.version` (registered in
  `data/config/version/version_files.csv`), `changelog.txt`. GitHub repo
  `andrzejsokolowski/starsector-the-everything-shop`. No forum topic yet.
- The nebula and all ten stations (including The Treasury, `tes_treasury`) are only generated in
  `onNewGameAfterProcGen`: changes to them need a new save.
- Chicken (from Chicken's Tech Shop) was removed in 1.0.0; nothing of that mod is left. Treasury
  prices follow the player's standing with the Independents (`TreasuryShopBase.getTariff`).
- The original stores (AI, Elite, General Trade) and the nebula generator started as Massacre's
  code; the mod_info author line credits him.
- Exotica Technologies is touched only by `theeverythingshop.compat.ExoticaBridge`, through
  `ExoticaCompat`, which catches `LinkageError` so a missing or different Exotica fork just closes
  the Exotica shops.
- Which special items are sold: `theeverythingshop.items.SpecialItemRules` (plain items),
  `DataItemStock` (items that need data), `data/config/the_everything_shop/excluded_items.csv`
  (never sold).
- Andrzej installs releases through his mod manager. Do not copy into the game folder.
- In PowerShell, run any cleanup that uses `Remove-Item` as its own command, apart from commands
  naming `C:\Program...` paths (the sandbox blocks the combination).
