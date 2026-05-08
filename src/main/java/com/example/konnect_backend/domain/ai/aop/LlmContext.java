package com.example.konnect_backend.domain.ai.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 파이프라인 외부에서 GeminiService를 호출할 때 로깅 컨텍스트(모듈명, 버전, 입력 변수)를 설정한다.
 * AOP가 이 어노테이션을 감지해 PromptContextHolder를 세팅하고 메서드 종료 후 정리한다.
 *
 * <p>varsExpression: SpEL 표현식으로 {@code Map<String, String>}을 반환해야 한다.
 * 메서드 파라미터는 {@code #paramName} 형식으로 참조한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LlmContext {

    String moduleName();

    int promptVersion() default 0;

    /** SpEL 표현식. Map<String, String>을 반환해야 한다. 기본값: 빈 맵 */
    String varsExpression() default "{}";
}
