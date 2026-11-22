---
navigation:
  title: Reporting bugs
  icon: minecraft:spyglass
  parent: settings.md
  position: 50
---

# Reporting bugs

Something does not work, an animal died without a reason you can see, or another mod does something strange? The bug
report form on GitHub ([github.com/0verEngineer/BettaCraft/issues](https://github.com/0verEngineer/BettaCraft/issues/new/choose))
helps you describe it; no field is required. Bettacraft can write down what it does, so the report shows the cause.

## 1. Switch on the debug log

`/bettacraft debug all on` (operators, or the owner of a single player world, also without cheats). From now on until
the game restarts, Bettacraft writes what happens and why into `logs/bettacraft-debug.log`:

* `care`: deaths with their reason (hunger, temperature, water values, old age, predators ...), the tank log
* `breeding`: nests, eggs, larvae that survive, young that are born
* `tank`: animals taken or refused (with the reason), the tank blocks
* `interior`, `network`: the interior, the miniature syncs and their size, the packets
* `compat`: other mods (Create, Better Fish Tanks, map mods ...) and their errors
* `render`: the miniatures on your screen
* `aquarist`: the jobs of the hired aquarist
* `world`: wild spawns

`/bettacraft debug care on` switches one area, `... off` switches it off, `... trace` writes even more (every care step,
every packet). `/bettacraft debug` shows what is on, `/bettacraft debug reset` goes back to the config.

To keep it on after a restart: `debug.logLevel` (INFO, DEBUG or TRACE) and `debug.areas` (`all` or a list of areas) in
`config/bettacraft-common.toml`, also in the config screen. The normal log stays clean: it gets info and warnings only,
and a warning that repeats comes at most once a minute with a count. The debug file has at most 10 MB, the one before it
is kept as `bettacraft-debug.1.log`. Switched off (the default), the debug log costs no performance.

## 2. Reproduce the problem

Do again what went wrong, or wait until it happens.

## 3. Write the report

Look at the tank the problem is about and type `/bettacraft report`. The file `logs/bettacraft-report-<date>.txt` has
the versions (Bettacraft, loader, Minecraft, Java), the integrations and all loaded mods, the settings you changed, the
data of the tank with its animals, the animal you look at and the end of the debug log. Click the path in the chat to
copy it; the link next to it opens the form. Drag the file into its field **Diagnosis**, together with a crash report
from `crash-reports/` if the game crashed.

Ideas and wishes have a form of their own on the same page.
