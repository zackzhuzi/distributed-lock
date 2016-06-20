package com.jedis.lock;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.JedisPool;

/**
 * JedisLock测试用例
 * 
 * @author yuzhupeng
 *
 */
public class JedisLockTest {
    private JedisPool jedisPool = null;
    private static final String lockName = "dragon";
    private static final int THREADCOUNT = 20;

    private int count = 20;

    @Before
    public void init() {
        jedisPool = JedisPoolFactory.getJedisPool();
        System.out.println("init count=" + count);
    }

    @Test
    public void jedisLockTest() {
        TestRunnable[] testRunnables = new TestRunnable[THREADCOUNT];
        for (int i = 0; i < THREADCOUNT; i++) {
            testRunnables[i] = new JedisLockThread();
        }

        // 多线程测试
        MultiThreadedTestRunner multiThreadedTestRunner = new MultiThreadedTestRunner(
                testRunnables);
        try {
            multiThreadedTestRunner.runTestRunnables();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    class JedisLockThread extends TestRunnable {
        @Override
        public void runTest() {
            JedisLock jedisLock = new JedisLock(jedisPool, lockName, 5, 86400);
            boolean tryLock = false;
            try {
                tryLock = jedisLock.tryLock();
                if (tryLock) {
                    System.out.println(Thread.currentThread().getName()
                            + "|lock success|" + lockName);
                    count--;
                } else {
                    System.out.println(Thread.currentThread().getName()
                            + "|lock failed|" + lockName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (tryLock) {
                    if (jedisLock.unLock()) {
                        System.out.println(Thread.currentThread().getName()
                                + "|unlock success|" + lockName);
                    } else {
                        System.out.println(Thread.currentThread().getName()
                                + "|unlock failed|" + lockName);
                    }
                }
            }
        }
    }

    @After
    public void printCount() {
        System.out.println("final count=" + count);
    }
}
