package com.macro.mall.portal.service.impl;

import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.mapper.PmsSkuStockMapper;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsProductExample;
import com.macro.mall.model.PmsSkuStock;
import com.macro.mall.model.PmsSkuStockExample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StallMenuServiceImplTest {

    @Mock
    private PmsProductMapper productMapper;
    @Mock
    private PmsSkuStockMapper skuStockMapper;

    @InjectMocks
    private StallMenuServiceImpl service;

    private PmsProduct skuProduct;
    private PmsProduct noSkuProduct;

    @BeforeEach
    void setUp() {
        skuProduct = new PmsProduct();
        skuProduct.setId(1L);
        skuProduct.setName("烤肠");
        skuProduct.setStock(99);

        noSkuProduct = new PmsProduct();
        noSkuProduct.setId(2L);
        noSkuProduct.setName("关东煮");
        noSkuProduct.setStock(5);
    }

    @Test
    void 有SKU时返回扣除锁定库存后的真实剩余() {
        PmsSkuStock sku = new PmsSkuStock();
        sku.setStock(10);
        sku.setLockStock(3);
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.singletonList(skuProduct));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.singletonList(sku));

        List<PmsProduct> menu = service.listMenu();

        assertEquals(1, menu.size());
        assertEquals(7, menu.get(0).getStock());
    }

    @Test
    void 无SKU时使用商品库存() {
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.singletonList(noSkuProduct));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.emptyList());

        List<PmsProduct> menu = service.listMenu();

        assertEquals(5, menu.get(0).getStock());
    }

    @Test
    void 售罄商品不过滤且剩余为0() {
        PmsSkuStock sku = new PmsSkuStock();
        sku.setStock(5);
        sku.setLockStock(5);
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Arrays.asList(skuProduct, noSkuProduct));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.singletonList(sku))
                .thenReturn(Collections.emptyList());

        List<PmsProduct> menu = service.listMenu();

        assertEquals(2, menu.size());
        assertEquals(0, menu.get(0).getStock());
        assertEquals(5, menu.get(1).getStock());
    }
}
