package club.piglin.brimstone.profiles

import club.piglin.brimstone.Brimstone
import com.mongodb.MongoException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndReplaceOptions
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.profiles.plugin.external.caffeine.cache.Cache
import me.lucko.helper.profiles.plugin.external.caffeine.cache.Caffeine
import me.lucko.helper.promise.Promise
import org.bson.Document
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ProfileHandler {
    private val profilesMap: Cache<UUID, Profile> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(6, TimeUnit.HOURS)
        .build()


    private val MINECRAFT_USERNAME_PATTERN: Pattern = Pattern.compile("^\\w{3,16}$")
    private fun isValidMcUsername(s: String): Boolean {
        return MINECRAFT_USERNAME_PATTERN.matcher(s).matches()
    }

    init {
        Events.subscribe(PlayerLoginEvent::class.java, EventPriority.MONITOR)
            .filter { it.result == PlayerLoginEvent.Result.ALLOWED }
            .handler { event ->
                Promise.start()
                    .thenApplySync {
                        lookupProfile(event.player.uniqueId)
                    }
                    .thenAcceptAsync {
                        it.get().name = event.player.name
                        it.get().lastLogin = System.currentTimeMillis()
                        saveProfile(it.get())
                        updateCache(it.get())
                        event.player.teleportAsync(it.get().getLastLocation())
                        Brimstone.log.info("[Profiles] ${event.player.name} joined, loading & saving ${event.player.name}'s profile")
                    }

            }
        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.MONITOR)
            .handler { event ->
                Promise.start()
                    .thenApplySync {
                        lookupProfile(event.player.uniqueId)
                    }
                    .thenAcceptAsync {
                        it.get().name = event.player.name
                        it.get().lastLoginLocationX = event.player.location.x
                        it.get().lastLoginLocationY = event.player.location.y
                        it.get().lastLoginLocationZ = event.player.location.z
                        saveProfile(it.get())
                        updateCache(it.get())
                        Brimstone.log.info("[Profiles] ${event.player.name} quit, saving ${event.player.name}'s profile")
                    }
            }
        Brimstone.log.info("[Profiles] Now monitoring for profile data.")
    }

    fun saveProfile(profile: Profile) {
        with (Brimstone.instance.dataSource.getDatabase("piglin").getCollection("profiles")) {
            val filter = Filters.eq("uuid", profile.uniqueId)
            val document = Document("uuid", profile.uniqueId)
                .append("name", profile.name)
                .append("lastLogin", Timestamp(profile.lastLogin))
                .append("firstJoin", Timestamp(profile.firstJoin))
                .append("lastLoginLocationX", profile.lastLoginLocationX)
                .append("lastLoginLocationY", profile.lastLoginLocationY)
                .append("lastLoginLocationZ", profile.lastLoginLocationZ)
                .append("xp", profile.xp)
                .append("level", profile.level)
                .append("gold", profile.gold)
            this.findOneAndReplace(filter, document, FindOneAndReplaceOptions().upsert(true))
        }
    }

    fun getProfile(uniqueId: UUID): Profile? {
        Objects.requireNonNull(uniqueId, "uniqueId")
        return profilesMap.getIfPresent(uniqueId)
    }

    fun lookupProfile(uniqueId: UUID): Promise<Profile> {
        Objects.requireNonNull(uniqueId, "uniqueId")
        val profile = getProfile(uniqueId)
        if (profile != null) {
            return Promise.completed(profile)
        }
        return Schedulers.async().supply {
            try {
                with (Brimstone.instance.dataSource.getDatabase("piglin").getCollection("profiles")) {
                    val filter = Filters.eq("uuid", uniqueId)
                    val document = this.find(filter).first()
                    val p: Profile
                    if (document != null) {
                        p = Profile(
                            uniqueId,
                            (document["name"] as String),
                            ((document["lastLogin"] as Date?)?.time) ?: System.currentTimeMillis(),
                            ((document["firstJoin"] as Date?)?.time) ?: System.currentTimeMillis(),
                            (document["lastLoginLocationX"] as Double?) ?: -35.5,
                            (document["lastLoginLocationY"] as Double?) ?: 34.5,
                            (document["lastLoginLocationZ"] as Double?) ?: -87.5,
                            (document["gold"] as Double?) ?: 0.0,
                            (document["xp"] as Double?) ?: 0.0,
                            (document["level"] as Int?) ?: 0
                        )
                    } else {
                        p = Profile(
                            uniqueId
                        )
                    }
                    updateCache(p)
                    return@supply p
                }
            } catch (e: MongoException) {
                e.printStackTrace()
            }
            return@supply null
        }
    }

    private fun updateCache(profile: Profile) {
        val existing: Profile? = this.profilesMap.getIfPresent(profile.uniqueId)
        if (existing == null || existing.lastLogin < profile.lastLogin) {
            this.profilesMap.put(profile.uniqueId, profile)
        }
    }
}