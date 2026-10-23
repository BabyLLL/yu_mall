import type { AdminInfo, AuthSession, AuthType, MemberInfo } from '@/types/member'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { adminLoginAPI, miniappLogoutAPI, updateMemberInfoAPI, wechatLoginAPI } from '@/apis/member'
import {
  getMiniappClientId,
  readAuthSession,
  removeAuthSession,
  writeAuthSession,
} from '@/utils/auth-session'
import { resetWechatAuthorization } from '@/utils/wechat-auth'

export const useMemberStore = defineStore('member', () => {
  const authSession = ref<AuthSession | undefined>(readAuthSession())
  const memberInfo = ref<MemberInfo | undefined>(
    authSession.value?.type === 'member' ? (authSession.value.profile as MemberInfo) : undefined,
  )
  const adminInfo = ref<AdminInfo | undefined>(
    authSession.value?.type === 'admin' ? (authSession.value.profile as AdminInfo) : undefined,
  )

  const authType = computed<AuthType | undefined>(() => authSession.value?.type)
  const hasLogin = computed(() => authType.value === 'member')
  const hasSession = computed(() => !!authSession.value)
  const isAdmin = computed(() => authType.value === 'admin')

  const persistSession = (session: AuthSession) => {
    authSession.value = session
    memberInfo.value = session.type === 'member' ? (session.profile as MemberInfo) : undefined
    adminInfo.value = session.type === 'admin' ? (session.profile as AdminInfo) : undefined
    writeAuthSession(session)
  }

  const clearSession = () => {
    authSession.value = undefined
    memberInfo.value = undefined
    adminInfo.value = undefined
    removeAuthSession()
    uni.removeStorageSync('username')
    uni.removeStorageSync('password')
  }

  const wechatLogin = async (code: string, profile?: { nickname?: string; avatarUrl?: string }) => {
    if (authSession.value) throw new Error('logout-required')
    const clientId = getMiniappClientId()
    const loginRes = await wechatLoginAPI({ code, clientId, ...profile })
    const data = loginRes.data
    if (
      data.authType !== 'member' ||
      data.clientId !== clientId ||
      !data.token ||
      !data.tokenHead ||
      !data.sessionId ||
      !data.member
    ) {
      throw new Error('invalid-member-session')
    }
    persistSession({
      type: 'member',
      token: data.token,
      tokenHead: data.tokenHead,
      clientId: data.clientId,
      sessionId: data.sessionId,
      profile: data.member,
    })
  }

  const adminLogin = async (username: string, password: string) => {
    if (authSession.value) throw new Error('logout-required')
    const clientId = getMiniappClientId()
    const loginRes = await adminLoginAPI({ username, password }, clientId)
    const data = loginRes.data
    if (
      data.authType !== 'admin' ||
      data.clientId !== clientId ||
      !data.token ||
      !data.tokenHead ||
      !data.sessionId ||
      !data.admin
    ) {
      throw new Error('invalid-admin-session')
    }
    persistSession({
      type: 'admin',
      token: data.token,
      tokenHead: data.tokenHead,
      clientId: data.clientId,
      sessionId: data.sessionId,
      profile: data.admin,
    })
  }

  /** 修改当前会员昵称，成功后同步本地会话中的会员资料 */
  const updateMemberProfile = async (nickname: string) => {
    if (authSession.value?.type !== 'member') throw new Error('not-member-session')
    const res = await updateMemberInfoAPI({ nickname })
    const info = res.data
    const session: AuthSession = { ...authSession.value, profile: info }
    authSession.value = session
    memberInfo.value = info
    writeAuthSession(session)
  }

  const memberLogout = async () => {
    if (!authSession.value) return
    try {
      await miniappLogoutAPI()
      clearSession()
      resetWechatAuthorization()
    } catch (error) {
      const response = error as { data?: { code?: number } }
      if (response?.data?.code === 401) {
        clearSession()
        resetWechatAuthorization()
        return
      }
      throw error
    }
  }

  return {
    authSession,
    memberInfo,
    adminInfo,
    hasLogin,
    hasSession,
    authType,
    isAdmin,
    wechatLogin,
    adminLogin,
    updateMemberProfile,
    clearSession,
    memberLogout,
    getClientId: getMiniappClientId,
  }
})
