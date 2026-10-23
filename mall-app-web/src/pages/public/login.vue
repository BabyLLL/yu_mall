<template>
  <view class="login-page">
    <view class="topbar">
      <view class="back-btn yticon icon-zuojiantou-up" @click="navBack"></view>
      <text class="topbar-label">{{ isAdminMode ? '管理员入口' : '商城会员' }}</text>
    </view>

    <view v-if="isAdminMode" class="login-card">
      <view class="brand-mark">
        <text class="brand-dot"></text>
        <text>MALL</text>
      </view>
      <text class="eyebrow">{{ isAdminMode ? 'SECURE ACCESS' : 'WELCOME BACK' }}</text>
      <text class="title">{{ isAdminMode ? '后台管理登录' : '微信授权登录' }}</text>
      <text class="subtitle">
        {{ isAdminMode ? '请输入管理员账号与密码继续' : '授权微信身份，开启你的购物体验' }}
      </text>

      <view class="form-content">
        <view class="input-item">
          <text class="input-label">管理员账号</text>
          <input v-model="username" type="text" placeholder="请输入账号" :maxlength="32" />
        </view>
        <view class="input-item">
          <text class="input-label">登录密码</text>
          <input
            v-model="password"
            password
            placeholder="请输入密码"
            :maxlength="64"
            @confirm="toAdminLogin"
          />
        </view>
        <button class="primary-btn" :disabled="logining" @click="toAdminLogin">
          {{ logining ? '登录中…' : '进入管理后台' }}
        </button>
      </view>

      <view v-if="errorMessage" class="error-message">{{ errorMessage }}</view>
    </view>

    <view v-else class="member-entry">
      <view class="wechat-ring"><text class="yticon icon-weixin"></text></view>
      <text class="title">微信授权登录</text>
      <button class="primary-btn" @click="openWechatSheet">继续</button>
    </view>

    <view class="footer-note">
      <text>{{ isAdminMode ? '管理员入口已受保护' : '游客可浏览商品，登录后解锁完整服务' }}</text>
    </view>
    <WechatAuthSheet />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useMemberStore } from '@/stores/member'
import WechatAuthSheet from '@/components/wechat-auth-sheet.vue'
import { requestWechatAuthorization } from '@/utils/wechat-auth'

const memberStore = useMemberStore()
const mode = ref<'member' | 'admin'>('member')
const redirect = ref('')
const username = ref('')
const password = ref('')
const logining = ref(false)
const errorMessage = ref('')
const isAdminMode = computed(() => mode.value === 'admin')

onLoad((query) => {
  const params = query as { mode?: string; redirect?: string }
  mode.value = params.mode === 'admin' ? 'admin' : 'member'
  redirect.value = params.redirect ? decodeURIComponent(params.redirect) : ''
  if (memberStore.hasSession) {
    errorMessage.value = '当前已有登录，请先退出'
    uni.showToast({ title: errorMessage.value, icon: 'none' })
    setTimeout(() => {
      const pages = getCurrentPages()
      if (pages.length > 1) uni.navigateBack()
    }, 500)
    return
  }
  if (mode.value === 'member') setTimeout(openWechatSheet, 0)
})

const finishLogin = () => {
  if (redirect.value) {
    const target = redirect.value.startsWith('/') ? redirect.value : '/pages/index/index'
    // tabBar 页走 switchTab（user 页为 tabBar 第三项；redirectTo 到 tab 页会失败）
    const tabPages = ['/pages/index/index', '/pages/order/order', '/pages/user/user']
    if (tabPages.includes(target)) {
      uni.switchTab({ url: target })
    } else {
      uni.redirectTo({ url: target })
    }
  } else {
    const pages = getCurrentPages()
    pages.length > 1 ? uni.navigateBack() : uni.switchTab({ url: '/pages/index/index' })
  }
}

const openWechatSheet = () => {
  requestWechatAuthorization(finishLogin)
}

