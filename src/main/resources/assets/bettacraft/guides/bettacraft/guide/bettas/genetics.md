---
navigation:
  title: Genetics
  icon: minecraft:written_book
  parent: bettas.md
  position: 30
---

# Genetics

Every betta carries **two alleles** for each of nine traits (loci): one from the mother, one from the father. The
dominant allele decides what you see; the other one stays hidden and can show up in the offspring. The tank menu shows
the full genotype of every fish (the first allele is the visible one) and the tooltip lists all nine loci.

| Locus | Alleles, dominant first |
|---|---|
| Tail | roundtail > spadetail > veiltail > delta > halfmoon > rosetail > crowntail > doubletail |
| Fin length | short (plakat) > long |
| Pectorals | normal > dumbo (big "elephant ear" pectoral fins) |
| Body colour | about 40 colours, darker colours dominate |
| Fin colour | the same colours, separate from the body (bicolour fish) |
| Pattern | marble > dragon scale > mask > koi > galaxy > grizzle > piebald > dalmatian > butterfly > solid > fade, bars, tipped |
| Pattern colour | colour of the pattern |
| Iridescence | high > low > none |
| Sheen colour | colour of the iridescence |

## Inheritance

For each locus a baby gets one random allele of the mother and one of the father (classic Mendel genetics). The sex is
50 % male, 50 % female. Each allele also has a small chance to **mutate** into a random other allele
(`breeding.mutationChance`, 1 %).

Example: two halfmoons that both carry a hidden veiltail (veiltail / halfmoon) give about 75 % veiltails and 25 %
halfmoons, because veiltail is dominant over halfmoon.

The [breeding tab](../breeding.md) of the tank menu calculates this for you: once an adult pair is in the tank it shows
the expected offspring per locus (a Punnett square) together with the genotypes of both parents.

## Pedigree

Every betta has its own id, a name (rename it in the menu), mother and father, its generation and its birth time. Wild
and bought fish are generation 0, their babies generation 1 and so on.

## Datapacks

Colours, genes and named varieties are data-driven: `data/<namespace>/bettacraft/colors/*.json`,
`.../genes/<locus>.json` (rank and frequency per allele) and `.../varieties/*.json`. Datapacks can add colours and
change dominance or frequencies; new tail models or pattern masks need a resource pack and code.
