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

- id `MM_ESM_Vanilla`, Java. The IntelliJ library paths only resolve under
  `D:\Games\StarSector\mods`, so build from the command line: `javac --release 17` against
  `D:\Games\StarSector\starsector-core\{starfarer.api,json,lwjgl,lwjgl_util}.jar` plus the
  MagicLib, LazyLib, LunaLib and Nexerelin jars from `D:\Games\StarSector\mods`, then
  `jar --create --file jars/MM_ExtraSubMarkets.jar -C <build dir> .`.
- Station and nebula generation only runs in `onNewGameAfterProcGen`: world changes need a new
  save.
- The game loads a separate copy at `D:\Games\StarSector\mods\$$$ Extra Submarkets (Babylon Ed)-2.0`.
  Andrzej copies the jar there by hand. Do not copy into the game folder.
- In PowerShell, run any cleanup that uses `Remove-Item` as its own command, apart from commands
  naming `C:\Program...` paths (the sandbox blocks the combination).
