<template>
  <view
    v-if="wechatAuthState.visible"
    class="auth-overlay"
    role="dialog"
    aria-modal="true"
    aria-label="微信授权登录"
    @touchmove.stop.prevent
    @click="cancelWechatAuthorization"
  >
    <view class="auth-sheet" @click.stop>
      <view class="sheet-handle"></view>
      <button
        class="close-button"
        aria-label="关闭微信授权登录"
        :disabled="wechatAuthState.loading"
        @click="cancelWechatAuthorization"
      >
        ×
      </button>

      <view class="wechat-icon" aria-hidden="true">
        <text class="yticon icon-weixin"></text>
      </view>
      <text class="sheet-title">微信授权登录</text>
      <text class="sheet-copy">登录后可使用购物车、订单、地址和收藏等服务</text>

      <view v-if="wechatAuthState.error" class="error-message" role="alert">
        {{ wechatAuthState.error }}
      </view>

      <button class="authorize-button" :disabled="wechatAuthState.loading" @click="authorize">
        {{
          wechatAuthState.loading
            ? '授权中...'
            : wechatAuthState.error
            ? '重新授权'
            : '微信授权并登录'
        }}
      </button>
      <button
        class="cancel-button"
        :disabled="wechatAuthState.loading"
        @click="cancelWechatAuthorization"
      >
        暂不登录
      </button>
      <text class="privacy-copy">授权仅用于识别商城会员身份</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useMemberStore } from '@/stores/member'
import {
  cancelWechatAuthorization,
  completeWechatAuthorization,
  registerWechatAuthHost,
  unregisterWechatAuthHost,
  wechatAuthState,
} from '@/utils/wechat-auth'

const memberStore = useMemberStore()

onMounted(registerWechatAuthHost)
onUnmounted(unregisterWechatAuthHost)

const getErrorMessage = (error: unknown) => {
  const response = error as { data?: { message?: string; code?: number } }
  if (response?.data?.code === 409) return '当前已有登录，请先退出后再试'
  return response?.data?.message || '授权未完成，请检查网络后重试'
}

const authorize = () => {
  if (wechatAuthState.loading) return
  if (memberStore.hasSession) {
    wechatAuthState.error = '当前已有登录，请先退出后再试'
    return
  }
  wechatAuthState.loading = true
  wechatAuthState.error = ''

  // #ifdef MP-WEIXIN
  uni.login({
    provider: 'weixin',
    success: async (result) => {
      try {
        await memberStore.wechatLogin(result.code)
        await completeWechatAuthorization()
        uni.showToast({ title: '登录成功', icon: 'success' })
      } catch (error) {
        wechatAuthState.error = getErrorMessage(error)
      } finally {
        wechatAuthState.loading = false
      }
    },
    fail: () => {
      wechatAuthState.error = '未获得微信授权，可稍后重试'
      wechatAuthState.loading = false
    },
  })
  // #endif

  // #ifndef MP-WEIXIN
  wechatAuthState.error = '请在微信小程序中完成授权登录'
  wechatAuthState.loading = false
  // #endif
}
</script>

<style lang="scss" scoped>
.auth-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: flex-end;
  background: rgba(20, 24, 33, 0.48);
}

.auth-sheet {
  position: relative;
  width: 100%;
  min-height: 620rpx;
  padding: 18rpx 44rpx calc(32rpx + env(safe-area-inset-bottom));
  border-radius: 24rpx 24rpx 0 0;
  background: #fff;
  color: #202636;
  text-align: center;
  animation: sheet-in 220ms ease-out;
}

.sheet-handle {
  width: 72rpx;
  height: 8rpx;
  margin: 0 auto 38rpx;
  border-radius: 8rpx;
  background: #d9dde5;
}

.close-button {
  position: absolute;
  top: 24rpx;
  right: 28rpx;
  width: 64rpx;
  height: 64rpx;
  padding: 0;
  border-radius: 50%;
  background: #f2f4f7;
  color: #505969;
  font-size: 42rpx;
  line-height: 60rpx;
}

.wechat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112rpx;
  height: 112rpx;
  margin: 0 auto 28rpx;
  border-radius: 50%;
  background: #eaf8f1;
  color: #16a866;
}

.wechat-icon .yticon {
  font-size: 58rpx;
}

.sheet-title {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
}

.sheet-copy {
  display: block;
  margin: 18rpx auto 0;
  color: #626c7d;
  font-size: 26rpx;
  line-height: 1.6;
}

.error-message {
  margin-top: 24rpx;
  padding: 18rpx 22rpx;
  border: 2rpx solid #ffd5dc;
  border-radius: 12rpx;
  background: #fff5f6;
  color: #c52f4c;
  font-size: 24rpx;
  line-height: 1.5;
}

.authorize-button,
.cancel-button {
  width: 100%;
  height: 88rpx;
  border-radius: 12rpx;
  font-size: 30rpx;
  font-weight: 600;
}

.authorize-button {
  margin-top: 36rpx;
  background: #16a866;
  color: #fff;
}

.cancel-button {
  margin-top: 16rpx;
  background: #f2f4f7;
  color: #3e4756;
}

.authorize-button[disabled],
.cancel-button[disabled] {
  opacity: 0.58;
}

.privacy-copy {
  display: block;
  margin-top: 22rpx;
  color: #7a8494;
  font-size: 22rpx;
}

@keyframes sheet-in {
  from {
    transform: translateY(100%);
  }

  to {
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-sheet {
    animation: none;
  }
}
</style>
