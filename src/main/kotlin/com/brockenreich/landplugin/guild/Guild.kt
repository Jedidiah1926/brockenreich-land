package com.brockenreich.landplugin.guild

import java.util.UUID

/**
 * A player-run organization, roughly analogous to a corporation - it can own land (see
 * Area.ownerGuild / AreaManager.isEffectiveAdmin) the way a company owns property: the leader and
 * officers manage that property on the guild's behalf, similar to how a corporation's officers
 * (not every rank-and-file employee) manage its assets. Regular members are part of the guild but
 * don't automatically gain any land-management rights from that alone.
 */
class Guild(val name: String, var leader: UUID) {
    val officers: MutableSet<UUID> = mutableSetOf()
    val members: MutableSet<UUID> = mutableSetOf()

    fun isMember(uuid: UUID): Boolean = uuid == leader || officers.contains(uuid) || members.contains(uuid)

    fun isOfficerOrAbove(uuid: UUID): Boolean = uuid == leader || officers.contains(uuid)
}
