---
navigation:
  title: Pipe bundles
  icon: bettacraft:glass_pipe
  parent: equipment/multiblock_filter.md
  position: 2
---

# Pipe bundles

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:stainless_steel_pipe" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:glass_pipe" z="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:filter_chamber" x="2" p:medium="ceramic" />
</GameScene>

![A row of pipe bundles carrying several lines between an aquarium and the filter](../images/pipe_bundle.png)

Up to **four pipes run in one block**, like the conduits of other mods: **sneak + right-click** a pipe with a glass or
stainless steel pipe and both run on in a **pipe bundle**; more go in the same way, up to four. Each pipe of the bundle is
its own line with its own connections, glass or steel, and its own role and ring (from the [connectors](filter_lines.md) its
line reaches): the pipes **never mix**, also two lines of the same role (two intake lines side by side). Sneak-right-click a
bundle with an empty hand to take the pipe you look at out again; with one pipe left the block is a plain pipe again.
Breaking a bundle gives every pipe back.

Between two bundles every pipe keeps to its own corner of the steel manifold and joins the pipe in the same corner of the
next bundle, so the bundles line up. A single pipe, a filter connector or another device next to a bundle meets **one** of
its pipes: the one that already leads there, else the one with an open end. Aquarium glass is met by all of them side by
side. The <ItemLink id="bettacraft:pipe_wrench" /> works on the pipe of the bundle you look at; **sneak + right-click** with
it moves that pipe to the next corner, to line it up with the next bundle.

Where a pipe leaves the bundle into a single pipe (or a filter connector, an aquarium), its arm comes out of the **middle**
of the manifold, in line with that pipe.

The fresh and waste water ports of the filter are gone: their pipes are plain stainless steel pipes now.
