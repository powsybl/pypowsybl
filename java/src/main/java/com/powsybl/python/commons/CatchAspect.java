package com.powsybl.python.commons;

import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class CatchAspect {

    @Pointcut("@annotation(catchAnnotation)")
    public void annotatedWithCatch(Catch catchAnnotation) {
    }

    @Around("annotatedWithCatch(catchAnnotation)")
    public Object aroundAnnotatedMethod(ProceedingJoinPoint joinPoint, Catch catchAnnotation) {
        var args = joinPoint.getArgs();
        ExceptionHandlerPointer exceptionHandlerPtr = (ExceptionHandlerPointer) args[args.length - 1];
        Util.doCatch(exceptionHandlerPtr, () -> joinPoint.proceed());
        return null;
    }
}
