package com.brockenreich.landplugin.farm

import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * Marks/detects the "animated" state /farm animate grants an item - the only state in which
 * FarmListener.onPlace will actually let one of the 14 tracked crop items be planted. The visible
 * glint comes from a hidden Unbreaking enchant (HIDE_ENCHANTS keeps it out of the tooltip's
 * enchantment list) - the item's own name (e.g. "빨간 버섯" vs "갈색 버섯", or a specific sapling
 * species) is left completely untouched, since overwriting it with our own generic per-type label
 * would lose exactly that distinction.
 */
class FarmItems(plugin: Plugin) {

    private val animatedKey = NamespacedKey(plugin, "farm_animated")

    fun isAnimated(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(animatedKey, PersistentDataType.BYTE)
    }

    fun animate(item: ItemStack) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(animatedKey, PersistentDataType.BYTE, 1)
        meta.addEnchant(Enchantment.UNBREAKING, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        item.itemMeta = meta
    }
}
