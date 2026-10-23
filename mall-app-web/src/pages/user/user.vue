<template>
  <view class="container">
    <view class="user-section">
      <!-- #ifdef MP -->
      <view class="mp-nav-btns">
        <text class="yticon icon-shezhi nav-btn" @click="handleNavTo('/pages/set/set')"></text>
        <text class="yticon icon-icon-- nav-btn" @click="handleNavToNotice"></text>
      </view>
      <!-- #endif -->
      <view class="user-info-box">
        <view class="portrait-box" @click="handleAvatarTap">
          <image class="portrait" :src="memberInfo?.icon || '/static/missing-face.png'"></image>
        </view>
        <view class="info-box">
          <text class="username">{{
            memberInfo?.nickname || memberInfo?.username || adminInfo?.username || '游客'
          }}</text>
        </view>
      </view>
    </view>

    <view
      class="cover-container"
      :style="[{ transform: coverTransform, transition: coverTransition }]"
      @touchstart="handleCoverTouchstart"
      @touchmove="handleCoverTouchmove"
      @touchend="handleCoverTouchend"
    >
      <!-- 订单 -->
      <view class="order-section">
        <view
          class="order-item"
          @click="handleNavToOrder()"
          hover-class="common-hover"
          :hover-stay-time="50"
        >
          <text class="yticon icon-shouye"></text>
          <text>全部订单</text>
        </view>
        <view
          class="order-item"
          @click="handleNavToOrder(1)"
          hover-class="common-hover"
          :hover-stay-time="50"
        >
          <text class="yticon icon-daifukuan"></text>
          <text>已下单</text>
        </view>
      </view>
      <!-- 摊主模式入口（已登录摊主会话时显示） -->
      <view v-if="memberStore.isAdmin" class="history-section icon">
        <mix-list-cell
          icon="icon-shouye"
          iconColor="#d9622b"
          title="摊主模式"
          border=""
          @eventClick="handleNavTo('/pages/vendor/index')"
        ></mix-list-cell>
      </view>
      <!-- 浏览历史 -->
      <view class="history-section icon">
        <mix-list-cell
          icon="icon-shezhi1"
          iconColor="#a08c7a"
          title="设置"
          border=""
          @eventClick="handleNavTo('/pages/set/set')"
        ></mix-list-cell>
      </view>
    </view>
    <WechatAuthSheet />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow, onNavigationBarButtonTap } from '@dcloudio/uni-app'
import { useMemberStore } from '@/stores/member'
import { useOrderTabStore } from '@/stores/order-tab'
import mixListCell from '@/components/mix-list-cell.vue'
import WechatAuthSheet from '@/components/wechat-auth-sheet.vue'
import { requestWechatAuthorization } from '@/utils/wechat-auth'
import { applyTabBarVisibility } from '@/utils/tab-bar-visibility'

// ===== Store 相关 =====
// 获取会员store
const memberStore = useMemberStore()
// 获取订单Tab store（switchTab 跨页传递目标 Tab）
const orderTabStore = useOrderTabStore()
// 是否登录
const hasLogin = computed(() => memberStore.authType === 'member')
// 会员信息
const memberInfo = computed(() => memberStore.memberInfo)
const adminInfo = computed(() => memberStore.adminInfo)
const avatarTapCount = ref(0)
let avatarTapTimer: ReturnType<typeof setTimeout> | undefined

// ===== 页面数据 =====
// 遮罩层变换样式
const coverTransform = ref('translateY(0px)')
// 遮罩层过渡动画
const coverTransition = ref('0s')
// 是否正在拖动
const moving = ref(false)
// 触摸起始Y坐标
let startY = 0
// 触摸移动Y坐标
let moveY = 0
// 页面是否在顶部
const pageAtTop = true

// ===== 生命周期 =====
// 页面显示时刷新状态
onShow(() => {
  applyTabBarVisibility()
})

// 导航栏按钮点击事件
onNavigationBarButtonTap((e: { index: number }) => {
  const index = e.index
  if (index === 0) {
    handleNavTo('/pages/set/set')
  } else if (index === 1) {
    // #ifdef APP-PLUS
    const pages = getCurrentPages()
    const page = pages[pages.length - 1]
    // @ts-ignore
    const currentWebview = page.$getAppWebview()
    currentWebview.hideTitleNViewButtonRedDot({
      index,
    })
    // #endif
    uni.navigateTo({
      url: '/pages/notice/notice',
    })
  }
})

// ===== 事件处理方法 =====
// 页面导航（无任何会话时跳转登录页；摊主会话下摊主模式入口也依赖此放行）
const handleNavTo = (url: string) => {
  if (url === '/pages/set/set' && memberStore.hasSession) {
    uni.navigateTo({ url })
    return
  }
  if (!memberStore.hasSession) {
    requestWechatAuthorization(() => {
      uni.navigateTo({ url })
    })
    return
  }
  uni.navigateTo({
    url,
  })
}

// 跳转"我的订单"（tabBar 页须用 switchTab，且无法携带 query，先记录目标 Tab）
const handleNavToOrder = (tabIndex?: number) => {
  if (!hasLogin.value) {
    requestWechatAuthorization(() => handleNavToOrder(tabIndex))
    return
  }
  if (tabIndex != null) {
    orderTabStore.setPendingTab(tabIndex)
  }
  uni.switchTab({ url: '/pages/order/order' })
}