const toAdminLogin = async () => {
  if (memberStore.hasSession) {
    errorMessage.value = '当前已有登录，请先退出'
    return
  }
  if (!username.value || !password.value) {
    errorMessage.value = '请输入管理员账号和密码'
    return
  }
  logining.value = true
  errorMessage.value = ''
  try {
    await memberStore.adminLogin(username.value, password.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(finishLogin, 400)
  } catch (error) {
    const response = error as { data?: { code?: number; message?: string } }
    errorMessage.value =
      response?.data?.code === 409
        ? '当前已有登录，请先退出'
        : response?.data?.message || '账号或密码错误，请重试'
  } finally {
    logining.value = false
  }
}

const navBack = () => uni.navigateBack()
</script>

<style lang="scss">
page {
  background: #f7f8fc;
}

.login-page {
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 26rpx) 42rpx 44rpx;
  position: relative;
  overflow: hidden;
  color: #202636;
}

.topbar {
  height: 72rpx;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}
.back-btn {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 38rpx;
  color: #4b5565;
}
.topbar-label {
  margin-left: 12rpx;
  font-size: 26rpx;
  color: #626c7d;
}

.login-card {
  position: relative;
  z-index: 1;
  margin-top: 78rpx;
  padding: 56rpx 42rpx 48rpx;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 16rpx;
  box-shadow: 0 22rpx 60rpx rgba(49, 58, 78, 0.1);
}

.brand-mark {
  display: flex;
  align-items: center;
  color: #202636;
  font-size: 30rpx;
  font-weight: 700;
}
.brand-dot {
  width: 16rpx;
  height: 16rpx;
  margin-right: 12rpx;
  border-radius: 50%;
  background: #fa436a;
  box-shadow: 10rpx 0 0 #ffb35c;
}
.eyebrow {
  display: block;
  margin-top: 76rpx;
  color: #fa436a;
  font-size: 22rpx;
}
.title {
  display: block;
  margin-top: 16rpx;
  color: #202636;
  font-size: 52rpx;
  font-weight: 700;
}
.subtitle {
  display: block;
  margin-top: 16rpx;
  color: #8992a3;
  font-size: 26rpx;
  line-height: 1.65;
}

.member-entry {
  margin-top: 160rpx;
  text-align: center;
}
.wechat-ring {
  width: 168rpx;
  height: 168rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 16rpx solid #e5f8ef;
  border-radius: 50%;
  color: #20c77a;
  background: #f7fffb;
}
.member-entry .wechat-ring {
  margin: 0 auto 40rpx;
}
.wechat-ring .yticon {
  font-size: 76rpx;
}

.form-content {
  margin-top: 56rpx;
}
.input-item {
  padding: 22rpx 24rpx 18rpx;
  margin-bottom: 22rpx;
  border: 2rpx solid #edf0f5;
  border-radius: 16rpx;
  background: #fbfcfe;
}
.input-label {
  display: block;
  color: #717b8d;
  font-size: 22rpx;
}
.input-item input {
  width: 100%;
  height: 54rpx;
  margin-top: 6rpx;
  color: #202636;
  font-size: 30rpx;
}
.primary-btn {
  width: 100%;
  height: 88rpx;
  margin-top: 34rpx;
  border-radius: 18rpx;
  background: #fa436a;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 12rpx 24rpx rgba(250, 67, 106, 0.2);
}
.primary-btn[disabled] {
  opacity: 0.58;
}
.privacy-copy {
  display: block;
  margin-top: 26rpx;
  color: #a0a8b6;
  font-size: 22rpx;
}
.error-message {
  margin-top: 28rpx;
  padding: 18rpx 20rpx;
  border-radius: 12rpx;
  background: #fff1f3;
  color: #d73557;
  text-align: center;
  font-size: 24rpx;
}
.footer-note {
  position: absolute;
  z-index: 1;
  bottom: 42rpx;
  left: 0;
  width: 100%;
  color: #a6adba;
  text-align: center;
  font-size: 22rpx;
}
</style>
