package com.brockenreich.landplugin.economy

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

class EconomyManager(private val dataFolder: File, private val logger: Logger) {

    private val file = File(dataFolder, "economy.yml")
    private val balances: MutableMap<UUID, Long> = mutableMapOf()

    fun balance(uuid: UUID): Long = balances[uuid] ?: 0L

    fun setBalance(uuid: UUID, amount: Long) {
        if (amount <= 0) balances.remove(uuid) else balances[uuid] = amount
        save()
    }

    fun deposit(uuid: UUID, amount: Long) {
        setBalance(uuid, balance(uuid) + amount)
    }

    /** Withdraws [amount] from [uuid] if they have enough; returns false (no change) otherwise. */
    fun withdraw(uuid: UUID, amount: Long): Boolean {
        val current = balance(uuid)
        if (current < amount) return false
        setBalance(uuid, current - amount)
        return true
    }

    /** Moves [amount] from [from] to [to] if [from] has enough; returns false (no change) otherwise. */
    fun transfer(from: UUID, to: UUID, amount: Long): Boolean {
        if (!withdraw(from, amount)) return false
        deposit(to, amount)
        return true
    }

    fun top(limit: Int): List<Pair<UUID, Long>> =
        balances.entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }

    fun load() {
        balances.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.getConfigurationSection("balances")?.getKeys(false)?.forEach { key ->
            val uuid = runCatching { UUID.fromString(key) }.getOrNull() ?: return@forEach
            val amount = yaml.getLong("balances.$key")
            if (amount > 0) balances[uuid] = amount
        }
        logger.info("Loaded ${balances.size} player balance(s).")
    }

    fun save() {
        val yaml = YamlConfiguration()
        balances.forEach { (uuid, amount) -> yaml.set("balances.$uuid", amount) }
        dataFolder.mkdirs()
        yaml.save(file)
    }
}
