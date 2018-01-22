package com.heller.jmockit.mall.service;

//订单服务类 ,用于下订单
public interface OrderService {
    
    /**
     * 下订单
     *
     * @param buyerId 买家ID
     * @param itemId  商品id
     * @return 返回 下订单是否成功
     */
    boolean submitOrder(long buyerId, long itemId);
    
}
