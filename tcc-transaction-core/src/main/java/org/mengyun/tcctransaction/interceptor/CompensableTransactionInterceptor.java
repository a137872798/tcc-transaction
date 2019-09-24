package org.mengyun.tcctransaction.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.mengyun.tcctransaction.NoExistedTransactionException;
import org.mengyun.tcctransaction.SystemException;
import org.mengyun.tcctransaction.Transaction;
import org.mengyun.tcctransaction.TransactionManager;
import org.mengyun.tcctransaction.api.TransactionStatus;
import org.mengyun.tcctransaction.utils.ReflectionUtils;
import org.mengyun.tcctransaction.utils.TransactionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by changmingxie on 10/30/15.
 * 拦截器对象
 */
public class CompensableTransactionInterceptor {

    static final Logger logger = Logger.getLogger(CompensableTransactionInterceptor.class.getSimpleName());

    /**
     * 事务管理器  相当于 事务相关的类对外的入口 比如 内部维护了 Participant
     */
    private TransactionManager transactionManager;

    private Set<Class<? extends Exception>> delayCancelExceptions = new HashSet<Class<? extends Exception>>();

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setDelayCancelExceptions(Set<Class<? extends Exception>> delayCancelExceptions) {
        this.delayCancelExceptions.addAll(delayCancelExceptions);
    }

    /**
     * 拦截 被@Compensable 修饰的 方法
     * @param pjp
     * @return
     * @throws Throwable
     */
    public Object interceptCompensableMethod(ProceedingJoinPoint pjp) throws Throwable {

        // 生成方法上下文信息
        CompensableMethodContext compensableMethodContext = new CompensableMethodContext(pjp);

        // 判断当前是否处在一个事务中
        boolean isTransactionActive = transactionManager.isTransactionActive();

        if (!TransactionUtils.isLegalTransactionContext(isTransactionActive, compensableMethodContext)) {
            throw new SystemException("no active compensable transaction while propagation is mandatory for method " + compensableMethodContext.getMethod().getName());
        }

        // 根据 不同角色 走不同
        switch (compensableMethodContext.getMethodRole(isTransactionActive)) {
            // 代表开启一个新事务
            case ROOT:
                return rootMethodProceed(compensableMethodContext);
            // 沿用之前的事务
            case PROVIDER:
                return providerMethodProceed(compensableMethodContext);
            default:
                return pjp.proceed();
        }
    }

    /**
     * 针对 根事务的增强
     * @param compensableMethodContext
     * @return
     * @throws Throwable
     */
    private Object rootMethodProceed(CompensableMethodContext compensableMethodContext) throws Throwable {

        Object returnValue = null;

        Transaction transaction = null;

        boolean asyncConfirm = compensableMethodContext.getAnnotation().asyncConfirm();

        boolean asyncCancel = compensableMethodContext.getAnnotation().asyncCancel();

        Set<Class<? extends Exception>> allDelayCancelExceptions = new HashSet<Class<? extends Exception>>();
        // 设置延迟关闭的所有异常类
        allDelayCancelExceptions.addAll(this.delayCancelExceptions);
        // 追加注解中包含的
        allDelayCancelExceptions.addAll(Arrays.asList(compensableMethodContext.getAnnotation().delayCancelExceptions()));

        try {

            // 开启事务 (在本线程绑定事务信息)
            transaction = transactionManager.begin(compensableMethodContext.getUniqueIdentity());

            try {
                // 执行切点方法  应该是在方法内部 通过将某些 参数者 注册到该事务对象中 实现统一管理 而某个server 一旦出现 try失败的情况 下就抛出异常
                returnValue = compensableMethodContext.proceed();
            } catch (Throwable tryingException) {

                if (!isDelayCancelException(tryingException, allDelayCancelExceptions)) {

                    logger.warn(String.format("compensable transaction trying failed. transaction content:%s", JSON.toJSONString(transaction)), tryingException);

                    transactionManager.rollback(asyncCancel);
                }

                throw tryingException;
            }

            transactionManager.commit(asyncConfirm);

        } finally {
            transactionManager.cleanAfterCompletion(transaction);
        }

        return returnValue;
    }

    /**
     * 代表沿用之前的事务  看来参与者对应的查询方法也是使用 @Compensable 注解来包裹
     * @param compensableMethodContext
     * @return
     * @throws Throwable
     */
    private Object providerMethodProceed(CompensableMethodContext compensableMethodContext) throws Throwable {

        Transaction transaction = null;


        boolean asyncConfirm = compensableMethodContext.getAnnotation().asyncConfirm();

        boolean asyncCancel = compensableMethodContext.getAnnotation().asyncCancel();

        try {

            switch (TransactionStatus.valueOf(compensableMethodContext.getTransactionContext().getStatus())) {
                // 处在 验证状态 创建一个 branche 对象 并执行  在方法内部应该会将 status 修改成 Confirm
                case TRYING:
                    transaction = transactionManager.propagationNewBegin(compensableMethodContext.getTransactionContext());
                    return compensableMethodContext.proceed();
                // 如果确认允许提交 将当前事务更新成Confirm后设置
                case CONFIRMING:
                    try {
                        transaction = transactionManager.propagationExistBegin(compensableMethodContext.getTransactionContext());
                        // 提交事务
                        transactionManager.commit(asyncConfirm);
                    } catch (NoExistedTransactionException excepton) {
                        //the transaction has been commit,ignore it.
                    }
                    break;
                // 同上
                case CANCELLING:

                    try {
                        transaction = transactionManager.propagationExistBegin(compensableMethodContext.getTransactionContext());
                        transactionManager.rollback(asyncCancel);
                    } catch (NoExistedTransactionException exception) {
                        //the transaction has been rollback,ignore it.
                    }
                    break;
            }

        } finally {
            transactionManager.cleanAfterCompletion(transaction);
        }

        Method method = compensableMethodContext.getMethod();

        return ReflectionUtils.getNullValue(method.getReturnType());
    }

    private boolean isDelayCancelException(Throwable throwable, Set<Class<? extends Exception>> delayCancelExceptions) {

        if (delayCancelExceptions != null) {
            for (Class delayCancelException : delayCancelExceptions) {

                Throwable rootCause = ExceptionUtils.getRootCause(throwable);

                if (delayCancelException.isAssignableFrom(throwable.getClass())
                        || (rootCause != null && delayCancelException.isAssignableFrom(rootCause.getClass()))) {
                    return true;
                }
            }
        }

        return false;
    }

}
