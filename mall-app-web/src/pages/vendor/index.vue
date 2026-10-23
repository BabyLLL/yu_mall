<template>
  <view class="container">
    <!-- 顶部：摊主信息 -->
    <view class="vendor-header">
      <view class="vendor-title-box">
        <text class="vendor-title">摊主模式</text>
        <text class="vendor-sub">{{ adminInfo?.username || '已登录摊主' }}</text>
      </view>
      <view class="logout-btn" @click="handleLogout">
        <text>退出</text>
      </view>
    </view>

    <!-- 分段切换 -->
    <view class="segment-box">
      <view
        class="segment-item"
        :class="{ current: segment === 'products' }"
        @click="handleSwitchSegment('products')"
      >
        <text>商品管理</text>
      </view>
      <view
        class="segment-item"
        :class="{ current: segment === 'orders' }"
        @click="handleSwitchSegment('orders')"
      >
        <text>订单队列</text>
      </view>
    </view>

    <!-- 商品管理 -->
    <view v-if="segment === 'products'" class="section-box">
      <view v-for="item in productList" :key="item.id" class="product-row">
        <image class="product-pic" :src="item.pic" mode="aspectFill" />
        <view class="product-info">
          <text class="product-name clamp">{{ item.name }}</text>
          <text class="product-price">¥{{ item.price }}</text>
        </view>
        <switch
          :checked="item.publishStatus === 1"
          color="#C23A22"
          @change="(e: any) => handleTogglePublish(item, e)"
        />
      </view>
      <view v-if="productList.length === 0" class="empty-box">
        <text class="empty-text">暂无商品，请到管理后台添加</text>
      </view>
    </view>

    <!-- 订单队列 -->
    <view v-if="segment === 'orders'" class="section-box">
      <!-- 状态过滤 -->
      <view class="order-filter">
        <view
          v-for="f in orderFilters"
          :key="f.state"
          class="filter-item"
          :class="{ current: orderFilter === f.state }"
          @click="handleSwitchFilter(f.state)"
        >
          <text>{{ f.text }}</text>
        </view>
      </view>

      <view v-for="order in orderList" :key="order.id" class="order-card">
        <view class="order-card-top">
          <text class="order-code">{{ order.pickupCode || order.orderSn }}</text>
          <text class="order-state">{{ statusText(order.status) }}</text>
        </view>
        <view class="order-goods">
          <text v-for="(g, gi) in order.orderItemList" :key="gi" class="order-goods-line">
            {{ g.productName }} x {{ g.productQuantity }}
          </text>
        </view>
        <view v-if="order.note" class="order-note">
          <text class="order-note-label">备注</text>
          <text class="order-note-text">{{ order.note }}</text>
        </view>
        <view class="order-card-bottom">
          <text class="order-amount">¥{{ order.payAmount }}</text>
          <view class="order-actions">
            <view
              v-if="order.status === 0"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'confirmPay')"
            >
              <text>确认收款</text>
            </view>
            <view
              v-if="order.status === 1"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'startMaking')"
            >
              <text>开始制作</text>
            </view>
            <view
              v-if="order.status === 2"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'finishMaking')"
            >
              <text>出餐完成</text>
            </view>
            <view
              v-if="order.status === 3"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'finish')"
            >
              <text>取货完成</text>
            </view>
            <view
              v-if="order.status !== 4 && order.status !== 5"
              class="order-action-btn plain"
              @click="handleCancelOrder(order.id)"
            >
              <text>取消</text>
            </view>
          </view>
        </view>
      </view>
      <view v-if="orderList.length === 0" class="empty-box">
        <text class="empty-text">暂无订单</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useMemberStore } from '@/stores/member'
import {
  cancelVendorOrderAPI,
  confirmPayAPI,
  finishMakingAPI,
  finishOrderAPI,
  getVendorOrdersAPI,
  getVendorProductsAPI,
  hideVendorProductAPI,
  showVendorProductAPI,
  startMakingAPI,
} from '@/apis/stall'
import { STALL_ORDER_STATUS, STALL_ORDER_STATUS_TEXT } from '@/types/stall'
import type { StallMenuItem, StallOrder } from '@/types/stall'

