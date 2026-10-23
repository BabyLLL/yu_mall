<template>
  <view class="container">
    <view
      v-if="!memberStore.isAdmin"
      class="list-cell b-b"
      @click="navTo('/pages/user/profile')"
      hover-class="cell-hover"
      :hover-stay-time="50"
    >
      <text class="cell-tit">个人资料</text>
      <text class="cell-more yticon icon-you"></text>
    </view>
    <view class="list-cell log-out-btn" @click="toLogout">
      <text class="cell-tit">退出登录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useMemberStore } from '@/stores/member'
// 获取会员store
const memberStore = useMemberStore()

// 退出登录
const toLogout = () => {
  uni.showModal({
    content: '确定要退出登录么',
    success: async (e) => {
      if (e.confirm) {
        uni.showLoading({ title: '正在退出' })
        try {
          await memberStore.memberLogout()
          uni.showToast({ title: '已退出登录', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 200)
        } catch {
          uni.showToast({ title: '退出失败，请检查网络后重试', icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    },
  })
}

// 页面内导航
const navTo = (url: string) => {
  uni.navigateTo({
    url: url,
  })
}
</script>

<style lang="scss">
page {
  background: #fff9f0;
}
</style>

<style lang="scss" scoped>
.list-cell {
  display: flex;
  align-items: baseline;
  padding: 24rpx $page-row-spacing;
  line-height: 60rpx;
  position: relative;
  background: #fff;
  &.log-out-btn {
    margin-top: 24rpx;
    .cell-tit {
      color: $uni-color-primary;
      text-align: center;
      margin-right: 0;
    }
  }
  &.cell-hover {
    background: #faf6f0;
  }
  &.b-b:after {
    left: 30rpx;
  }
  .cell-more {
    align-self: baseline;
    font-size: $font-lg;
    color: $font-color-light;
    margin-left: 10rpx;
  }
  .cell-tit {
    flex: 1;
    font-size: $font-base + 2rpx;
    color: $font-color-dark;
    margin-right: 10rpx;
  }
}
</style>
