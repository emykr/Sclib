package kr.sobin.pp.api.cmap

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.plugin.Plugin
import java.lang.reflect.Field

/**
 * SimpleCommandRegistrar: Annotation-based automatic command registration for Bukkit plugins.
 * Supports registering both main commands and their aliases (including non-English/Unicode).
 * Ensures only unique commands are registered and prevents duplicate registration.
 * Uses a data class for pending commands, providing type safety and property access.
 * Provides clear separation of concerns and safe null handling.
 */
object SimpleCommandRegistrar {
    // Cache for CommandMap instance
    private var commandMap: CommandMap? = null

    // Set to keep track of already-registered command names (prevents duplicates including aliases)
    private val registeredCommands = mutableSetOf<String>()

    // Used to detect reload situations
    private var lastRegisteredCount = 0
    private var alreadyWarnedReload = false

    /**
     * Uses reflection to get the Bukkit CommandMap instance.
     * @return CommandMap instance for dynamic command registration
     * @throws IllegalStateException if CommandMap could not be found or is wrong type
     */
    private fun getCommandMap(): CommandMap {
        if (commandMap != null) return commandMap!!
        val server = Bukkit.getServer()
        val field: Field = server.javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        val map = field.get(server)
        require(map is CommandMap) { "commandMap field is not a CommandMap" }
        commandMap = map
        return commandMap!!
    }

    /**
     * Registers all queued commands to the Bukkit CommandMap.
     * Should only be called ONCE per plugin lifecycle (e.g., in onEnable).
     * @param plugin The plugin instance (for logging and namespace)
     */
    fun registerAll(plugin: Plugin) {
        if (lastRegisteredCount > 0 && !alreadyWarnedReload) {
            plugin.logger.warning("[SimpleCommandRegistrar] Detected plugin reload! Custom command registrations should be managed carefully to avoid duplicates.")
            alreadyWarnedReload = true
        }
        val cmap = getCommandMap()
        var count = 0
        val logList = StringBuilder()
        for (pcmd in pendingCommands) {
            // Register main command name if not already registered
            if (!registeredCommands.contains(pcmd.name)) {
                if (cmap.getCommand(pcmd.name) == null) {
                    cmap.register(plugin.name.lowercase(), pcmd.cmd)
                    count++
                    registeredCommands.add(pcmd.name)
                    logList.append("- ${pcmd.name} (permission: ${pcmd.permission}${if (pcmd.admin) ", admin" else ""})\n")
                }
            }
            // Register aliases individually, as Bukkit commandMap does not always do this for dynamically created commands
            for (alias in pcmd.cmd.aliases) {
                if (!registeredCommands.contains(alias)) {
                    if (cmap.getCommand(alias) == null) {
                        // usageMessage is protected, so pass "" in constructor and do not attempt to copy after
                        cmap.register(plugin.name.lowercase(), AliasCommandWrapper(alias, pcmd.cmd))
                        count++
                        registeredCommands.add(alias)
                        logList.append("- $alias (alias for ${pcmd.name})\n")
                    }
                }
            }
        }
        lastRegisteredCount += count
        if (count > 0) {
            plugin.logger.info("$count custom command(s) registered:\n$logList")
        }
        pendingCommands.clear()
    }

    /**
     * Data class to hold pending command registration info.
     * @property name The command name.
     * @property permission The permission node required for this command.
     * @property admin Whether this command is admin-only.
     * @property cmd The actual Bukkit Command instance.
     */
    private data class PendingCmd(
        val name: String,
        val permission: String,
        val admin: Boolean,
        val cmd: Command
    )

    // List of commands to be registered on registerAll
    private val pendingCommands = mutableListOf<PendingCmd>()

    /**
     * Queues a command for registration. This does not immediately register the command.
     * @param name Command name.
     * @param permission Required permission node.
     * @param isAdmin Whether the command is admin-only.
     * @param command The Bukkit Command instance.
     */
    fun queueForRegistration(name: String, permission: String, isAdmin: Boolean, command: Command) {
        pendingCommands.add(PendingCmd(name, permission, isAdmin, command))
    }

    /**
     * Wrapper for command aliases: Makes an alias name invoke the same logic as the main command.
     * This is necessary because Bukkit CommandMap's register(name, command) does not always make aliases accessible
     * when commands are registered dynamically at runtime.
     */
    private class AliasCommandWrapper(
        aliasName: String,
        private val original: Command
    ) : Command(aliasName, original.description, "", emptyList()) {
        init {
            this.permission = original.permission
            // Deprecated: permissionMessage is deprecated but included for legacy support if needed.
            @Suppress("DEPRECATION")
            this.permissionMessage = original.permissionMessage
            // usageMessage is intentionally NOT copied due to protected access.
        }
        override fun execute(sender: org.bukkit.command.CommandSender, label: String, args: Array<out String>): Boolean {
            // Call original command's logic
            return original.execute(sender, label, args)
        }

        override fun tabComplete(sender: org.bukkit.command.CommandSender, alias: String, args: Array<out String>): MutableList<String> {
            return original.tabComplete(sender, alias, args).toMutableList()
        }
    }
}