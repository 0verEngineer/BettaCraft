package org.overengineer.bettacraft.platform.registry;

/** M52b: implemented by {@code Item.Properties} through {@code ItemPropertiesMixin}. */
public interface BlockPrefixProperties {
    /** Use the block's name ({@code block.<ns>.<path>}) unless another name was set. */
    void bettacraft$blockPrefixIfDefault();
}
