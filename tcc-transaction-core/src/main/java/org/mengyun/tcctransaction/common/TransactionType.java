

package org.mengyun.tcctransaction.common;

/**
 * Created by changmingxie on 11/15/15.
 * 事务类型
 */
public enum TransactionType {

    ROOT(1),
    /**
     * 代表从根事务传播过来的  应该是对应事务传播级别的 NEW_TRANSACTION
     */
    BRANCH(2);

    int id;

    TransactionType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static TransactionType valueOf(int id) {
        switch (id) {
            case 1:
                return ROOT;
            case 2:
                return BRANCH;
            default:
                return null;
        }
    }

}
