package com.macro.mall.portal.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.github.pagehelper.PageHelper;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mapper.OmsOrderItemMapper;
import com.macro.mall.mapper.OmsOrderMapper;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.mapper.PmsSkuStockMapper;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderItem;
import com.macro.mall.model.OmsOrderItemExample;
import com.macro.mall.model.OmsOrderExample;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsProductExample;
import com.macro.mall.model.PmsSkuStock;
import com.macro.mall.model.PmsSkuStockExample;
import com.macro.mall.portal.dao.PortalOrderDao;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.StallOrderConsts;
import com.macro.mall.portal.domain.StallTodayStats;
import com.macro.mall.portal.service.StallVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 摊主模式Service实现
 * Created by macro on 2026/10/12.
 */
@Service
public class StallVendorServiceImpl implements StallVendorService {

    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;
    @Autowired
    private PortalOrderDao portalOrderDao;

    @Override
    public List<PmsProduct> listAllProducts() {
        PmsProductExample example = new PmsProductExample();
        //含已隐藏商品：只排除已删除
        example.createCriteria().andDeleteStatusEqualTo(0);
        example.setOrderByClause("sort desc, id desc");
        List<PmsProduct> productList = productMapper.selectByExample(example);
        //真实剩余库存（stock-lock_stock）覆写返回值：与顾客菜单接口同口径
        for (PmsProduct product : productList) {
            product.setStock(realStock(product));
        }
        return productList;
    }

    private int realStock(PmsProduct product) {
        PmsSkuStockExample skuExample = new PmsSkuStockExample();
        skuExample.createCriteria().andProductIdEqualTo(product.getId());
        List<PmsSkuStock> skuList = skuStockMapper.selectByExample(skuExample);
        if (CollUtil.isEmpty(skuList)) {
            //无SKU时按商品库存
            return product.getStock() == null ? 0 : product.getStock();
        }
        //摊位商品取第一个SKU的真实库存
        PmsSkuStock sku = skuList.get(0);
        return sku.getStock() - sku.getLockStock();
    }

    @Override
    public StallTodayStats todayStats() {
        //实收口径：今日创建且状态为已付款/制作中/待取货/已完成
        OmsOrderExample orderExample = new OmsOrderExample();
        orderExample.createCriteria()
                .andDeleteStatusEqualTo(0)
                .andOrderTypeEqualTo(StallOrderConsts.ORDER_TYPE_STALL)
                .andCreateTimeGreaterThanOrEqualTo(DateUtil.beginOfDay(new Date()))
                .andStatusIn(Arrays.asList(
                        StallOrderConsts.STATUS_PAID,
                        StallOrderConsts.STATUS_MAKING,
                        StallOrderConsts.STATUS_READY,
                        StallOrderConsts.STATUS_FINISHED));
        List<OmsOrder> orderList = orderMapper.selectByExample(orderExample);

        StallTodayStats stats = new StallTodayStats();
        stats.setOrderCount((long) orderList.size());
        BigDecimal totalAmount = new BigDecimal(0);
        for (OmsOrder order : orderList) {
            if (order.getPayAmount() != null) {
                totalAmount = totalAmount.add(order.getPayAmount());
            }
        }
        stats.setTotalAmount(totalAmount);
        if (CollUtil.isEmpty(orderList)) {
            stats.setItems(new ArrayList<>());
            return stats;
        }
        List<Long> orderIds = orderList.stream().map(OmsOrder::getId).collect(Collectors.toList());
        OmsOrderItemExample itemExample = new OmsOrderItemExample();
        itemExample.createCriteria().andOrderIdIn(orderIds);
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(itemExample);
        //按商品名分组聚合数量与金额
        Map<String, StallTodayStats.Item> itemMap = new LinkedHashMap<>();
        for (OmsOrderItem orderItem : orderItemList) {
            String name = orderItem.getProductName();
            if (name == null) {
                continue;
            }
            StallTodayStats.Item item = itemMap.computeIfAbsent(name, key -> {
                StallTodayStats.Item newItem = new StallTodayStats.Item();
                newItem.setProductName(key);
                newItem.setQuantity(0);
                newItem.setAmount(new BigDecimal(0));
                return newItem;
            });
            if (orderItem.getProductQuantity() != null) {
                item.setQuantity(item.getQuantity() + orderItem.getProductQuantity());
            }
            if (orderItem.getRealAmount() != null) {
                item.setAmount(item.getAmount().add(orderItem.getRealAmount()));
            }
        }
        List<StallTodayStats.Item> items = new ArrayList<>(itemMap.values());
        items.sort(Comparator.comparing(StallTodayStats.Item::getAmount).reversed());
        stats.setItems(items);
        return stats;
    }

