package com.macro.mall.portal.service.impl;

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
import com.macro.mall.portal.domain.StallOrderConsts;
import com.macro.mall.portal.domain.StallTodayStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StallVendorServiceImplTest {

    @Mock
    private PmsProductMapper productMapper;
    @Mock
    private PmsSkuStockMapper skuStockMapper;
    @Mock
    private OmsOrderMapper orderMapper;
    @Mock
    private OmsOrderItemMapper orderItemMapper;
    @Mock
    private PortalOrderDao portalOrderDao;

    @InjectMocks
    private StallVendorServiceImpl service;

    private PmsProduct product;

    @BeforeEach
    void setUp() {
        product = new PmsProduct();
        product.setId(1L);
        product.setName("烤肠");
        product.setStock(99);
    }

    @Test
    void 商品列表返回扣除锁定库存后的真实剩余() {
        PmsSkuStock sku = new PmsSkuStock();
        sku.setStock(10);
        sku.setLockStock(3);
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.singletonList(product));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.singletonList(sku));

        List<PmsProduct> products = service.listAllProducts();

        assertEquals(1, products.size());
        assertEquals(7, products.get(0).getStock());
    }

    @Test
    void 无SKU商品用商品库存() {
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.singletonList(product));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.emptyList());

        List<PmsProduct> products = service.listAllProducts();

        assertEquals(99, products.get(0).getStock());
    }

    @Test
    void 售罄商品返回剩余0且不过滤() {
        PmsSkuStock sku = new PmsSkuStock();
        sku.setStock(5);
        sku.setLockStock(5);
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.singletonList(product));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.singletonList(sku));

        List<PmsProduct> products = service.listAllProducts();

        assertEquals(1, products.size());
        assertEquals(0, products.get(0).getStock());
    }

    private OmsOrder order(long id, Integer status, String payAmount) {
        OmsOrder order = new OmsOrder();
        order.setId(id);
        order.setStatus(status);
        order.setPayAmount(new BigDecimal(payAmount));
        return order;
    }

    @Test
    void 今日战绩按实收口径统计并按金额降序聚合明细() {
        //状态过滤由查询条件（status IN 1-4）在 SQL 层完成，单测只喂入符合口径的订单验证聚合
        OmsOrder paid = order(1L, StallOrderConsts.STATUS_PAID, "10.00");
        OmsOrder finished = order(2L, StallOrderConsts.STATUS_FINISHED, "25.50");
        when(orderMapper.selectByExample(any(OmsOrderExample.class)))
                .thenReturn(Arrays.asList(paid, finished));

        OmsOrderItem sausage1 = new OmsOrderItem();
        sausage1.setOrderId(1L);
        sausage1.setProductName("烤肠");
        sausage1.setProductQuantity(2);
        sausage1.setRealAmount(new BigDecimal("10.00"));
        OmsOrderItem sausage2 = new OmsOrderItem();
        sausage2.setOrderId(2L);
        sausage2.setProductName("烤肠");
        sausage2.setProductQuantity(3);
        sausage2.setRealAmount(new BigDecimal("15.00"));
        OmsOrderItem juice = new OmsOrderItem();
        juice.setOrderId(2L);
        juice.setProductName("果汁");
        juice.setProductQuantity(1);
        juice.setRealAmount(new BigDecimal("10.50"));
        when(orderItemMapper.selectByExample(any(OmsOrderItemExample.class)))
                .thenReturn(Arrays.asList(sausage1, sausage2, juice));

        StallTodayStats stats = service.todayStats();

        assertEquals(2, stats.getOrderCount());
        assertEquals(0, new BigDecimal("35.50").compareTo(stats.getTotalAmount()));
        assertEquals(2, stats.getItems().size());
        //金额降序：烤肠(25.00) 在 果汁(10.50) 前
        assertEquals("烤肠", stats.getItems().get(0).getProductName());
        assertEquals(5, stats.getItems().get(0).getQuantity());
        assertEquals(0, new BigDecimal("25.00").compareTo(stats.getItems().get(0).getAmount()));
        assertEquals("果汁", stats.getItems().get(1).getProductName());
        assertTrue(stats.getItems().get(1).getQuantity() == 1);
    }

    @Test
    void 今日无订单时返回空明细() {
        when(orderMapper.selectByExample(any(OmsOrderExample.class)))
                .thenReturn(Collections.emptyList());

        StallTodayStats stats = service.todayStats();

        assertEquals(0, stats.getOrderCount());
        assertEquals(0, new BigDecimal(0).compareTo(stats.getTotalAmount()));
        assertTrue(stats.getItems().isEmpty());
    }
}
