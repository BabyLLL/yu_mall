<template>
  <view class="container">
    <view class="profile-card">
      <view class="cell">
        <text class="cell-label">头像</text>
        <image class="avatar" :src="memberStore.memberInfo?.icon || '/static/missing-face.png'" />
      </view>
      <view class="cell">
        <text class="cell-label">昵称</text>
        <input
          class="nickname-input"
          v-model="nickname"
          type="nickname"
          :maxlength="64"
          placeholder="输入新昵称"
          placeholder-class="nickname-placeholder"
        />
      </view>
    </view>
    <button class="save-btn" :loading="saving" :disabled="saving" @click="handleSave">保存</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useMemberStore } from '@/stores/member'
import { requestWechatAuthorization } from '@/utils/wechat-auth'

const memberStore = useMemberStore()

// 昵称输入（初值取当前会话资料）
const nickname = ref('')
const saving = ref(false)

const initFromStore = () => {
  nickname.value = memberStore.memberInfo?.nickname || memberStore.memberInfo?.username || ''
}

onShow(() => {
  if (memberStore.hasLogin) {
    initFromStore()
    return
  }
  // 未登录先引导登录，取消登录则返回上一页
  requestWechatAuthorization(() => {
    if (memberStore.hasLogin) {
      initFromStore()
    } else {
      uni.navigateBack()
    }
  })
})

// 保存昵称
const handleSave = async () => {
  const trimmed = nickname.value.trim()
  if (!trimmed) {
    uni.showToast({ title: '昵称不能为空', icon: 'none' })
    return
  }
  if (trimmed.length > 64) {
    uni.showToast({ title: '昵称长度不能超过64个字符', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await memberStore.updateMemberProfile(trimmed)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch {
    uni.showToast({ title: '保存失败，请检查网络后重试', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 24rpx;
}

.profile-card {
  background: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(74, 55, 40, 0.06);
  overflow: hidden;
}

.cell {
  display: flex;
  align-items: center;
  padding: 28rpx 30rpx;

  & + .cell {
    border-top: 1rpx solid #faf4ea;
  }

  .cell-label {
    width: 140rpx;
    font-size: 28rpx;
    color: #4a3728;
  }

  .avatar {
    margin-left: auto;
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
    border: 3rpx solid #ffe8cc;
  }

  .nickname-input {
    flex: 1;
    margin-left: auto;
    text-align: right;
    font-size: 28rpx;
    color: #4a3728;
  }
}

.nickname-placeholder {
  color: #a08c7a;
}

.save-btn {
  margin-top: 48rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
  background: $uni-color-primary;
  border-radius: 44rpx;

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.6;
    color: #ffffff;
    background: $uni-color-primary;
  }
}
</style>
