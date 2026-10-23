<template>
  <view>
    <view class="status-section">
      <image :src="orderStatus.image" class="icon" />
      <text class="label-text">{{ orderStatus.text }}</text>
    </view>

    <!-- 取货码（进行中订单大字展示） -->
    <view v-if="isOngoing(order.status)" class="pickup-section">
      <text class="pickup-label">取货码</text>
      <text class="pickup-code">{{ order.pickupCode || '--' }}</text>
      <text v-if="order.status === 3" class="pickup-hint">已出餐，请到摊取货</text>
      <text v-else class="pickup-hint">请留意摊主叫号，取餐时当面付款</text>
    </view>

    <view class="goods-section">
      <view class="g-header b-b">
        <text class="name">商品信息</text>
      </view>
      <!-- 商品列表 -->
      <view class="g-item" v-for="item in order.orderItemList" :key="item.id">
        <image :src="item.productPic"></image>
        <view class="right">
          <text class="title clamp">{{ item.productName }}</text>
          <view class="price-box">
            <text class="price">￥{{ item.productPrice }}</text>
            <text class="number">x {{ item.productQuantity }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 金额明细 -->
    <view class="yt-list">
      <view class="yt-list-cell b-b">
        <text class="cell-tit clamp">商品合计</text>
        <text class="cell-tip">￥{{ order.totalAmount }}</text>
      </view>
      <view class="yt-list-cell desc-cell">
        <text class="cell-tit clamp">备注</text>
        <text class="cell-tip">{{ order.note || '无' }}</text>
      </view>
    </view>

    <!-- 订单明细 -->
    <view class="yt-list">
      <view class="yt-list-cell b-b">
        <text class="cell-tit clamp">订单编号</text>
        <text class="cell-tip">{{ order.orderSn }}</text>
      </view>
      <view class="yt-list-cell b-b">
        <text class="cell-tit clamp">提交时间</text>
        <text class="cell-tip">{{ formatDateTime(order.createTime) }}</text>
      </view>
      <view
        class="yt-list-cell b-b"
        v-if="order.status === 1 || order.status === 2 || order.status === 3 || order.status === 4"
      >
        <text class="cell-tit clamp">收款金额</text>
        <text class="cell-tip">￥{{ order.payAmount }}</text>
      </view>
      <view
        class="yt-list-cell b-b"
        v-if="order.status === 1 || order.status === 2 || order.status === 3 || order.status === 4"
      >
        <text class="cell-tit clamp">收款时间</text>
        <text class="cell-tip">{{ formatDateTime(order.paymentTime) }}</text>
      </view>
    </view>

    <!-- 底部 -->
    <view class="footer" v-if="order.status === 0">
      <view class="action-box b-t">
        <button class="action-btn" @click="handleCancelOrder(order.id)">取消订单</button>
      </view>
      <view class="price-content">
        <text>应付金额</text>
        <text class="price-tip">￥</text>
        <text class="price">{{ order.payAmount }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { formatDate } from '@/utils/date'
import { cancelStallOrderAPI, getStallOrderListAPI } from '@/apis/stall'
import { STALL_ORDER_STATUS, STALL_ORDER_STATUS_TEXT } from '@/types/stall'
import type { StallOrder } from '@/types/stall'

// ===== 页面数据 =====
// 订单ID
const orderId = ref<number>(0)
// 订单详情
const order = ref<StallOrder>({} as StallOrder)
// 订单状态信息
const orderStatus = ref<{ text: string; image: string }>({ text: '', image: '' })

// ===== 状态辅助 =====
// 进行中（未完成未取消）大字展示取货码
const isOngoing = (status: number): boolean => {
  return (
    status === STALL_ORDER_STATUS.CREATED ||
    status === STALL_ORDER_STATUS.PAID ||
    status === STALL_ORDER_STATUS.MAKING ||
    status === STALL_ORDER_STATUS.READY
  )
}

// ===== loadData =====
// 加载订单详情（摊位订单走 /stall/order/list 过滤取单条，详情接口由订单列表数据承载取货码）
const loadData = async () => {
  try {
    const res = await getStallOrderListAPI({ pageNum: 1, pageSize: 50 })
    const list = (res.data?.list || []) as StallOrder[]
    const found = list.find((item) => item.id === orderId.value)
    if (found) {
      order.value = found
      setOrderStatus(found.status)
    }
  } catch (e) {
    console.error('加载订单详情失败', e)
  }
}

// ===== onLoad =====
// 页面加载
onLoad((options) => {
  if (options?.orderId) {
    orderId.value = +options.orderId
    loadData()
  }
})

// 页面显示时刷新（摊主可能刚推进了状态）
onShow(() => {
  if (orderId.value) {
    loadData()
  }
})

// ===== 事件处理方法 =====

// 取消订单（仅已下单状态可取消）
const handleCancelOrder = (id: number) => {
  uni.showModal({
    title: '提示',
    content: '是否要取消该订单？',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '请稍后' })
        try {
          await cancelStallOrderAPI(id)
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

// ===== 其他方法 =====

// 设置订单状态信息（摊位状态机）
const setOrderStatus = (status: number) => {
  const statusMap: Record<number, { text: string; image: string }> = {
    0: { text: '已下单 · 待付款', image: '/static/icon_wait.png' },
    1: { text: '已付款 · 待制作', image: '/static/icon_deliver.png' },
    2: { text: '制作中', image: '/static/icon_deliver.png' },
    3: { text: '待取货', image: '/static/icon_receive.png' },
    4: { text: '已完成', image: '/static/icon_finish.png' },
    5: { text: '已取消', image: '/static/icon_close.png' },
  }
  orderStatus.value = statusMap[status] || { text: STALL_ORDER_STATUS_TEXT[status] || '', image: '' }
}

// 格式化日期时间
const formatDateTime = (time: string | null | undefined): string => {
  if (time == null || time === '') {
    return 'N/A'
  }
  const date = new Date(time)
  return formatDate(date, 'yyyy-MM-dd hh:mm:ss')
}
</script>

<style lang="scss">
page {
  background: $page-color-base;
}
</style>

<style lang="scss" scoped>
$stall-red: #c23a22;
$text-main: #3d2b1f;
$text-sub: #8a7a6d;

.status-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 0 32rpx;
  background: #fff;

  .icon {
    width: 96rpx;
    height: 96rpx;
    margin-bottom: 16rpx;
  }

  .label-text {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-main;
  }
}

/* 大字取货码 */
.pickup-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 24rpx;
  padding: 40rpx 32rpx;
  background: #fff;
  border: 2rpx dashed rgba(194, 58, 34, 0.4);
  border-radius: 20rpx;

  .pickup-label {
    font-size: 26rpx;
    color: $text-sub;
  }

  .pickup-code {
    font-size: 120rpx;
    font-weight: 800;
    color: $stall-red;
    letter-spacing: 8rpx;
    line-height: 1.2;
    margin: 12rpx 0;
  }

  .pickup-hint {
    font-size: 24rpx;
    color: $text-sub;
  }
}

.goods-section {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 32rpx;

  .g-header {
    display: flex;
    align-items: center;
    height: 60rpx;

    .name {
      font-size: 30rpx;
      font-weight: 600;
      color: $text-main;
    }
  }
}

.g-item {
  display: flex;
  padding: 20rpx 0;

  image {
    display: block;
    width: 120rpx;
    height: 120rpx;
    border-radius: 12rpx;
  }

  .right {
    flex: 1;
    padding-left: 24rpx;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .title {
      font-size: 28rpx;
      color: $text-main;
    }

    .price-box {
      display: flex;
      justify-content: space-between;
      align-items: baseline;

      .price {
        font-size: 28rpx;
        font-weight: 600;
        color: $stall-red;
      }

      .number {
        font-size: 24rpx;
        color: $text-sub;
      }
    }
  }
}

.yt-list {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 0 32rpx;
}

.yt-list-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;

  .cell-tit {
    flex: 1;
    font-size: 28rpx;
    color: $text-main;
  }

  .cell-tip {
    font-size: 28rpx;
    color: $text-main;
  }
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding-bottom: env(safe-area-inset-bottom);

  .action-box {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    height: 100rpx;
    padding-right: 24rpx;

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
  }

  .price-content {
    display: flex;
    align-items: baseline;
    justify-content: flex-end;
    padding: 0 24rpx 20rpx;
    font-size: 26rpx;
    color: $text-sub;

    .price-tip {
      margin-left: 8rpx;
      color: $stall-red;
    }

    .price {
      font-size: 36rpx;
      font-weight: 700;
      color: $stall-red;
    }
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
