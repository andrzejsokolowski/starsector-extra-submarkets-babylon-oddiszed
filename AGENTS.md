# Extra Submarkets (Oddisz + Babylon ED)

Andrzej's edition of the Babylon edition of Massacre's Extra Submarkets: the Gilgamesh Nebula
with stores selling every hull, weapon and wing. These instructions apply to this repository,
including when it is opened without the parent mod workspace. The workspace `AGENTS.md` one
folder up governs Git identity and communication.

## Notion

Tasks and history: the **Extra Submarkets (Oddisz + Babylon ED) board** in Andrzej's Notion,
under **Starsector mods** (`collection://4ddc9708-d3a5-4446-9241-ee894aae1f65`). Working rules and
game API notes: **Claude Information** (`collection://7bf9b1d6-b9e5-4b62-87f1-757bbaa51919`),
starting with "Start here: working on Starsector mods through Notion". Read the board before
working, give every task a ticket (ticket ids start with ESM) and keep it current. The Notion
tools are deferred: load them with ToolSearch first.

Update the board during the work, not only at release time. Before changing files, put the
message on its ticket: a new request gets a new ticket (In progress), and a problem Andrzej
reports on Needs Human Test work moves that ticket to Changes Requested with the notes quoted.
Before ending the turn, update the ticket's body (What was done, How, Commits, Test list) and
its Status, Version and End date. Read the board with `notion-query-data-sources` mode `view`
on `view://3fd20ca7-5d20-8183-beb2-000c7c3d5397` or `notion-fetch` a ticket. SQL and rows mode share a
small quota that runs out. The workspace `AGENTS.md` has the full routine.

## Essentials

- id `MM_ESM_Vanilla`, Java. Build with `python build.py`: it compiles the jar with `javac --release 17`
  against the game and the library mods in `D:\Games\StarSector\mods` (MagicLib, LazyLib, LunaLib,
  Nexerelin, and Exotica Technologies as compile-only), then stages `ExtraSubmarketsOddisz/` and writes
  `ExtraSubmarketsOddisz-<version>.zip` in the root. `--jar-only` stops after the jar.
- Release metadata: `mod_info.json`, `extra_submarkets_oddisz.version` (registered in
  `data/config/version/version_files.csv`), `changelog.txt`. No forum topic.
- Chicken's Roost (station `esm_chicken_roost`, Chicken = person `esm_chicken`) and her shops are created
  on every game load when missing, so existing saves get them. The other stations and the nebula are
  only generated in `onNewGameAfterProcGen`: changes to them need a new save.
- Chicken, her missions and the shop code came from Chicken's Tech Shop by Chickenboy (MIT); keep
  `LICENSE-ChickenTechShop.txt` in the release.
- Exotica Technologies is touched only by `data.scripts.compat.ExoticaBridge`, through `ExoticaCompat`,
  which catches `LinkageError` so a missing or different Exotica fork just closes the Exotica shops.
- Which special items are sold: `data.scripts.chicken.SpecialItemRules` (plain items), `DataItemStock`
  (items that need data), `data/config/esm_chicken/excluded_items.csv` (never sold).
- Andrzej installs releases through his mod manager. Do not copy into the game folder.
- In PowerShell, run any cleanup that uses `Remove-Item` as its own command, apart from commands
  naming `C:\Program...` paths (the sandbox blocks the combination).
