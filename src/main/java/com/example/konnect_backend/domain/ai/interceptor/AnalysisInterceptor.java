package com.example.konnect_backend.domain.ai.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 각 문서 분석 요청을 식별할 request_id를 지정합니다. </br>
 * 식별자는 요청 내 LLM API 호출을 하나로 묶어줍니다. </br>
 * MDC는 스레드 단위에서 격리됩니다.
 */
public class AnalysisInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_KEY = "requestId";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        MDC.remove(REQUEST_ID_KEY);
    }
}