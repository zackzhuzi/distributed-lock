package com.jedis.lock;

import redis.clients.jedis.JedisPool;

/**
 * Jedis实现分布式锁
 * 
 * @author yuzhu.peng
 *
 */
public class JedisLock {
    private static final String PREFIX = "redis.lock.";

    // 超时时间:默认30s
    private int waitTimeOut = 30;
    // 锁的过期时间：默认1分钟
    private int lockExpireTime = 60;
    // 锁的名字
    private String lockName;

    private JedisPool jedisPool;

    public JedisLock(String lockName) {
        this.setLockName(lockName);
    }

    public JedisLock(String lockName, int waitTimeOut, int lockExpireTime) {
        this.setLockName(lockName);
        this.waitTimeOut = waitTimeOut;
        this.lockExpireTime = lockExpireTime;
    }

    public JedisLock(JedisPool jedisPool, String lockName, int waitTimeOut,
            int lockExpireTime) {
        this.jedisPool = jedisPool;
        this.setLockName(lockName);
        this.waitTimeOut = waitTimeOut;
        this.lockExpireTime = lockExpireTime;
    }

    public JedisPool getJedisPool() {
        if (jedisPool == null) {
            synchronized (this) {
                if (jedisPool == null) {
                    jedisPool = JedisPoolFactory.getJedisPool();
                }
            }
        }
        return jedisPool;
    }

    /**
     * 获取锁
     * 
     * @return
     * @author yuzhu.peng
     * @since 2016年6月12日
     */
    public boolean lock() {

        return true;
    }

    /**
     * 释放锁
     * 
     * @return
     * @author yuzhu.peng
     * @since 2016年6月12日
     */
    public boolean unLock() {

        return true;
    }

    public void setLockName(String lockName) {
        this.lockName = PREFIX + lockName;
    }
}
