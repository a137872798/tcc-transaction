package org.mengyun.tcctransaction.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Created by changmingxie on 10/30/15.
 * 动态代理切点
 */
@Aspect
public abstract class CompensableTransactionAspect {

    private CompensableTransactionInterceptor compensableTransactionInterceptor;

    public void setCompensableTransactionInterceptor(CompensableTransactionInterceptor compensableTransactionInterceptor) {
        this.compensableTransactionInterceptor = compensableTransactionInterceptor;
    }

    /**
     * 定义切点为 携带 @Compensable注解的方法
     */
    @Pointcut("@annotation(org.mengyun.tcctransaction.api.Compensable)")
    public void compensableService() {

    }

    /**
     * 在切点方法前后调用拦截逻辑
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("compensableService()")
    public Object interceptCompensableMethod(ProceedingJoinPoint pjp) throws Throwable {

        return compensableTransactionInterceptor.interceptCompensableMethod(pjp);
    }

    public abstract int getOrder();
}
