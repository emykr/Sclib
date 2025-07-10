package kr.sobin.pp.api

/**
 * 이 어노테이션이 붙은 함수/클래스는 리플렉션 기반 명령어 시스템에서 런타임에 사용됨을 명시합니다.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Suppress("unused")
annotation class UsedByCommandSystem