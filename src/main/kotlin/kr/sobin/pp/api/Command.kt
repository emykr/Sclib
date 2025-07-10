package kr.sobin.pp.api

/**
 * 명령어 및 서브명령어를 정의하는 어노테이션.
 */
@UsedByCommandSystem
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val value: String,
    val permission: String = "",
    val isAdmin: Boolean = false,
    val alias: Array<String> = [],
    val arguments: Array<CommandArgument> = []
)