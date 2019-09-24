package org.mengyun.tcctransaction.api;

/**
 * Created by changming.xie on 1/18/17.
 * 事务传播级别
 */
public enum Propagation {
    /**
     * 如果已经存在一个事务 沿用该事务， 否则新建一个事务
     */
    REQUIRED(0),
    /**
     * 如果存在一个事务 沿用该事务， 否则不使用事务
     */
    SUPPORTS(1),
    /**
     * 如果已经存在一个事务 沿用该事务， 否则抛出异常
     */
    MANDATORY(2),
    /**
     * 总是开启新事务， 如果当前已经有事务存在 挂起 该事务
     */
    REQUIRES_NEW(3);

    private final int value;

    private Propagation(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
