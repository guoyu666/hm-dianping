package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author GuoYu
 * @since 2024-6-16
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 创建一个阻塞队列
    private static final BlockingDeque<VoucherOrder> orderTasks = new LinkedBlockingDeque<>(1024 * 1024);

    // 创建一个线程池
    private final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 秒杀业务需要在类初始化之后就立即执行，所以这里需要用到@PostConstruct注解
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    // 创建线程任务
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 获取队列中的信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 创建订单
                    handlerVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("订单处理异常！！", e);
                }
            }
        }
    }

    private IVoucherOrderService proxy;

    private void handlerVoucherOrder(VoucherOrder voucherOrder) {
        // 1. 获取用户
        Long userId = voucherOrder.getUserId();
        // 2. 创建锁对象，作为兜底方案
        RLock redisLock = redissonClient.getLock("lock:order" + userId);
        // 3. 获取锁
        boolean isLock = redisLock.tryLock();
        // 4. 判断是否获取锁成功
        if (!isLock) {
            log.error("不允许重复下单！");
            return;
        }
        try {
            // 5. 使用代理对象，由于这是另外一个线程
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            redisLock.unlock();
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 1. 执行lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
        // 2. 判断返回值，如果不为0，则表示有错误，代表没有购买资格，并返回错误信息
        if (result != null && result.intValue() != 0) {
            return Result.fail(result.intValue() == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.1 返回0，运行到这表示秒杀成功，有购买资格，把订单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.2 订单ID
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.3 用户ID
        voucherOrder.setUserId(userId);
        // 2.4 代金劵ID
        voucherOrder.setVoucherId(voucherId);
        // 2.5 保存到阻塞队列
        orderTasks.add(voucherOrder);

        // 主线程获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // 返回订单Id
        return Result.ok(orderId);

    }

    /*@Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠劵(秒杀卷与优惠卷共享一个ID！)
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2. 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始");
        }
        // 3. 判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 已经结束
            return Result.fail("秒杀已经结束");
        }
        // 4. 判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        // 创建锁对象，这里我们使用redisson来实现分布式锁
        RLock lock = redissonClient.getLock("lock:order" + userId);
        // 尝试获取锁，参数分别是：获取锁的最大等待时间（期间会重试），锁自动释放时间，时间单位
        boolean isLock = lock.tryLock();    // 这里我们使用无参数的版本！！
        if (!isLock) {
            // 获取锁失败，返回错误或重试
            return Result.fail("不允许重复下单！！");
        }
        try {
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }

    }*/

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5.一人一单
        Long userId = UserHolder.getUser().getId();
        Long voucherId = voucherOrder.getVoucherId();
        // 5.1 查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2 判断订单是否存在
        if (count > 0) {
            // 用户已经购买过了
            log.error("已经抢过优惠劵了");
            return;
        }

        // 6. 扣减库存
        boolean success = seckillVoucherService.update().
                setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0)    // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减库存失败
            log.error("库存不足！");
        }
        save(voucherOrder);
    }
}