    @Override
    public void updatePublishStatus(Long productId, Integer publishStatus) {
        PmsProduct product = productMapper.selectByPrimaryKey(productId);
        if (product == null || Integer.valueOf(1).equals(product.getDeleteStatus())) {
            Asserts.fail("商品不存在");
        }
        PmsProduct update = new PmsProduct();
        update.setId(productId);
        update.setPublishStatus(publishStatus);
        productMapper.updateByPrimaryKeySelective(update);
    }

    @Override
    public CommonPage<OmsOrderDetail> listOrders(Integer status, Integer pageNum, Integer pageSize) {
        if (status == null || status == -1) {
            status = null;
        }
        PageHelper.startPage(pageNum, pageSize);
        OmsOrderExample orderExample = new OmsOrderExample();
        OmsOrderExample.Criteria criteria = orderExample.createCriteria();
        criteria.andDeleteStatusEqualTo(0)
                .andOrderTypeEqualTo(StallOrderConsts.ORDER_TYPE_STALL);
        if (status != null) {
            criteria.andStatusEqualTo(status);
        }
        orderExample.setOrderByClause("create_time desc");
        List<OmsOrder> orderList = orderMapper.selectByExample(orderExample);
        CommonPage<OmsOrder> orderPage = CommonPage.restPage(orderList);
        CommonPage<OmsOrderDetail> resultPage = new CommonPage<>();
        resultPage.setPageNum(orderPage.getPageNum());
        resultPage.setPageSize(orderPage.getPageSize());
        resultPage.setTotal(orderPage.getTotal());
        resultPage.setTotalPage(orderPage.getTotalPage());
        if (CollUtil.isEmpty(orderList)) {
            resultPage.setList(new ArrayList<>());
            return resultPage;
        }
        List<Long> orderIds = orderList.stream().map(OmsOrder::getId).collect(Collectors.toList());
        OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
        orderItemExample.createCriteria().andOrderIdIn(orderIds);
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
        List<OmsOrderDetail> orderDetailList = new ArrayList<>();
        for (OmsOrder omsOrder : orderList) {
            OmsOrderDetail orderDetail = new OmsOrderDetail();
            BeanUtil.copyProperties(omsOrder, orderDetail);
            List<OmsOrderItem> relatedItemList = orderItemList.stream()
                    .filter(item -> item.getOrderId().equals(orderDetail.getId()))
                    .collect(Collectors.toList());
            orderDetail.setOrderItemList(relatedItemList);
            orderDetailList.add(orderDetail);
        }
        resultPage.setList(orderDetailList);
        return resultPage;
    }

    @Override
    @Transactional
    public void transition(Long orderId, Integer targetStatus) {
        OmsOrder order = getStallOrder(orderId);
        Set<Integer> allowed = StallOrderConsts.VENDOR_TRANSITIONS.get(order.getStatus());
        if (allowed == null || !allowed.contains(targetStatus)) {
            Asserts.fail("订单当前状态不允许此操作");
        }
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        update.setStatus(targetStatus);
        //收款时间记录在 paymentTime
        if (targetStatus == StallOrderConsts.STATUS_PAID) {
            update.setPaymentTime(new Date());
        }
        //出餐完成（待取货）时间记录在 deliveryTime
        if (targetStatus == StallOrderConsts.STATUS_READY) {
            update.setDeliveryTime(new Date());
        }
        //完成时间记录在 receiveTime
        if (targetStatus == StallOrderConsts.STATUS_FINISHED) {
            update.setReceiveTime(new Date());
        }
        orderMapper.updateByPrimaryKeySelective(update);
    }

    @Override
    @Transactional
    public void cancelByVendor(Long orderId) {
        OmsOrder order = getStallOrder(orderId);
        if (order.getStatus() == StallOrderConsts.STATUS_FINISHED
                || order.getStatus() == StallOrderConsts.STATUS_CANCELED) {
            Asserts.fail("订单已完成或已取消");
        }
        releaseStockAndCancel(order);
    }

    private OmsOrder getStallOrder(Long orderId) {
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null
                || order.getOrderType() == null
                || order.getOrderType() != StallOrderConsts.ORDER_TYPE_STALL
                || Integer.valueOf(1).equals(order.getDeleteStatus())) {
            Asserts.fail("摊位订单不存在");
        }
        return order;
    }

    private void releaseStockAndCancel(OmsOrder order) {
        OmsOrder update = new OmsOrder();
        update.setId(order.getId());
        update.setStatus(StallOrderConsts.STATUS_CANCELED);
        orderMapper.updateByPrimaryKeySelective(update);
        OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
        orderItemExample.createCriteria().andOrderIdEqualTo(order.getId());
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
        if (!CollUtil.isEmpty(orderItemList)) {
            portalOrderDao.releaseSkuStockLock(orderItemList);
        }
    }
}
