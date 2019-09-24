package org.mengyun.tcctransaction.recover;

import java.util.Set;

/**
 * Created by changming.xie on 6/1/16.
 * 恢复对象 管理重试 以及关闭等操作
 */
public interface RecoverConfig {

    /**
     * 最大重试次数
     * @return
     */
    public int getMaxRetryCount();

    /**
     * 恢复所用时长???
     * @return
     */
    public int getRecoverDuration();

    /**
     * 代表 定时表达式
     * @return
     */
    public String getCronExpression();

    /**
     * 延迟关闭的异常
     * @return
     */
    public Set<Class<? extends Exception>> getDelayCancelExceptions();

    /**
     * 设置延迟异常
     * @param delayRecoverExceptions
     */
    public void setDelayCancelExceptions(Set<Class<? extends Exception>> delayRecoverExceptions);

    /**
     * 获取异步线程池大小
     * @return
     */
    public int getAsyncTerminateThreadPoolSize();
}
