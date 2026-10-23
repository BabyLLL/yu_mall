package com.macro.mall.portal.service.impl;

import com.macro.mall.common.exception.ApiException;
import com.macro.mall.mapper.OmsOrderItemMapper;
import com.macro.mall.mapper.OmsOrderMapper;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.mapper.PmsSkuStockMapper;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsProductExample;
import com.macro.mall.model.PmsSkuStock;
import com.macro.mall.model.PmsSkuStockExample;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.component.CancelOrderSender;
import com.macro.mall.portal.component.PickupCodeGenerator;
import com.macro.mall.portal.dao.PortalOrderDao;
import com.macro.mall.portal.dao.PortalOrderItemDao;
import com.macro.mall.portal.domain.StallOrderItemParam;
import com.macro.mall.portal.domain.StallOrderParam;
import com.macro.mall.portal.service.UmsMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StallOrderServiceImplTest {

    @Mock
    private UmsMemberService memberService;
    @Mock
    private PmsProductMapper productMapper;
    @Mock
    private PmsSkuStockMapper skuStockMapper;
    @Mock
    private OmsOrderMapper orderMapper;
    @Mock
    private PortalOrderItemDao orderItemDao;
    @Mock
    private PortalOrderDao portalOrderDao;
    @Mock
    private OmsOrderItemMapper orderItemMapper;
    @Mock
    private PickupCodeGenerator pickupCodeGenerator;
    @Mock
    private CancelOrderSender cancelOrderSender;
    @Mock
    private com.macro.mall.common.service.RedisService redisService;

    @InjectMocks
    private StallOrderServiceImpl service;

    private UmsMember member;
    private PmsProduct product;
    private PmsSkuStock sku;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "stallOrderTimeoutMinutes", 30);
        ReflectionTestUtils.setField(service, "REDIS_KEY_ORDER_ID", "oms:orderId");
        ReflectionTestUtils.setField(service, "REDIS_DATABASE", "mall");

        member = new UmsMember();
        member.setId(1L);
        member.setUsername("test");

        product = new PmsProduct();
        product.setId(100L);
        product.setName("原味烤肠");
        product.setPrice(new BigDecimal("3"));
        product.setPublishStatus(1);
        product.setDeleteStatus(0);
        product.setStock(100);
        product.setProductSn("P100");

        sku = new PmsSkuStock();
        sku.setId(1000L);
        sku.setProductId(100L);
        sku.setSkuCode("P100-1");
        sku.setStock(100);
        sku.setLockStock(0);
    }

    private StallOrderParam param(Long productId, int quantity, String note) {
        StallOrderItemParam itemParam = new StallOrderItemParam();
        itemParam.setProductId(productId);
        itemParam.setQuantity(quantity);
        StallOrderParam p = new StallOrderParam();
        p.setItems(Collections.singletonList(itemParam));
        p.setNote(note);
        return p;
    }

    private void stubProduct() {
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.singletonList(product));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.singletonList(sku));
    }

    @Test
    void createSuccessWritesPickupCodeAndSendsDelayMessage() {
        when(memberService.getCurrentMember()).thenReturn(member);
        stubProduct();
        when(redisService.incr(anyString(), anyLong())).thenReturn(1L);
        when(pickupCodeGenerator.next()).thenReturn("N001");

        Long orderId = service.create(param(100L, 2, "多辣"));

        // mybatis generator 的 insert 使用 selectKey 回填 ID，mock 环境下 ID 为 null，
        // 后续 order.getId() 传入 sendMessage 与 orderItem 回填均依赖它
        assertEquals(null, orderId);
        ArgumentCaptor<OmsOrder> orderCaptor = ArgumentCaptor.forClass(OmsOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        OmsOrder inserted = orderCaptor.getValue();
        assertEquals("N001", inserted.getPickupCode());
        assertEquals(new BigDecimal("6"), inserted.getPayAmount());
        assertEquals(0, inserted.getStatus());
        assertEquals(2, inserted.getOrderType());
        assertEquals("多辣", inserted.getNote());
        //库存锁定（先查SKU再更新）
        verify(skuStockMapper).updateByPrimaryKeySelective(any(PmsSkuStock.class));
        //延迟取消消息，30分钟（mock 下 ID 为 null，仅验证时长）
        verify(cancelOrderSender).sendMessage(null, 30 * 60 * 1000L);
    }

    @Test
    void createFailsWhenProductHidden() {
        when(memberService.getCurrentMember()).thenReturn(member);
        // mock 模拟数据库按 publish_status=1 过滤后的结果：下架商品查不到
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.emptyList());

        ApiException ex = assertThrows(ApiException.class, () -> service.create(param(100L, 1, null)));
        assertEquals("商品已下架，无法下单", ex.getMessage());
        verify(orderMapper, never()).insert(any(OmsOrder.class));
        verify(cancelOrderSender, never()).sendMessage(anyLong(), anyLong());
    }

    @Test
    void createFailsWhenStockNotEnough() {
        when(memberService.getCurrentMember()).thenReturn(member);
        when(productMapper.selectByExample(any(PmsProductExample.class)))
                .thenReturn(Collections.singletonList(product));
        when(skuStockMapper.selectByExample(any(PmsSkuStockExample.class)))
                .thenReturn(Collections.singletonList(sku));
        sku.setStock(1);
        sku.setLockStock(1); //真实库存 0

        assertThrows(ApiException.class, () -> service.create(param(100L, 2, null)));
        verify(orderMapper, never()).insert(any(OmsOrder.class));
    }

    @Test
    void createFailsOnEmptyItems() {
        StallOrderParam empty = new StallOrderParam();
        empty.setItems(Collections.emptyList());
        assertThrows(ApiException.class, () -> service.create(empty));
    }

    @Test
    void listPassesPageNumFirstAndReturnsEmptyListOnEmptyPage() {
        when(memberService.getCurrentMember()).thenReturn(member);
        //分页参数顺序回归：pageNum=1、pageSize=10 不得错位为 startPage(10, 1)
        service.list(-1, 1, 10);
        com.github.pagehelper.Page<?> localPage = com.github.pagehelper.page.PageMethod.getLocalPage();
        org.junit.jupiter.api.Assertions.assertNotNull(localPage, "startPage 未生效");
        assertEquals(1, localPage.getPageNum());
        assertEquals(10, localPage.getPageSize());
        com.github.pagehelper.page.PageMethod.clearPage();
        //空页响应恒含 list 字段（空数组而非 null）
        when(orderMapper.selectByExample(any(com.macro.mall.model.OmsOrderExample.class)))
                .thenReturn(Collections.emptyList());
        com.macro.mall.common.api.CommonPage<com.macro.mall.portal.domain.OmsOrderDetail> result =
                service.list(-1, 1, 10);
        org.junit.jupiter.api.Assertions.assertNotNull(result.getList());
        assertTrue(result.getList().isEmpty());
    }
}