const handleAvatarTap = () => {
  if (memberStore.hasSession) {
    avatarTapCount.value = 0
    if (avatarTapTimer) clearTimeout(avatarTapTimer)
    // uni.showToast({ title: '请先退出当前登录', icon: 'none' })
    return
  }
  avatarTapCount.value += 1
  if (avatarTapTimer) clearTimeout(avatarTapTimer)
  if (avatarTapCount.value >= 5) {
    avatarTapCount.value = 0
    uni.navigateTo({ url: '/pages/public/login?mode=admin' })
    return
  }
  avatarTapTimer = setTimeout(() => {
    avatarTapCount.value = 0
  }, 1000)
}

// 跳转到通知页（小程序端导航栏右侧按钮）
const handleNavToNotice = () => {
  uni.navigateTo({
    url: '/pages/notice/notice',
  })
}

// 遮罩层触摸开始
const handleCoverTouchstart = (e: TouchEvent) => {
  if (!pageAtTop) {
    return
  }
  coverTransition.value = 'transform .1s linear'
  startY = e.touches[0].clientY
}

// 遮罩层触摸移动
const handleCoverTouchmove = (e: TouchEvent) => {
  moveY = e.touches[0].clientY
  let moveDistance = moveY - startY
  if (moveDistance < 0) {
    moving.value = false
    return
  }
  moving.value = true
  if (moveDistance >= 80 && moveDistance < 100) {
    moveDistance = 80
  }

  if (moveDistance > 0 && moveDistance <= 80) {
    coverTransform.value = `translateY(${moveDistance}px)`
  }
}

// 遮罩层触摸结束
const handleCoverTouchend = () => {
  if (moving.value === false) {
    return
  }
  moving.value = false
  coverTransition.value = 'transform 0.3s cubic-bezier(.21,1.93,.53,.64)'
  coverTransform.value = 'translateY(0px)'
}
</script>

<style lang="scss" scoped>
%flex-center {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

%section {
  display: flex;
  justify-content: space-around;
  align-content: center;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(74, 55, 40, 0.06);
}

/* 我的页配色：奶油暖底 + 杏色渐变头部 */
$user-bg: #fff9f0;
$user-apricot: #ffe8cc;
$user-apricot-deep: #ffd9ad;
$user-text-main: #4a3728;

.user-section {
  height: 420rpx;
  padding: 100rpx 30rpx 0;
  position: relative;
  background: linear-gradient(160deg, $user-apricot 0%, $user-apricot-deep 100%);

  .mp-nav-btns {
    position: absolute;
    top: 60rpx;
    right: 30rpx;
    z-index: 10;
    display: flex;
    align-items: center;
    gap: 30rpx;

    .nav-btn {
      font-size: 48rpx;
      color: $user-text-main;
    }
  }
}

.user-info-box {
  height: 180rpx;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;

  .portrait {
    width: 130rpx;
    height: 130rpx;
    border: 5rpx solid #fff;
    border-radius: 50%;
    box-shadow: 0 4rpx 12rpx rgba(74, 55, 40, 0.15);
  }

  .username {
    font-size: 38rpx;
    font-weight: 600;
    color: $user-text-main;
    margin-left: 20rpx;
  }
}

.vip-card-box {
  display: flex;
  flex-direction: column;
  color: #f7d680;
  height: 240rpx;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.8));
  border-radius: 16rpx 16rpx 0 0;
  overflow: hidden;
  position: relative;
  padding: 20rpx 24rpx;

  .card-bg {
    position: absolute;
    top: 20rpx;
    right: 0;
    width: 380rpx;
    height: 260rpx;
  }

  .b-btn {
    position: absolute;
    right: 20rpx;
    top: 16rpx;
    width: 132rpx;
    height: 40rpx;
    text-align: center;
    line-height: 40rpx;
    font-size: 22rpx;
    color: #36343c;
    border-radius: 20px;
    background: linear-gradient(to right, #f9e6af, #ffd465);
    z-index: 1;
  }

  .tit {
    font-size: 30rpx;
    color: #f7d680;
    margin-bottom: 28rpx;

    .yticon {
      color: #f6e5a3;
      margin-right: 16rpx;
    }
  }

  .e-b {
    font-size: 24rpx;
    color: #d8cba9;
    margin-top: 10rpx;
  }
}

.cover-container {
  background: $user-bg;
  margin-top: -80rpx;
  padding: 0 30rpx 20rpx;
  position: relative;
  border-radius: 32rpx 32rpx 0 0;
}

.tj-sction {
  @extend %section;

  .tj-item {
    @extend %flex-center;
    flex-direction: column;
    height: 140rpx;
    font-size: 24rpx;
    color: #75787d;
  }

  .num {
    font-size: 32rpx;
    color: #303133;
    margin-bottom: 8rpx;
  }
}

.order-section {
  @extend %section;
  padding: 28rpx 0;
  margin-top: 20rpx;

  .order-item {
    @extend %flex-center;
    width: 120rpx;
    height: 120rpx;
    border-radius: 10rpx;
    font-size: 24rpx;
    color: #303133;
  }

  .yticon {
    font-size: 48rpx;
    margin-bottom: 18rpx;
    color: #d9622b;
  }

  .icon-shouhoutuikuan {
    font-size: 44rpx;
  }
}

.history-section {
  padding: 30rpx 0 0;
  margin-top: 20rpx;
  background: #fff;
  border-radius: 10rpx;
}
</style>
