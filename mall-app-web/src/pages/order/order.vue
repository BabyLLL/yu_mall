<template>
  <view class="content">
    <view class="navbar">
      <view
        v-for="(item, index) in navList"
        :key="index"
        class="nav-item"
        :class="{ current: tabCurrentIndex === index }"
        @click="handleTabClick(index)"
      >
        {{ item.text }}
      </view>
    </view>

    <swiper
      :current="tabCurrentIndex"
      class="swiper-box"
      :duration="300"
      @change="handleSwiperChange"
    >
      <swiper-item v-for="(tabItem, tabIndex) in navList" :key="tabIndex" class="tab-content">
        <scroll-view class="list-scroll-content" scroll-y @scrolltolower="loadData('add')">
          <!-- 空白页 -->
          <empty v-if="orderList == null || orderList.length === 0"></empty>

          <!-- 订单列表 -->
          <view
            v-for="(item, index) in orderList"
            :key="index"
            class="order-item"
            hover-class="common-hover"
            :hover-stay-time="80"
          >
            <view class="i-top b-b">
              <text class="time">{{ formatDateTime(item.createTime) }}</text>
              <text class="state" :style="{ color: statusColor(item.status) }">{{
                formatStatus(item.status)
              }}</text>
            </view>

            <!-- 进行中订单：大字取货码常驻展示 -->
            <view v-if="isOngoing(item.status)" class="pickup-box">
              <view class="pickup-left">
                <text class="pickup-label">取货码</text>
                <text class="pickup-code">{{ item.pickupCode || '--' }}</text>
              </view>
              <text v-if="item.status === 3" class="pickup-hint">已出餐，请到摊取货</text>
              <text v-else class="pickup-hint">请留意摊主叫号</text>
            </view>

            <view
              v-for="(orderItem, itemIndex) in item.orderItemList"
              :key="itemIndex"
              class="goods-box-single"
              @click="handleShowOrderDetail(item.id)"
            >
              <image class="goods-img" :src="orderItem.productPic" mode="aspectFill"></image>
              <view class="right">
                <text class="title clamp">{{ orderItem.productName }}</text>
                <text class="attr-box">x {{ orderItem.productQuantity }}</text>
                <text class="price">{{ orderItem.productPrice }}</text>
              </view>
            </view>

            <view class="price-box">
              共
              <text class="num">{{ calcTotalQuantity(item) }}</text>
              件商品 实付款
              <text class="price">{{ item.payAmount }}</text>
            </view>
            <view v-if="item.status === 0" class="action-box b-t">
              <button class="action-btn" @click="handleCancelOrder(item.id)">取消订单</button>
              <button class="action-btn recom" @click="handleShowOrderDetail(item.id)">
                查看详情
              </button>
            </view>
            <view v-if="item.note" class="note-box">
              <text class="note-label">备注</text>
              <text class="note-text">{{ item.note }}</text>
            </view>
          </view>

          <uni-load-more v-if="orderList != null && orderList.length > 0" :status="loadingType"></uni-load-more>
        </scroll-view>
      </swiper-item>
    </swiper>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onLoad, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import { formatDate } from '@/utils/date'
import { cancelStallOrderAPI, getStallOrderListAPI } from '@/apis/stall'
import { useOrderTabStore } from '@/stores/order-tab'
import { STALL_ORDER_STATUS, STALL_ORDER_STATUS_TEXT } from '@/types/stall'
import type { StallOrder } from '@/types/stall'
// empty 组件为平铺路径（components/empty.vue），easycom 默认规则不扫描，需显式引入
import empty from '@/components/empty.vue'

// ===== 导航数据 =====
// Tab 导航列表（摊位状态）
const navList = [
  { state: -1, text: '全部' },
  { state: STALL_ORDER_STATUS.CREATED, text: '已下单' },
  { state: STALL_ORDER_STATUS.MAKING, text: '制作中' },
  { state: STALL_ORDER_STATUS.READY, text: '待取货' },
  { state: STALL_ORDER_STATUS.FINISHED, text: '已完成' },
]

// ===== Store 相关 =====
// 订单Tab store（接收其他页面 switchTab 过来的目标 Tab）
const orderTabStore = useOrderTabStore()

// ===== 页面数据 =====
// 当前选中的Tab索引
const tabCurrentIndex = ref(0)
// 订单列表
const orderList = ref<StallOrder[]>([])
// 加载状态：more-加载更多 / loading-加载中 / nomore-没有更多
const loadingType = ref<'more' | 'loading' | 'noMore'>('more')
// 分页请求参数
const searchParam = ref<{ status: number; pageNum: number; pageSize: number }>({
  status: -1,
  pageNum: 1,
  pageSize: 5,
})