const memberStore = useMemberStore()
const adminInfo = computed(() => memberStore.adminInfo)

// ===== 页面数据 =====
// 当前分段
const segment = ref<'products' | 'orders'>('products')
// 商品列表
const productList = ref<StallMenuItem[]>([])
// 订单列表
const orderList = ref<StallOrder[]>([])
// 订单过滤状态
const orderFilter = ref(-1)
// 订单过滤选项
const orderFilters = [
  { state: -1, text: '全部' },
  { state: STALL_ORDER_STATUS.CREATED, text: '待收款' },
  { state: STALL_ORDER_STATUS.PAID, text: '已付款' },
  { state: STALL_ORDER_STATUS.MAKING, text: '制作中' },
  { state: STALL_ORDER_STATUS.READY, text: '待取货' },
]

// ===== 加载 =====
// 加载商品列表
const loadProducts = async () => {
  try {
    const res = await getVendorProductsAPI()
    productList.value = (res.data || []) as StallMenuItem[]
  } catch (e) {
    console.error('加载商品失败', e)
  }
}

// 加载订单队列
const loadOrders = async () => {
  try {
    const res = await getVendorOrdersAPI({ status: orderFilter.value, pageNum: 1, pageSize: 50 })
    orderList.value = (res.data?.list || []) as StallOrder[]
  } catch (e) {
    console.error('加载订单失败', e)
  }
}

onShow(() => {
  if (segment.value === 'products') {
    loadProducts()
  } else {
    loadOrders()
  }
})

// ===== 事件处理 =====
// 切换分段
const handleSwitchSegment = (seg: 'products' | 'orders') => {
  segment.value = seg
  if (seg === 'products') {
    loadProducts()
  } else {
    loadOrders()
  }
}

// 切换订单过滤
const handleSwitchFilter = (state: number) => {
  orderFilter.value = state
  loadOrders()
}

// 商品上架/隐藏开关
const handleTogglePublish = async (item: StallMenuItem, e: any) => {
  const show = e.detail.value
  try {
    if (show) {
      await showVendorProductAPI(item.id)
      item.publishStatus = 1
    } else {
      await hideVendorProductAPI(item.id)
      item.publishStatus = 0
    }
    uni.showToast({ icon: 'success', title: show ? '已上架' : '已隐藏' })
  } catch (err: any) {
    // 失败回滚开关
    item.publishStatus = show ? 0 : 1
    uni.showToast({ icon: 'none', title: err?.message || '操作失败' })
  }
}

// 状态推进
const handleTransition = async (
  orderId: number,
  action: 'confirmPay' | 'startMaking' | 'finishMaking' | 'finish',
) => {
  try {
    if (action === 'confirmPay') {
      await confirmPayAPI(orderId)
    } else if (action === 'startMaking') {
      await startMakingAPI(orderId)
    } else if (action === 'finishMaking') {
      await finishMakingAPI(orderId)
    } else {
      await finishOrderAPI(orderId)
    }
    uni.showToast({ icon: 'success', title: '操作成功' })
    loadOrders()
  } catch (err: any) {
    uni.showToast({ icon: 'none', title: err?.message || '操作失败' })
  }
}

// 取消订单
const handleCancelOrder = (orderId: number) => {
  uni.showModal({
    title: '提示',
    content: '确认取消该订单？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelVendorOrderAPI(orderId)
          uni.showToast({ icon: 'success', title: '已取消' })
          loadOrders()
        } catch (err: any) {
          uni.showToast({ icon: 'none', title: err?.message || '取消失败' })
        }
      }
    },
  })
}

// 退出摊主模式
const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '退出摊主模式？',
    success: async (res) => {
      if (res.confirm) {
        try {
          const { miniappLogoutAPI } = await import('@/apis/member')
          await miniappLogoutAPI()
        } catch {
          // 注销失败也清本地会话
        }
        memberStore.clearSession()
        uni.switchTab({ url: '/pages/index/index' })
      }
    },
  })
}

