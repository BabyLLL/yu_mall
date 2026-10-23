package com.macro.mall.portal.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.PageHelper;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.common.service.RedisService;
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
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.component.CancelOrderSender;
import com.macro.mall.portal.component.PickupCodeGenerator;
import com.macro.mall.portal.dao.PortalOrderDao;
import com.macro.mall.portal.dao.PortalOrderItemDao;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.StallOrderConsts;
import com.macro.mall.portal.domain.StallOrderItemParam;
import com.macro.mall.portal.domain.StallOrderParam;
import com.macro.mall.portal.service.StallOrderService;
import com.macro.mall.portal.service.UmsMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 摊位订单管理Service实现
 * 下单不依赖购物车/地址/优惠券/积分，按商品现价直接计算金额。
 * Created by macro on 2026/10/12.
 */
@Service
public class StallOrderServiceImpl implements StallOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StallOrderServiceImpl.class);

    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private PortalOrderItemDao orderItemDao;
    @Autowired
    private PortalOrderDao portalOrderDao;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;
    @Autowired
    private PickupCodeGenerator pickupCodeGenerator;
    @Autowired
    private CancelOrderSender cancelOrderSender;
    @Autowired
    private RedisService redisService;
    @Value("${redis.key.orderId}")
    private String REDIS_KEY_ORDER_ID;
    @Value("${redis.database}")
    private String REDIS_DATABASE;
    /**
     * 摊位订单未付款超时时间（分钟），默认30分钟
     */
    @Value("${stall.order.timeout-minutes:30}")
    private Integer stallOrderTimeoutMinutes;

    @Override
    @Transactional
    public Long create(StallOrderParam stallOrderParam) {
        if (stallOrderParam == null || CollUtil.isEmpty(stallOrderParam.getItems())) {
            Asserts.fail("请先选择商品");
        }
        UmsMember currentMember = memberService.getCurrentMember();
        //校验商品并生成订单商品明细
        List<OmsOrderItem> orderItemList = new ArrayList<>();
        List<PmsSkuStock> lockedSkuList = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal(0);
        for (StallOrderItemParam itemParam : stallOrderParam.getItems()) {
            if (itemParam.getProductId() == null || itemParam.getQuantity() == null || itemParam.getQuantity() <= 0) {
                Asserts.fail("下单商品参数不合法");
            }
            PmsProductExample productExample = new PmsProductExample();
            productExample.createCriteria().andIdEqualTo(itemParam.getProductId())
                    .andPublishStatusEqualTo(1)
                    .andDeleteStatusEqualTo(0);
            List<PmsProduct> productList = productMapper.selectByExample(productExample);
            if (CollUtil.isEmpty(productList)) {
                Asserts.fail("商品已下架，无法下单");
            }
            PmsProduct product = productList.get(0);
            //库存校验：取默认第一个SKU（摊位商品不区分规格）
            PmsSkuStockExample skuExample = new PmsSkuStockExample();
            skuExample.createCriteria().andProductIdEqualTo(product.getId());
            skuExample.setOrderByClause("id asc");
            List<PmsSkuStock> skuList = skuStockMapper.selectByExample(skuExample);
            PmsSkuStock sku = CollUtil.isNotEmpty(skuList) ? skuList.get(0) : null;
            int realStock = sku == null ? product.getStock() : sku.getStock() - sku.getLockStock();
            if (realStock < itemParam.getQuantity()) {
                Asserts.fail("库存不足，无法下单");
            }
            OmsOrderItem orderItem = new OmsOrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductPic(product.getPic());
            orderItem.setProductBrand(product.getBrandName());
            orderItem.setProductSn(product.getProductSn());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setProductQuantity(itemParam.getQuantity());
            orderItem.setProductCategoryId(product.getProductCategoryId());
            if (sku != null) {
                orderItem.setProductSkuId(sku.getId());
                orderItem.setProductSkuCode(sku.getSkuCode());
                lockedSkuList.add(sku);
            }
            orderItem.setPromotionAmount(new BigDecimal(0));
            orderItem.setCouponAmount(new BigDecimal(0));
            orderItem.setIntegrationAmount(new BigDecimal(0));
            orderItem.setRealAmount(product.getPrice().multiply(new BigDecimal(itemParam.getQuantity())));
            orderItemList.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getRealAmount());
        }
        //锁定库存（复用SKU锁定库存机制）
        for (PmsSkuStock skuStock : lockedSkuList) {
            int lockQuantity = orderItemList.stream()
                    .filter(item -> skuStock.getId().equals(item.getProductSkuId()))
                    .mapToInt(OmsOrderItem::getProductQuantity)
                    .sum();
            skuStock.setLockStock(skuStock.getLockStock() + lockQuantity);
            skuStockMapper.updateByPrimaryKeySelective(skuStock);
        }
        //构造订单
        OmsOrder order = new OmsOrder();
        order.setMemberId(currentMember.getId());
        order.setMemberUsername(currentMember.getUsername());
        order.setCreateTime(new Date());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setFreightAmount(new BigDecimal(0));
        order.setPromotionAmount(new BigDecimal(0));
        order.setIntegrationAmount(new BigDecimal(0));
        order.setCouponAmount(new BigDecimal(0));
        order.setDiscountAmount(new BigDecimal(0));
        order.setPayType(0);
        //订单来源：1->app订单
        order.setSourceType(1);
        //状态：0->已下单(待付款)
        order.setStatus(StallOrderConsts.STATUS_CREATED);
        //订单类型：2->摊位订单
        order.setOrderType(StallOrderConsts.ORDER_TYPE_STALL);
        order.setNote(stallOrderParam.getNote());
        order.setConfirmStatus(0);
        order.setDeleteStatus(0);
        order.setIntegration(0);
        order.setGrowth(0);
        order.setOrderSn(generateOrderSn());
        //生成取货码
        order.setPickupCode(pickupCodeGenerator.next());
        orderMapper.insert(order);
        for (OmsOrderItem orderItem : orderItemList) {
            orderItem.setOrderId(order.getId());
            orderItem.setOrderSn(order.getOrderSn());
        }
        orderItemDao.insertList(orderItemList);
        //发送延迟消息，超时未付款自动取消（MQ不可用时下单不阻断，超时取消降级为摊主手动取消）
        try {
            cancelOrderSender.sendMessage(order.getId(), stallOrderTimeoutMinutes * 60 * 1000L);
        } catch (RuntimeException e) {
            LOGGER.error("摊位订单超时取消延迟消息发送失败，orderId={}", order.getId(), e);
        }
        return order.getId();
    }

    @Override
    public CommonPage<OmsOrderDetail> list(Integer status, Integer pageNum, Integer pageSize) {
        if (status == null || status == -1) {
            status = null;
        } else if (status == StallOrderConsts.STATUS_FINISHED || status == StallOrderConsts.STATUS_CANCELED) {
            //历史态归类到对应摊位状态
        }
        UmsMember member = memberService.getCurrentMember();
        PageHelper.startPage(pageNum, pageSize);
        OmsOrderExample orderExample = new OmsOrderExample();
        OmsOrderExample.Criteria criteria = orderExample.createCriteria();
        criteria.andDeleteStatusEqualTo(0)
                .andMemberIdEqualTo(member.getId())
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
    public void cancelByMember(Long orderId) {
        UmsMember member = memberService.getCurrentMember();
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || !member.getId().equals(order.getMemberId())) {
            Asserts.fail("不能取消他人订单！");
        }
        if (order.getOrderType() == null || order.getOrderType() != StallOrderConsts.ORDER_TYPE_STALL) {
            Asserts.fail("非摊位订单，无法取消");
        }
        if (!StallOrderConsts.canMemberCancel(order.getStatus())) {
            Asserts.fail("订单制作中，请联系摊主取消");
        }
        updateOrderStatusAndReleaseStock(order, StallOrderConsts.STATUS_CANCELED);
    }

    /**
     * 更新订单状态并释放锁定库存（摊主取消与顾客取消、超时取消共用）
     */
    public void updateOrderStatusAndReleaseStock(OmsOrder order, Integer targetStatus) {
        OmsOrder updateOrder = new OmsOrder();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(targetStatus);
        orderMapper.updateByPrimaryKeySelective(updateOrder);
        OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
        orderItemExample.createCriteria().andOrderIdEqualTo(order.getId());
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
        if (!CollUtil.isEmpty(orderItemList)) {
            portalOrderDao.releaseSkuStockLock(orderItemList);
        }
    }

    private String generateOrderSn() {
        StringBuilder sb = new StringBuilder();
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = REDIS_DATABASE + ":" + REDIS_KEY_ORDER_ID + date;
        Long increment = redisService.incr(key, 1);
        sb.append(date);
        //订单来源：1->app订单；支付方式：0->未支付（当面付）
        sb.append(String.format("%02d", 1));
        sb.append(String.format("%02d", 0));
        String incrementStr = increment.toString();
        if (incrementStr.length() <= 6) {
            sb.append(String.format("%06d", increment));
        } else {
            sb.append(incrementStr);
        }
        return sb.toString();
    }
}
