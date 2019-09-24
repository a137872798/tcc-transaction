package org.mengyun.tcctransaction;

import org.mengyun.tcctransaction.api.TransactionContext;
import org.mengyun.tcctransaction.api.TransactionContextEditor;
import org.mengyun.tcctransaction.support.FactoryBuilder;
import org.mengyun.tcctransaction.utils.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by changmingxie on 10/30/15.
 * 终结对象
 */
public class Terminator implements Serializable {

    private static final long serialVersionUID = -164958655471605778L;


    public Terminator() {

    }

    /**
     * 执行 rollback 或者 commit 的实际对象
     * @param transactionContext 包含本次事务id status 等信息
     * @param invocationContext  包含应该调用的方法 和 入参
     * @param transactionContextEditorClass  具备从参数中剥离出 TransactionContext 的能力
     * @return
     */
    public Object invoke(TransactionContext transactionContext, InvocationContext invocationContext, Class<? extends TransactionContextEditor> transactionContextEditorClass) {


        if (StringUtils.isNotEmpty(invocationContext.getMethodName())) {

            try {

                // 单例工厂
                Object target = FactoryBuilder.factoryOf(invocationContext.getTargetClass()).getInstance();

                Method method = null;

                // 执行 commit or rollback
                method = target.getClass().getMethod(invocationContext.getMethodName(), invocationContext.getParameterTypes());

                // 从外部将 context 对象设置进去
                FactoryBuilder.factoryOf(transactionContextEditorClass).getInstance().set(transactionContext, target, method, invocationContext.getArgs());

                return method.invoke(target, invocationContext.getArgs());

            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
        return null;
    }
}