// ===== 状态辅助 =====
// 进行中（未完成未取消）的订单大字展示取货码
const isOngoing = (status: number): boolean => {
  return (
    status === STALL_ORDER_STATUS.CREATED ||
    status === STALL_ORDER_STATUS.PAID ||
    status === STALL_ORDER_STATUS.MAKING ||
    status === STALL_ORDER_STATUS.READY
  )
}

// 状态颜色
const statusColor = (status: number): string => {
  if (status === STALL_ORDER_STATUS.READY) return '#C23A22'
  if (status === STALL_ORDER_STATUS.CANCELED) return '#8a7a6d'
  if (status === STALL_ORDER_STATUS.FINISHED) return '#52a352'
  return '#C23A22'
}

// ===== loadData =====
// 加载订单列表数据
const loadData = async (type: 'refresh' | 'add' = 'refresh') => {
  if (type === 'refresh') {
    searchParam.value.pageNum = 1
  } else {
    searchParam.value.pageNum++
  }

  const index = tabCurrentIndex.value
  const navItem = navList[index]

  if (loadingType.value === 'loading') {
    // 防止重复加载
    return
  }

  searchParam.value.status = navItem.state
  loadingType.value = 'loading'

  try {
    const res = await getStallOrderListAPI(searchParam.value)
    const list = res.data.list as StallOrder[]
    if (type === 'refresh') {
      orderList.value = list
      loadingType.value = 'more'
    } else {
      if (list != null && list.length > 0) {
        orderList.value = orderList.value.concat(list)
        loadingType.value = 'more'
      } else {
        searchParam.value.pageNum--
        loadingType.value = 'noMore'
      }
    }
  } catch (e) {
    console.error('加载订单列表失败', e)
    loadingType.value = 'more'
  }

  // 下拉刷新时停止刷新动画
  if (type === 'refresh') {
    uni.stopPullDownRefresh()
  }
}

// ===== 轮询自动刷新 =====
// 摊主推进状态后，顾客停留在订单页也能自动看到最新状态（约 10s 一轮）
const POLL_INTERVAL_MS = 10000
// 轮询定时器（onShow 启动，onHide/onUnload 停止；无进行中订单时自动停止）
let pollTimer: ReturnType<typeof setInterval> | null = null

// 是否存在进行中订单（状态 0-3）
const hasOngoingOrder = computed(() => orderList.value.some((o) => isOngoing(o.status)))

// 按当前是否有进行中订单同步轮询启停
const syncPolling = () => {
  if (hasOngoingOrder.value) {
    if (pollTimer == null) {
      pollTimer = setInterval(silentRefresh, POLL_INTERVAL_MS)
    }
  } else {
    stopPolling()
  }
}

const stopPolling = () => {
  if (pollTimer != null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 静默刷新当前 tab 列表（复用 loadData 的 loading 防重入）
const silentRefresh = async () => {
  await loadData('refresh')
  syncPolling()
}

// 应用其他页面（如"我的"页）switchTab 过来的目标 Tab（switchTab 无法携带 query）
const applyPendingTab = () => {
  const pending = orderTabStore.takePendingTab()
  if (pending != null) {
    tabCurrentIndex.value = pending
  }
}

// ===== onLoad =====
// 页面加载时执行
onLoad((options) => {
  if (options?.state != null) {
    tabCurrentIndex.value = +options.state
  }
  applyPendingTab()
  loadData()
})

// 页面显示时刷新（从摊主模式切回、下单完成、从"我的"页跳转等场景），并按需启动轮询
onShow(async () => {
  applyPendingTab()
  await loadData('refresh')
  syncPolling()
})

// 页面隐藏/卸载时停止轮询
onHide(() => {
  stopPolling()
})

onUnload(() => {
  stopPolling()
})

// ===== onPullDownRefresh =====
// 下拉刷新
onPullDownRefresh(() => {
  loadData('refresh')
})

// ===== 事件处理方法 =====

// Swiper切换
const handleSwiperChange = async (e: { detail: { current: number } }) => {
  tabCurrentIndex.value = e.detail.current
  await loadData()
  syncPolling()
}

// 顶部Tab点击
const handleTabClick = async (index: number) => {
  tabCurrentIndex.value = index
  await loadData()
  syncPolling()
}

// 取消订单（仅已下单状态可取消）
const handleCancelOrder = (orderId: number) => {
  uni.showModal({
    title: '提示',
    content: '是否要取消该订单？',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '请稍后' })
        try {
          await cancelStallOrderAPI(orderId)
          uni.hideLoading()
          uni.showToast({ icon: 'success', title: '订单已取消' })
          loadData()
        } catch (e: any) {
          uni.hideLoading()
          uni.showToast({
            icon: 'none',
            title: e?.message || '取消失败，订单制作中请联系摊主',
          })
          console.error('取消订单失败', e)
        }
      }
    },
  })
}

