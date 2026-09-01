package com.brockenreich.landplugin.farm

import org.bukkit.Material

/**
 * The 14 items /farm animate can enchant. `autoGrows` marks whether AreaFarmManager schedules a
 * fixed-time full-growth for it once planted - the three height-growing plants (cactus, sugar
 * cane, bamboo) are excluded from that (per design decision: they keep growing however tall
 * naturally, so a single "fully grown" timer doesn't apply the same way it does to an age-capped
 * crop) and are only unlocked for planting, nothing more.
 */
enum class FarmCropType(val label: String, val displayName: String, val autoGrows: Boolean, val defaultMinutes: Double) {
    SWEET_BERRY("sweetBerry", "달콤한 열매", true, 15.0),
    POTATO("potato", "감자", true, 20.0),
    CARROT("carrot", "당근", true, 20.0),
    BEETROOT("beetrootSeeds", "비트 씨앗", true, 15.0),
    MELON("melonSeeds", "수박 씨앗", true, 30.0),
    PUMPKIN("pumpkinSeeds", "호박 씨앗", true, 30.0),
    WHEAT("wheatSeeds", "밀 씨앗", true, 20.0),
    NETHER_WART("netherWart", "네더 와트", true, 25.0),
    MUSHROOM("mushroom", "버섯", true, 40.0),
    SAPLING("sapling", "나무 묘목", true, 30.0),
    COCOA("cocoa", "코코아", true, 20.0),
    CACTUS("cactus", "선인장", false, 0.0),
    SUGAR_CANE("sugarCane", "사탕수수", false, 0.0),
    BAMBOO("bamboo", "죽순", false, 0.0);

    companion object {
        private val byItemMaterial: Map<Material, FarmCropType> = buildMap {
            put(Material.SWEET_BERRIES, SWEET_BERRY)
            put(Material.POTATO, POTATO)
            put(Material.CARROT, CARROT)
            put(Material.BEETROOT_SEEDS, BEETROOT)
            put(Material.MELON_SEEDS, MELON)
            put(Material.PUMPKIN_SEEDS, PUMPKIN)
            put(Material.WHEAT_SEEDS, WHEAT)
            put(Material.NETHER_WART, NETHER_WART)
            put(Material.RED_MUSHROOM, MUSHROOM)
            put(Material.BROWN_MUSHROOM, MUSHROOM)
            put(Material.COCOA_BEANS, COCOA)
            put(Material.CACTUS, CACTUS)
            put(Material.SUGAR_CANE, SUGAR_CANE)
            put(Material.BAMBOO, BAMBOO)
            Material.entries
                .filter { it.name.endsWith("_SAPLING") || it.name == "MANGROVE_PROPAGULE" }
                .forEach { put(it, SAPLING) }
        }

        /** Which FarmCropType a held item (seed/sapling/etc.) belongs to, or null if it isn't one of the 14. */
        fun forItem(material: Material): FarmCropType? = byItemMaterial[material]
    }
}
