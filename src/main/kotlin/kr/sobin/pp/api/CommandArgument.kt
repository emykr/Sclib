package kr.sobin.pp.api

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommandArgument(
    val value: String,
    val alias: Array<String> = [],
    val permission: String = "",
    val isAdmin: Boolean = false
)