// 状态文案
const statusText = (status: number): string => {
  return STALL_ORDER_STATUS_TEXT[status] || ''
}
</script>

<style lang="scss" scoped>
$stall-red: #c23a22;
$stall-dark: #3d2b1f;
$stall-bg: #fdf8f2;
$stall-card: #ffffff;
$text-main: #3d2b1f;
$text-sub: #8a7a6d;

page {
  background: $stall-bg;
}

.container {
  padding: 24rpx 24rpx 60rpx;
}

.vendor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 36rpx;
  background: linear-gradient(160deg, $stall-dark 0%, #241811 100%);
  border-radius: 20rpx;
  margin-bottom: 24rpx;

  .vendor-title-box {
    display: flex;
    flex-direction: column;
  }

  .vendor-title {
    font-size: 36rpx;
    font-weight: 700;
    color: #ffffff;
  }

  .vendor-sub {
    margin-top: 6rpx;
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.6);
  }

  .logout-btn {
    padding: 10rpx 32rpx;
    border: 2rpx solid rgba(255, 255, 255, 0.4);
    border-radius: 32rpx;
    font-size: 26rpx;
    color: #ffffff;
  }
}

.segment-box {
  display: flex;
  background: $stall-card;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 24rpx;

  .segment-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 72rpx;
    border-radius: 12rpx;
    font-size: 28rpx;
    color: $text-sub;

    &.current {
      background: $stall-red;
      color: #ffffff;
      font-weight: 600;
    }
  }
}

.section-box {
  background: $stall-card;
  border-radius: 20rpx;
  padding: 12rpx 24rpx;
}

.product-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f7f2ec;

  .product-pic {
    width: 88rpx;
    height: 88rpx;
    border-radius: 12rpx;
    margin-right: 20rpx;
  }

  .product-info {
    flex: 1;
    display: flex;
    flex-direction: column;

    .product-name {
      font-size: 28rpx;
      color: $text-main;
      margin-bottom: 6rpx;
    }

    .product-price {
      font-size: 26rpx;
      font-weight: 600;
      color: $stall-red;
    }
  }
}

.order-filter {
  display: flex;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f7f2ec;

  .filter-item {
    padding: 8rpx 24rpx;
    margin-right: 12rpx;
    border-radius: 28rpx;
    font-size: 24rpx;
    color: $text-sub;
    background: #f7f2ec;

    &.current {
      background: $stall-red;
      color: #ffffff;
    }
  }
}

.order-card {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f7f2ec;

  .order-card-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12rpx;

    .order-code {
      font-size: 40rpx;
      font-weight: 800;
      color: $stall-red;
      letter-spacing: 2rpx;
    }

    .order-state {
      font-size: 26rpx;
      font-weight: 600;
      color: $text-main;
    }
  }

  .order-goods {
    display: flex;
    flex-direction: column;

    .order-goods-line {
      font-size: 26rpx;
      color: $text-main;
      line-height: 40rpx;
    }
  }

  .order-note {
    display: flex;
    margin-top: 8rpx;

    .order-note-label {
      font-size: 24rpx;
      color: $text-sub;
      margin-right: 12rpx;
      flex-shrink: 0;
    }

    .order-note-text {
      font-size: 24rpx;
      color: $stall-red;
    }
  }

  .order-card-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 16rpx;

    .order-amount {
      font-size: 32rpx;
      font-weight: 700;
      color: $text-main;
    }

    .order-actions {
      display: flex;

      .order-action-btn {
        padding: 12rpx 28rpx;
        margin-left: 16rpx;
        border-radius: 32rpx;
        font-size: 26rpx;

        &.primary {
          background: $stall-red;
          color: #ffffff;
        }

        &.plain {
          border: 2rpx solid #e8dfd4;
          color: $text-sub;
        }
      }
    }
  }
}

.empty-box {
  display: flex;
  justify-content: center;
  padding: 80rpx 0;

  .empty-text {
    font-size: 26rpx;
    color: $text-sub;
  }
}

.clamp {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
</style>