// 查看订单详情
const handleShowOrderDetail = (orderId: number) => {
  uni.navigateTo({
    url: `/pages/order/orderDetail?orderId=${orderId}`,
  })
}

// ===== 其他方法 =====

// 格式化订单状态（摊位状态机）
const formatStatus = (status: number): string => {
  return STALL_ORDER_STATUS_TEXT[status] || ''
}

// 格式化日期时间
const formatDateTime = (time: string | null | undefined): string => {
  if (time == null || time === '') {
    return 'N/A'
  }
  const date = new Date(time)
  return formatDate(date, 'yyyy-MM-dd hh:mm:ss')
}

// 计算商品总数量
const calcTotalQuantity = (order: StallOrder): number => {
  let totalQuantity = 0
  if (order.orderItemList != null && order.orderItemList.length > 0) {
    for (const item of order.orderItemList) {
      totalQuantity += item.productQuantity
    }
  }
  return totalQuantity
}
</script>

<style lang="scss">
page {
  background: $page-color-base;
  height: 100%;
}
</style>

<style lang="scss" scoped>
$content-bg: #fdf8f2;
$stall-red: #c23a22;
$text-main: #3d2b1f;
$text-sub: #8a7a6d;

.content {
  background: $content-bg;
  height: 100%;
}

.swiper-box {
  height: calc(100% - 40px);
}

.list-scroll-content {
  height: 100%;
}

.navbar {
  display: flex;
  height: 40px;
  padding: 0 5px;
  background: #fff;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 10;

  .nav-item {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    font-size: 15px;
    color: $text-main;
    position: relative;

    &.current {
      color: $stall-red;

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 0;
        transform: translateX(-50%);
        width: 44px;
        height: 0;
        border-bottom: 2px solid $stall-red;
      }
    }
  }
}

.order-item {
  display: flex;
  flex-direction: column;
  padding: 20rpx 30rpx;
  background: #fff;
  margin: 20rpx 24rpx 0;
  border-radius: 16rpx;
}

.i-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60rpx;
  /* 供全局 .b-b:after 绝对定位，细线相对卡片头部而非页面根 */
  position: relative;

  .time {
    font-size: 24rpx;
    color: $text-sub;
  }

  .state {
    font-size: 26rpx;
    font-weight: 600;
  }
}

/* 大字取货码 */
.pickup-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12rpx 0;
  padding: 20rpx 24rpx;
  background: rgba(194, 58, 34, 0.06);
  border: 2rpx dashed rgba(194, 58, 34, 0.4);
  border-radius: 16rpx;

  .pickup-left {
    display: flex;
    align-items: baseline;
  }

  .pickup-label {
    font-size: 24rpx;
    color: $text-sub;
    margin-right: 16rpx;
  }

  .pickup-code {
    font-size: 72rpx;
    font-weight: 800;
    color: $stall-red;
    letter-spacing: 4rpx;
    line-height: 1.1;
  }

  .pickup-hint {
    font-size: 24rpx;
    color: $text-sub;
  }
}

.goods-box-single {
  display: flex;
  padding: 20rpx 0;

  .goods-img {
    display: block;
    width: 120rpx;
    height: 120rpx;
    border-radius: 12rpx;
  }

  .right {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    flex: 1;
    padding-left: 24rpx;

    .title {
      font-size: 28rpx;
      color: $text-main;
    }

    .attr-box {
      font-size: 24rpx;
      color: $text-sub;
    }

    .price {
      font-size: 28rpx;
      font-weight: 600;
      color: $stall-red;
    }
  }
}

.price-box {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  padding: 20rpx 0;
  font-size: 26rpx;
  color: $text-sub;

  .num {
    margin: 0 8rpx;
    color: $text-main;
  }

  .price {
    margin-left: 8rpx;
    font-size: 32rpx;
    font-weight: 700;
    color: $stall-red;
  }
}

.action-box {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 100rpx;
  /* 供全局 .b-t:after 绝对定位，细线相对操作栏而非页面根 */
  position: relative;

  .action-btn {
    width: 200rpx;
    height: 60rpx;
    margin: 0;
    margin-left: 20rpx;
    padding: 0;
    text-align: center;
    line-height: 60rpx;
    font-size: 26rpx;
    color: $text-main;
    background: #fff;
    border-radius: 100px;
    border: 2rpx solid #e8dfd4;
  }

  .recom {
    background: $stall-red;
    color: #fff;
    border: none;
  }
}

.note-box {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 0;
  border-top: 1rpx solid #f0e9e1;

  .note-label {
    font-size: 24rpx;
    color: $text-sub;
    margin-right: 16rpx;
    flex-shrink: 0;
  }

  .note-text {
    font-size: 24rpx;
    color: $text-main;
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
