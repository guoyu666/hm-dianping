package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author GuoYu
 * @since 2024-6-16
 */
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

    @Override
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
        // 创建锁对象
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);

        // 获取锁（可重入），指定锁的名称
        RLock lock = redissonClient.getLock("lock:order" + userId);
        // 尝试获取锁，参数分别是：获取锁的最大等待时间（期间会重试），锁自动释放时间，时间单位
        boolean isLock = lock.tryLock();    // 这里我们使用无参数的版本！！
        if (!isLock){
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

    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 5.一人一单
        Long userId = UserHolder.getUser().getId();
        // 5.1 查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2 判断订单是否存在
        if (count > 0) {
            // 用户已经购买过了
            return Result.fail("用户已经购买过了！！");
        }

        // 6. 扣减库存
        boolean success = seckillVoucherService.update().
                setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0)    // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减库存失败
            return Result.fail("库存不足！！");
        }
        // 7. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2 用户id
        voucherOrder.setUserId(userId);
        // 7.3 代金劵id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        // 8. 返回订单id
        return Result.ok(orderId);
    }
}
