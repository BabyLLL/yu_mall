import { reactive } from 'vue'
import { readAuthSession } from '@/utils/auth-session'

type PendingAction = () => void | Promise<void>

export const wechatAuthState = reactive({
  visible: false,
  loading: false,
  error: '',
})

let pendingAction: PendingAction | undefined
let mountedHosts = 0

const LOGIN_PAGE_ROUTE = 'pages/public/login'

export const registerWechatAuthHost = () => {
  mountedHosts += 1
}

export const unregisterWechatAuthHost = () => {
  mountedHosts = Math.max(0, mountedHosts - 1)
}

// 直接打开授权弹层（调用方需保证当前页已挂载 WechatAuthSheet，无导航兜底）
export const showWechatAuthSheet = (action?: PendingAction) => {
  pendingAction = action
  wechatAuthState.error = ''
  wechatAuthState.visible = true
}

export const requestWechatAuthorization = (action?: PendingAction) => {
  const session = readAuthSession()
  if (session) {
    uni.showToast({ title: '当前已有登录，请先退出', icon: 'none' })
    return false
  }
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as { route?: string } | undefined
  // 已在登录页时不重复导航（登录页自身负责开弹层），防止登录页叠栈
  if (current?.route === LOGIN_PAGE_ROUTE) {
    return true
  }
  if (mountedHosts === 0) {
    const redirect = current?.route ? `/${current.route}` : '/pages/index/index'
    uni.navigateTo({
      url: `/pages/public/login?redirect=${encodeURIComponent(redirect)}`,
    })
    return true
  }
  showWechatAuthSheet(action)
  return true
}

export const cancelWechatAuthorization = () => {
  if (wechatAuthState.loading) return
  pendingAction = undefined
  wechatAuthState.error = ''
  wechatAuthState.visible = false
}

export const completeWechatAuthorization = async () => {
  const action = pendingAction
  pendingAction = undefined
  wechatAuthState.visible = false
  wechatAuthState.error = ''
  if (action) await action()
}

export const resetWechatAuthorization = () => {
  pendingAction = undefined
  wechatAuthState.visible = false
  wechatAuthState.loading = false
  wechatAuthState.error = ''
}
