package com.brockenreich.landplugin.farm

import org.bukkit.Material

/**
 * The items /farm animate can enchant. `autoGrows` marks whether AreaFarmManager schedules
 * growth for it once planted. Most crops get a single fixed-time full-growth; cactus/sugar
 * cane/bamboo instead grow one height segment at a time, repeating indefinitely (see
 * FarmManager.HEIGHT_TYPES) - they're treated as shared community resources, not any one
 * player's crop, so their base segment can only be broken by an OP (see FarmListener), keeping
 * the plant itself alive for everyone to keep harvesting from the segments above it.
 */
enum class FarmCropType(val label: String, val displayName: String, val autoGrows: Boolean, val defaultMinutes: Double) {
    SWEET_BERRY("sweetBerry", "달콤한 열매", true, 1.0),
    POTATO("potato", "감자", true, 4.0),
    CARROT("carrot", "당근", true, 10.0),
    BEETROOT("beetrootSeeds", "비트 씨앗", true, 15.0),
    MELON("melonSeeds", "수박 씨앗", true, 30.0),
    PUMPKIN("pumpkinSeeds", "호박 씨앗", true, 60.0),
    WHEAT("wheatSeeds", "밀 씨앗", true, 60.0),
    NETHER_WART("netherWart", "네더 와트", true, 30.0),
    MUSHROOM("mushroom", "버섯", true, 120.0),
    // Oak/spruce/birch/acacia/cherry saplings and mangrove propagules - a normal 1x1 tree.
    // Jungle and dark oak are split out below: a lone jungle sapling still grows (much slower),
    // while dark oak can only ever grow as a 2x2 "big tree" - see FarmListener.onPlace, which
    // detects a completed 2x2 of either species and schedules JUNGLE_BIG_TREE/DARK_OAK_SAPLING
    // for all 4 corners instead of the lone duration.
    SAPLING("sapling", "나무 묘목", true, 180.0),
    JUNGLE_SAPLING("jungleSapling", "정글 나무 묘목", true, 2160.0),
    JUNGLE_BIG_TREE("jungleBigTree", "정글 큰 나무", true, 10080.0),
    DARK_OAK_SAPLING("darkOakSapling", "짙은 참나무 묘목", true, 480.0),
    COCOA("cocoa", "코코아", true, 60.0),
    CACTUS("cactus", "선인장", true, 60.0),
    SUGAR_CANE("sugarCane", "사탕수수", true, 180.0),
    BAMBOO("bamboo", "죽순", true, 120.0);

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
            put(Material.CRIMSON_FUNGUS, MUSHROOM)
            put(Material.WARPED_FUNGUS, MUSHROOM)
            // Item-side mapping only: which JUNGLE_SAPLING/JUNGLE_BIG_TREE or DARK_OAK_SAPLING a
            // held item resolves to is irrelevant here since /farm animate only cares whether the
            // material is farm-manageable at all - the jungle/dark-oak 2x2 distinction is a
            // planting-time decision made in FarmListener, not an item-type one.
            put(Material.JUNGLE_SAPLING, JUNGLE_SAPLING)
            put(Material.DARK_OAK_SAPLING, DARK_OAK_SAPLING)
            put(Material.AZALEA, SAPLING)
            put(Material.FLOWERING_AZALEA, SAPLING)
            put(Material.COCOA_BEANS, COCOA)
            put(Material.CACTUS, CACTUS)
            put(Material.SUGAR_CANE, SUGAR_CANE)
            put(Material.BAMBOO, BAMBOO)
            Material.entries
                .filter {
                    (it.name.endsWith("_SAPLING") && it != Material.JUNGLE_SAPLING && it != Material.DARK_OAK_SAPLING) ||
                        it.name == "MANGROVE_PROPAGULE"
                }
                .forEach { put(it, SAPLING) }
        }

        /** Which FarmCropType a held item (seed/sapling/etc.) belongs to, or null if it isn't one of the 14. */
        fun forItem(material: Material): FarmCropType? = byItemMaterial[material]
    }
}
