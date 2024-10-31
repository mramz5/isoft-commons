package com.isoft.commons.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

@Aspect
@Component
public class LoggerAspect{
    private static final Logger logger = LoggerFactory.getLogger(LoggerAspect.class);

    @Before("execution(public * com.isoft.loans.controller.*.*(..)) || execution(public * com.isoft.cards.controller.*.*(..)) ||" +
            "execution(public * com.isoft.accounts.controller.*.*(..))")
    public void logExchangeTrace(JoinPoint joinPoint) {
        String correlationId = "correlationId", value = "";
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameters();
        for (int i = 0; i < args.length; i++) {
            if (parameters[i].getName().equals(correlationId)) {
                value = (String) args[i];
                break;
            }
        }
        logger.info("correlation-id received : {}", value);
    }
}
