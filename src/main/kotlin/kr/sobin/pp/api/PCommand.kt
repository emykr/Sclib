package kr.sobin.pp.api

import kr.sobin.pp.api.cmap.SimpleCommandRegistrar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command as BukkitCommand
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * 커스텀 명령 시스템의 베이스 클래스.
 * 메서드 어노테이션(@CommandArgument)와 클래스 어노테이션(@Command) 모두 지원.
 */
abstract class PCommand : CommandExecutor, TabCompleter {
    class Arguments(private val args: Array<out String>?) {
        fun get(index: Int): String? = args?.getOrNull(index)
        val size: Int get() = args?.size ?: 0
        fun asList(): List<String> = args?.toList() ?: emptyList()
    }

    fun send(sender: CommandSender, component: Component) {
        sender.sendMessage(component)
    }

    fun colored(text: String, color: NamedTextColor): Component = Component.text(text, color)

    init {
        val clazz = this::class
        val classAnno = clazz.findAnnotation<Command>()
        if (classAnno != null) {
            val cmdName = classAnno.value
            val perm = classAnno.permission
            val admin = classAnno.isAdmin
            val aliases = classAnno.alias
            require(cmdName.isNotBlank()) { "Command name must not be blank in @Command annotation on class." }
            // 메인 permission 없이도 등록 가능(서브에서만 쓸 수도 있음)
            val self = this
            val cmd = object : org.bukkit.command.Command(cmdName, "", "/$cmdName", aliases.toList()) {
                init {
                    if (perm.isNotBlank()) permission = perm
                    permissionMessage = "§cYou do not have permission."
                }
                override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
                    return self.onCommand(sender, this, label, args)
                }
                override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                    return self.onTabComplete(sender, this, alias, args).toMutableList()
                }
            }
            SimpleCommandRegistrar.queueForRegistration(cmdName, perm, admin, cmd)
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: BukkitCommand,
        label: String,
        args: Array<out String>?
    ): Boolean {
        val arguments = Arguments(args)
        val sub = arguments.get(0)?.lowercase() ?: "default"

        // 1. 메서드에 직접 @CommandArgument가 달린 것 우선 분기
        val argHandler = this::class.declaredFunctions.firstOrNull { func ->
            func.hasAnnotation<CommandArgument>() && (
                    func.findAnnotation<CommandArgument>()!!.value.lowercase() == sub ||
                            func.findAnnotation<CommandArgument>()!!.alias.any { it.lowercase() == sub }
                    )
        }
        if (argHandler != null) {
            val argAnno = argHandler.findAnnotation<CommandArgument>()!!
            // 권한/OP 체크
            if (argAnno.isAdmin && (sender !is Player || !sender.isOp)) {
                send(sender, colored("이 명령어는 관리자(오피)만 사용할 수 있습니다.", NamedTextColor.RED))
                return true
            }
            if (argAnno.permission.isNotBlank() && !sender.hasPermission(argAnno.permission)) {
                send(sender, colored("이 명령어를 사용할 권한이 없습니다.", NamedTextColor.RED))
                return true
            }
            return argHandler.call(this, sender, command, label, arguments) as? Boolean ?: false
        }

        // 2. 기존 방식(@Command(arguments = ...))도 지원
        val handler = this::class.declaredFunctions.firstOrNull { func ->
            if (!func.hasAnnotation<Command>()) return@firstOrNull false
            val anno = func.findAnnotation<Command>()!!
            // 1. value/alias
            if (anno.value.lowercase() == sub ||
                anno.alias.any { it.lowercase() == sub }) return@firstOrNull true
            // 2. CommandArgument
            anno.arguments.any { argAnno ->
                argAnno.value.lowercase() == sub ||
                        argAnno.alias.any { it.lowercase() == sub }
            }
        }
        val funcAnno = handler?.findAnnotation<Command>()
        var perm: String? = null
        var admin: Boolean? = null
        if (funcAnno != null) {
            val arg = funcAnno.arguments.firstOrNull { it.value.lowercase() == sub || it.alias.any { a -> a.lowercase() == sub } }
            if (arg != null) {
                perm = arg.permission.ifBlank { null }
                admin = if (arg.isAdmin) true else null
            } else {
                perm = funcAnno.permission.ifBlank { null }
                admin = if (funcAnno.isAdmin) true else null
            }
        }
        if (admin == true && (sender !is Player || !sender.isOp)) {
            send(sender, colored("이 명령어는 관리자(오피)만 사용할 수 있습니다.", NamedTextColor.RED))
            return true
        }
        if (!perm.isNullOrBlank() && !sender.hasPermission(perm)) {
            send(sender, colored("이 명령어를 사용할 권한이 없습니다.", NamedTextColor.RED))
            return true
        }
        if (handler != null) {
            return handler.call(this, sender, command, label, arguments) as? Boolean ?: false
        }

        // fallback: default 핸들러
        val defaultHandler = this::class.declaredFunctions.firstOrNull { func ->
            func.hasAnnotation<CommandArgument>() && (
                    func.findAnnotation<CommandArgument>()!!.value.lowercase() == "default" ||
                            func.findAnnotation<CommandArgument>()!!.alias.any { it.lowercase() == "default" }
                    )
        } ?: this::class.declaredFunctions.firstOrNull { func ->
            if (!func.hasAnnotation<Command>()) return@firstOrNull false
            val anno = func.findAnnotation<Command>()!!
            if (anno.value.lowercase() == "default" ||
                anno.alias.any { it.lowercase() == "default" }) return@firstOrNull true
            anno.arguments.any { argAnno ->
                argAnno.value.lowercase() == "default" ||
                        argAnno.alias.any { it.lowercase() == "default" }
            }
        }
        if (defaultHandler != null) {
            return defaultHandler.call(this, sender, command, label, arguments) as? Boolean ?: false
        }
        sender.sendMessage(Component.text("Unknown command.", NamedTextColor.RED))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: BukkitCommand,
        label: String,
        args: Array<out String>?
    ): List<String> {
        // @CommandArgument가 달린 메서드까지 포함해서 자동완성
        val argSubs = this::class.declaredFunctions
            .filter { it.hasAnnotation<CommandArgument>() }
            .flatMap {
                val anno = it.findAnnotation<CommandArgument>()!!
                listOf(anno.value) + anno.alias
            }
        val allSubs = this::class.declaredFunctions
            .filter { it.hasAnnotation<Command>() }
            .flatMap {
                val anno = it.findAnnotation<Command>()!!
                val subList = mutableListOf<String>()
                subList += anno.value
                subList += anno.alias
                for (argAnno in anno.arguments) {
                    subList += argAnno.value
                    subList += argAnno.alias
                }
                subList
            }
        val result = (argSubs + allSubs).filter { it != "default" }
        return if (args == null || args.size != 1) emptyList()
        else result.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}