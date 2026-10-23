<template>
  <view class="container">
    <!-- 顶部：摊主信息 -->
    <view class="vendor-header">
      <view class="vendor-title-box">
        <text class="vendor-title">摊主模式</text>
        <text class="vendor-sub">{{ adminInfo?.username || '已登录摊主' }}</text>
      </view>
      <view class="logout-row">
        <view class="header-btn" @click="handleNavToMine">
          <text>我的</text>
        </view>
        <view class="header-btn outline" @click="handleLogout">
          <text>退出</text>
        </view>
      </view>
    </view>

    <!-- 今日战绩卡片 -->
    <view class="stats-row" @click="handleNavToSales">
      <view class="stats-card">
        <text class="stats-num">¥{{ formatAmount(stats?.totalAmount) }}</text>
        <text class="stats-label">今日实收</text>
      </view>
      <view class="stats-card">
        <text class="stats-num">{{ stats?.orderCount ?? 0 }}</text>
        <text class="stats-label">今日订单</text>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="entry-card" @click="handleNavToProducts">
      <view class="entry-info">
        <text class="entry-title">商品管理</text>
        <text class="entry-sub">{{ productCount }} 个商品 · 上下架与库存</text>
      </view>
      <text class="entry-arrow">›</text>
    </view>
    <view class="entry-card" @click="handleNavToOrders">
      <view class="entry-info">
        <text class="entry-title">订单队列</text>
        <text class="entry-sub">{{ ongoingCount }} 单进行中 · 收款与出餐</text>
      </view>
      <text class="entry-arrow">›</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useMemberStore } from '@/stores/member'
import {
  getVendorOrdersAPI,
  getVendorProductsAPI,
  getVendorTodayStatsAPI,
} from '@/apis/stall'
import { applyTabBarVisibility } from '@/utils/tab-bar-visibility'
import { STALL_ORDER_STATUS } from '@/types/stall'
import type { StallMenuItem, StallOrder, VendorTodayStats } from '@/types/stall'

const memberStore = useMemberStore()
const adminInfo = computed(() => memberStore.adminInfo)

// ===== 页面数据 =====
// 今日战绩
const stats = ref<VendorTodayStats | null>(null)
// 商品数（入口辅助信息）
const productCount = ref(0)
// 进行中订单数（入口辅助信息）
const ongoingCount = ref(0)

// ===== 加载 =====
// 加载今日战绩
const loadStats = async () => {
  try {
    const res = await getVendorTodayStatsAPI()
    stats.value = res.data || null
  } catch (e) {
    console.error('加载今日战绩失败', e)
  }
}

// 加载入口辅助信息（商品数 / 进行中订单数）
const loadEntrySummary = async () => {
  try {
    const [productsRes, ordersRes] = await Promise.all([
      getVendorProductsAPI(),
      getVendorOrdersAPI({ status: -1, pageNum: 1, pageSize: 50 }),
    ])
    const products = (productsRes.data || []) as StallMenuItem[]
    const orders = (ordersRes.data?.list || []) as StallOrder[]
    productCount.value = products.length
    ongoingCount.value = orders.filter(
      (o) =>
        o.status === STALL_ORDER_STATUS.CREATED ||
        o.status === STALL_ORDER_STATUS.PAID ||
        o.status === STALL_ORDER_STATUS.MAKING ||
        o.status === STALL_ORDER_STATUS.READY,
    ).length
  } catch (e) {
    console.error('加载入口信息失败', e)
  }
}

onShow(() => {
  applyTabBarVisibility()
  loadStats()
  loadEntrySummary()
})

// ===== 事件处理 =====
// 金额统一两位小数
const formatAmount = (value?: number) => {
  return Number(value ?? 0).toFixed(2)
}

// 进入今日战绩明细页
const handleNavToSales = () => {
  uni.navigateTo({ url: '/pages/vendor/sales' })
}

// 进入商品管理页
const handleNavToProducts = () => {
  uni.navigateTo({ url: '/pages/vendor/products' })
}

// 进入订单队列页
const handleNavToOrders = () => {
  uni.navigateTo({ url: '/pages/vendor/orders' })
}

// 返回「我的」页（tabBar 保持隐藏直至退出摊主）
const handleNavToMine = () => {
  uni.switchTab({ url: '/pages/user/user' })
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
  padding: 24rpx;
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

  .logout-row {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  .header-btn {
    padding: 10rpx 32rpx;
    background: rgba(255, 255, 255, 0.16);
    border-radius: 32rpx;
    font-size: 26rpx;
    color: #ffffff;

    &.outline {
      background: transparent;
      border: 2rpx solid rgba(255, 255, 255, 0.4);
    }
  }
}

/* 今日战绩卡片（与战绩明细页同源样式） */
.stats-row {
  display: flex;
  gap: 24rpx;
  margin-bottom: 24rpx;

  .stats-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40rpx 0 32rpx;
    background: linear-gradient(160deg, $stall-dark 0%, #241811 100%);
    border-radius: 20rpx;

    .stats-num {
      font-size: 44rpx;
      font-weight: 700;
      color: #ffffff;
    }

    .stats-label {
      margin-top: 12rpx;
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.6);
    }
  }
}

/* 功能入口卡片 */
.entry-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 36rpx;
  margin-bottom: 24rpx;
  background: $stall-card;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(61, 43, 31, 0.06);

  .entry-info {
    display: flex;
    flex-direction: column;
  }

  .entry-title {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-main;
  }

  .entry-sub {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $text-sub;
  }

  .entry-arrow {
    font-size: 48rpx;
    font-weight: 600;
    color: $stall-red;
    line-height: 48rpx;
  }
}
</style>
