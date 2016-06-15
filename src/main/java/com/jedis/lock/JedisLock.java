package com.jedis.lock;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * Jedis实现分布式锁
 * 
 * @author yuzhu.peng
 *
 */
public class JedisLock {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(JedisLock.class);
    private static final String PREFIX = "redis.lock.";

    private static final int waitInterVal = 1000; // 获取锁失败睡眠周期
    // 超时时间:默认5s
    private int waitTimeOut = 5;
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
                }

                // ttl=-1 说明key存在，但是没有过期时间。原因是：上次setNx成功后，程序crash，但是没有执行expire
                // 这种情况下可以把上次的锁占为己有
                if (jedis.ttl(lockName) == -1) {
                    jedis.expire(lockName, lockExpireTime);
                    result = true;
                }

                try {
                    Thread.sleep(waitInterVal);
                } catch (InterruptedException e) {
                    LOGGER.error("thread sleep exception", e);
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            if (jedis != null) {
                needReturn = false;
                jedisPool.returnBrokenResource(jedis);
            }
            LOGGER.error(e.getMessage(), e);
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
                jedis.watch(lockName);
                if (jedis.exists(lockName)) {
                    Transaction trans = jedis.multi();
                    jedis.del(lockName);
                    List<Object> transResult = trans.exec();
                    if (transResult == null) {
                        retryTimes--;
                        continue;
                    }
                    result = true;
                }
                jedis.unwatch();
                break;
            }
        } catch (Exception e) {
            if (jedis != null) {
                needReturn = false;
                jedisPool.returnBrokenResource(jedis);
            }
            LOGGER.error(e.getMessage(), e);
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
}
