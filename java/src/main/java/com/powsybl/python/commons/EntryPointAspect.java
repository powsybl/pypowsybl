package com.powsybl.python.commons;

import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.word.WordFactory;

import static com.powsybl.python.commons.Util.setException;

@Aspect
public class EntryPointAspect {

    @Pointcut("@annotation(entryPointAnnotation)")
    public void annotatedWithEntryPoint(CEntryPoint entryPointAnnotation) {
    }

    @Around("annotatedWithEntryPoint(entryPointAnnotation)")
    public Object aroundAnnotatedMethod(ProceedingJoinPoint joinPoint, CEntryPoint entryPointAnnotation) {
        var args = joinPoint.getArgs();
        ExceptionHandlerPointer exceptionHandlerPtr = (ExceptionHandlerPointer) args[args.length - 1];
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
        }
        return null;
    }
}
