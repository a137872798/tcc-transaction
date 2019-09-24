package org.mengyun.tcctransaction.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.mengyun.tcctransaction.api.Compensable;
import org.mengyun.tcctransaction.api.Propagation;
import org.mengyun.tcctransaction.api.TransactionContext;
import org.mengyun.tcctransaction.api.UniqueIdentity;
import org.mengyun.tcctransaction.common.MethodRole;
import org.mengyun.tcctransaction.support.FactoryBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Created by changming.xie on 04/04/19.
 * 补偿方法的上下文对象
 */
public class CompensableMethodContext {

    /**
     * 切点对象
     */
    ProceedingJoinPoint pjp = null;

    /**
     * 对应的补偿方法
     */
    Method method = null;

    /**
     * 补偿元信息
     */
    Compensable compensable = null;

    /**
     * 事务传播级别
     */
    Propagation propagation = null;

    /**
     * 事务上下文对象 包含状态和 id
     */
    TransactionContext transactionContext = null;

    public CompensableMethodContext(ProceedingJoinPoint pjp) {
        this.pjp = pjp;
        this.method = getCompensableMethod();
        // 从方法上 获取 注解信息
        this.compensable = method.getAnnotation(Compensable.class);
        this.propagation = compensable.propagation();
        // 从切点的入参中 剥离出事务上下文参数
        this.transactionContext = FactoryBuilder.factoryOf(compensable.transactionContextEditor()).getInstance().get(pjp.getTarget(), method, pjp.getArgs());

    }

    public Compensable getAnnotation() {
        return compensable;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public Method getMethod() {
        return method;
    }

    /**
     * 获取唯一标识对象
     * @return
     */
    public Object getUniqueIdentity() {
        // 获取目标方法中所有参数注解
        Annotation[][] annotations = this.getMethod().getParameterAnnotations();

        for (int i = 0; i < annotations.length; i++) {
            for (Annotation annotation : annotations[i]) {
                // 返回携带 @UniqueIdentity 的参数
                if (annotation.annotationType().equals(UniqueIdentity.class)) {

                    Object[] params = pjp.getArgs();
                    Object unqiueIdentity = params[i];

                    return unqiueIdentity;
                }
            }
        }

        return null;
    }


    /**
     * 从切点中找到被 @Compensable 修饰的方法 并返回  一般来说就是切点对象 如果不是的话 就在切点对应的目标类上找到对应的方法并设置
     * @return
     */
    private Method getCompensableMethod() {
        Method method = ((MethodSignature) (pjp.getSignature())).getMethod();

        if (method.getAnnotation(Compensable.class) == null) {
            try {
                method = pjp.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
        return method;
    }

    /**
     * 根据当前是否已经存在事务 确立事务角色
     * @param isTransactionActive
     * @return
     */
    public MethodRole getMethodRole(boolean isTransactionActive) {
        // 代表总是使用一个新事务
        if ((propagation.equals(Propagation.REQUIRED) && !isTransactionActive && transactionContext == null) ||
                propagation.equals(Propagation.REQUIRES_NEW)) {
            return MethodRole.ROOT;
        // 代表沿用事务
        } else if ((propagation.equals(Propagation.REQUIRED) || propagation.equals(Propagation.MANDATORY)) && !isTransactionActive && transactionContext != null) {
            return MethodRole.PROVIDER;
        // 这里角色的分配还不是很明白
        } else {
            return MethodRole.NORMAL;
        }
    }

    /**
     * 执行切点方法
     * @return
     * @throws Throwable
     */
    public Object proceed() throws Throwable {
        return this.pjp.proceed();
    }
}