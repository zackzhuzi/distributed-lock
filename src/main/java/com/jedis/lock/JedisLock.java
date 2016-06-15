package com.jedis.lock;

import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Jedis实现分布式锁
 * 
 * @author yuzhu.peng
 *
 */
public class JedisLock {
    private static final String PREFIX = "redis.lock.";

    private static final int waitInterVal = 1000; // 获取锁失败睡眠周期
    // 超时时间:默认5s
    private int waitTimeOut = 5;
    // 锁的过期时间：默认1分钟
    private int lockExpireTime = 60;
    // 锁的名字
    private String lockName;

    private JedisPool jedisPool;

    /**
     * 初始化参数
     * 
     * @param lockName
     */
    public JedisLock(String lockName) {
        this.setLockName(lockName);
    }

    /**
     * 初始化参数
     * 
     * @param lockName
     * @param waitTimeOut
     *            :seconds
     * @param lockExpireTime
     *            :seconds
     */
    public JedisLock(String lockName, int waitTimeOut, int lockExpireTime) {
        this.setLockName(lockName);
        this.waitTimeOut = waitTimeOut;
        this.lockExpireTime = lockExpireTime;
    }

    /**
     * 初始化参数
     * 
     * @param jedisPool
     * @param lockName
     * @param waitTimeOut
     *            :seconds
     * @param lockExpireTime
     *            :seconds
     */
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
    @SuppressWarnings("deprecation")
    public boolean tryLock() {
        Jedis jedis = null;
        boolean needReturn = true;
        boolean result = false;

        try {
            // 从JEDIS连接池获取Jedis
            jedis = jedisPool.getResource();
            String uuid = UUID.randomUUID().toString();
            long end = waitTimeOut * 1000 + System.currentTimeMillis();
            while (System.currentTimeMillis() < end) {
                if (jedis.setnx(lockName, uuid) > 0) {
                    jedis.expire(lockName, lockExpireTime);
                    result = true;
                    break;
                }

                // ttl=-1 说明key存在，但是没有过期时间。原因是：上次setNx成功后，程序crash，但是没有执行expire
                // 这种情况下可以把上次的锁占为己有
                if (jedis.ttl(lockName) == -1) {
                    jedis.expire(lockName, lockExpireTime);
                    result = true;
                    break;
                }

                try {
                    Thread.sleep(waitInterVal);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            if (jedis != null) {
                needReturn = false;
                jedisPool.returnBrokenResource(jedis);
            }
            e.printStackTrace();
        } finally {
            if (jedis != null && needReturn) {
                jedisPool.returnResource(jedis);
            }
        }
        return result;
    }

    /**
     * 释放锁
     * 
     * @return
     * @author yuzhu.peng
     * @since 2016年6月12日
     */
    @SuppressWarnings("deprecation")
    public boolean unLock() {
        Jedis jedis = null;
        boolean needReturn = true;
        boolean result = false;

        try {
            // 从JEDIS连接池获取Jedis
            jedis = jedisPool.getResource();
            int retryTimes = 3;
            while (retryTimes > 0) {
                // 假如某个key 正处于 WATCH 命令的监视之下，且事务块中有和这个key相关的命令，那么EXEC
                // 命令只在这个key没有被其他命令所改动的情况下执行并生效，否则该事务被打断(abort)。
                if (jedis.exists(lockName)) {
                    Long del = jedis.del(lockName);
                    if (del < 1) {
                        retryTimes--;
                        continue;
                    }
                    result = true;
                }
                break;
            }
        } catch (Exception e) {
            if (jedis != null) {
                needReturn = false;
                jedisPool.returnBrokenResource(jedis);
            }
            e.printStackTrace();
        } finally {
            if (jedis != null && needReturn) {
                jedisPool.returnResource(jedis);
            }
        }
        return result;
    }

    public void setLockName(String lockName) {
        this.lockName = PREFIX + lockName;
    }

    public String getLockName() {
        return this.lockName;
    }
}
