package org.mengyun.tcctransaction.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * Created by changmingxie on 10/25/15.
 * 可补偿注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Compensable {

    /**
     * 事务的传播级别
     * @return
     */
    public Propagation propagation() default Propagation.REQUIRED;

    /**
     * 指定用于判定 confirm 的方法名
     * @return
     */
    public String confirmMethod() default "";

    /**
     * 指定用于判断 cancel 的方法名
     * @return
     */
    public String cancelMethod() default "";

    /**
     * 使用的事务编辑对象
     * @return
     */
    public Class<? extends TransactionContextEditor> transactionContextEditor() default DefaultTransactionContextEditor.class;

    /**
     * 一组延时异常
     * @return
     */
    public Class<? extends Exception>[] delayCancelExceptions() default {};

    public boolean asyncConfirm() default false;

    public boolean asyncCancel() default false;

    /**
     * 空实现
     */
    class NullableTransactionContextEditor implements TransactionContextEditor {

        @Override
        public TransactionContext get(Object target, Method method, Object[] args) {
            return null;
        }

        @Override
        public void set(TransactionContext transactionContext, Object target, Method method, Object[] args) {

        }
    }

    /**
     * 默认的事务编辑器
     */
    class DefaultTransactionContextEditor implements TransactionContextEditor {

        @Override
        public TransactionContext get(Object target, Method method, Object[] args) {
            // 好像是从方法中找到 TransactionContext 参数的位置 那么不是强耦合了吗 跟业务代码
            int position = getTransactionContextParamPosition(method.getParameterTypes());

            if (position >= 0) {
                return (TransactionContext) args[position];
            }

            return null;
        }

        /**
         * 将事务上下文 对象设置到参数中对应的位置
         * @param transactionContext
         * @param target
         * @param method
         * @param args
         */
        @Override
        public void set(TransactionContext transactionContext, Object target, Method method, Object[] args) {

            int position = getTransactionContextParamPosition(method.getParameterTypes());
            if (position >= 0) {
                args[position] = transactionContext;
            }
        }

        /**
         * 从参数列表中 定位 TransactionContext 参数的下标
         */
        public static int getTransactionContextParamPosition(Class<?>[] parameterTypes) {

            int position = -1;

            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i].equals(org.mengyun.tcctransaction.api.TransactionContext.class)) {
                    position = i;
                    break;
                }
            }
            return position;
        }

        /**
         * 获取 TransactionContext 的参数值
         * @param args
         * @return
         */
        public static TransactionContext getTransactionContextFromArgs(Object[] args) {

            TransactionContext transactionContext = null;

            for (Object arg : args) {
                if (arg != null && org.mengyun.tcctransaction.api.TransactionContext.class.isAssignableFrom(arg.getClass())) {

                    transactionContext = (org.mengyun.tcctransaction.api.TransactionContext) arg;
                }
            }

            return transactionContext;
        }
    }
}