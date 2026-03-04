<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createCustomerOrder,
  getOrderByIdAndPhone,
  getPublicCategories,
  getPublicMenuItems,
  getCustomerMe,
  getCustomerAddresses,
  createCustomerAddress,
  updateCustomerAddress,
  deleteCustomerAddress,
  setDefaultAddress,
} from '../api'
import { hasCustomerToken, clearCustomerToken } from '../utils/auth'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const categories = ref([])
const activeCategoryId = ref(null)
const menuItems = ref([])
const cart = ref([])

const orderForm = reactive({
  customerName: '',
  customerPhone: '',
  deliveryAddress: '',
  remark: '',
})

const cartTotal = computed(() =>
  cart.value.reduce(
    (sum, item) => sum + Number(item.price) * Number(item.quantity),
    0
  )
)

const cartCount = computed(() =>
  cart.value.reduce((sum, item) => sum + Number(item.quantity), 0)
)

const cartDrawerOpen = ref(false)
function toggleCartDrawer() {
  cartDrawerOpen.value = !cartDrawerOpen.value
}

// 订单查询
const orderLookupOpen = ref(false)
const orderLookupOrderId = ref('')
const orderLookupPhone = ref('')
const orderLookupLoading = ref(false)
const orderLookupResult = ref(null)
const orderLookupError = ref('')

const statusLabels = {
  PENDING: '待接单',
  ACCEPTED: '已接单',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

function toggleOrderLookup() {
  orderLookupOpen.value = !orderLookupOpen.value
  if (!orderLookupOpen.value) {
    orderLookupResult.value = null
    orderLookupError.value = ''
  }
}

async function queryOrder() {
  const id = orderLookupOrderId.value?.trim()
  const phone = orderLookupPhone.value?.trim()
  if (!id || !phone) {
    orderLookupError.value = '请输入订单号和手机号'
    orderLookupResult.value = null
    return
  }
  orderLookupError.value = ''
  orderLookupResult.value = null
  orderLookupLoading.value = true
  try {
    const { data } = await getOrderByIdAndPhone(id, phone)
    orderLookupResult.value = data
  } catch (e) {
    orderLookupError.value = e.message || '查询失败'
    orderLookupResult.value = null
  } finally {
    orderLookupLoading.value = false
  }
}

function formatOrderDate(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

function formatOrderMoney(value) {
  return Number(value).toFixed(2)
}
function closeCartDrawer() {
  cartDrawerOpen.value = false
}

// 前台用户登录与地址
const customerProfile = ref(null)
const addresses = ref([])
const myDropdownOpen = ref(false)
const addressDrawerOpen = ref(false)
const selectedAddressId = ref(null)
const addressForm = reactive({
  receiverName: '',
  phone: '',
  address: '',
  defaultAddress: false,
})
const editingAddressId = ref(null)
const addressSaving = ref(false)

const isLoggedIn = computed(() => hasCustomerToken() && customerProfile.value)

async function loadCustomerProfile() {
  if (!hasCustomerToken()) return
  try {
    const { data } = await getCustomerMe()
    customerProfile.value = data
    await loadAddresses()
  } catch (_) {
    customerProfile.value = null
    addresses.value = []
  }
}

async function loadAddresses() {
  if (!hasCustomerToken()) return
  try {
    const { data } = await getCustomerAddresses()
    addresses.value = data
    if (data.length > 0 && !selectedAddressId.value) {
      const defaultAddr = data.find((a) => a.defaultAddress) || data[0]
      selectedAddressId.value = defaultAddr.id
      applyAddressToOrder(defaultAddr)
    }
  } catch (_) {
    addresses.value = []
  }
}

function applyAddressToOrder(addr) {
  if (!addr) return
  orderForm.customerName = addr.receiverName
  orderForm.customerPhone = addr.phone
  orderForm.deliveryAddress = addr.address
}

function openAddressDrawer() {
  myDropdownOpen.value = false
  addressDrawerOpen.value = true
  editingAddressId.value = null
  addressForm.receiverName = ''
  addressForm.phone = ''
  addressForm.address = ''
  addressForm.defaultAddress = false
}

function closeAddressDrawer() {
  addressDrawerOpen.value = false
  editingAddressId.value = null
}

function editAddress(addr) {
  editingAddressId.value = addr.id
  addressForm.receiverName = addr.receiverName
  addressForm.phone = addr.phone
  addressForm.address = addr.address
  addressForm.defaultAddress = addr.defaultAddress
}

async function saveAddress() {
  if (!addressForm.receiverName || !addressForm.phone || !addressForm.address) {
    alert('请填写收货人、电话和地址')
    return
  }
  addressSaving.value = true
  try {
    const payload = {
      receiverName: addressForm.receiverName.trim(),
      phone: addressForm.phone.trim(),
      address: addressForm.address.trim(),
      defaultAddress: addressForm.defaultAddress,
    }
    if (editingAddressId.value) {
      await updateCustomerAddress(editingAddressId.value, payload)
      const idx = addresses.value.findIndex((a) => a.id === editingAddressId.value)
      if (idx !== -1) {
        addresses.value[idx] = { ...addresses.value[idx], ...payload }
      }
    } else {
      const { data } = await createCustomerAddress(payload)
      addresses.value = [data, ...addresses.value]
    }
    editingAddressId.value = null
    addressForm.receiverName = ''
    addressForm.phone = ''
    addressForm.address = ''
    addressForm.defaultAddress = false
  } catch (e) {
    alert(e.message)
  } finally {
    addressSaving.value = false
  }
}

async function doSetDefault(addr) {
  try {
    const { data } = await setDefaultAddress(addr.id)
    const idx = addresses.value.findIndex((a) => a.id === addr.id)
    if (idx !== -1) addresses.value[idx] = data
    addresses.value.forEach((a) => {
      if (a.id !== addr.id) a.defaultAddress = false
    })
    selectedAddressId.value = addr.id
    applyAddressToOrder(data)
  } catch (e) {
    alert(e.message)
  }
}

async function doDeleteAddress(addr) {
  if (!confirm('确定删除该地址？')) return
  try {
    await deleteCustomerAddress(addr.id)
    addresses.value = addresses.value.filter((a) => a.id !== addr.id)
    if (selectedAddressId.value === addr.id) {
      selectedAddressId.value = addresses.value[0]?.id || null
      if (addresses.value[0]) applyAddressToOrder(addresses.value[0])
    }
  } catch (e) {
    alert(e.message)
  }
}

function onSelectAddress() {
  const addr = addresses.value.find((a) => a.id === selectedAddressId.value)
  if (addr) applyAddressToOrder(addr)
}

function logoutCustomer() {
  clearCustomerToken()
  customerProfile.value = null
  addresses.value = []
  selectedAddressId.value = null
  myDropdownOpen.value = false
  orderForm.customerName = ''
  orderForm.customerPhone = ''
  orderForm.deliveryAddress = ''
}

const imageBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
function getImageUrl(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : imageBaseUrl + url
}

onMounted(async () => {
  document.title = '今天吃什么'
  await loadCategories()
  await loadMenuItems(activeCategoryId.value)
  await loadCustomerProfile()
})

async function loadCategories() {
  loading.value = true
  try {
    const { data } = await getPublicCategories()
    categories.value = data
    if (data.length > 0) {
      activeCategoryId.value = data[0].id
    }
  } catch (error) {
    alert(error.message)
  } finally {
    loading.value = false
  }
}

async function loadMenuItems(categoryId) {
  loading.value = true
  try {
    const { data } = await getPublicMenuItems(categoryId)
    menuItems.value = data
  } catch (error) {
    alert(error.message)
  } finally {
    loading.value = false
  }
}

async function selectCategory(categoryId) {
  activeCategoryId.value = categoryId
  await loadMenuItems(categoryId)
}

function addToCart(menuItem) {
  const existing = cart.value.find((item) => item.id === menuItem.id)
  if (existing) {
    existing.quantity += 1
    return
  }
  cart.value.push({
    id: menuItem.id,
    name: menuItem.name,
    price: menuItem.price,
    quantity: 1,
  })
}

function changeQuantity(item, delta) {
  const next = item.quantity + delta
  if (next <= 0) {
    cart.value = cart.value.filter((cartItem) => cartItem.id !== item.id)
    return
  }
  item.quantity = next
}

function clearCart() {
  cart.value = []
}

async function submitOrder() {
  if (!isLoggedIn) {
    alert('请先登录后再下单')
    router.push('/login')
    return
  }
  if (cart.value.length === 0) {
    alert('购物车为空，无法下单')
    return
  }
  if (!orderForm.customerName || !orderForm.customerPhone || !orderForm.deliveryAddress) {
    alert('请完整填写联系人、电话和地址')
    return
  }

  submitting.value = true
  try {
    const payload = {
      customerName: orderForm.customerName,
      customerPhone: orderForm.customerPhone,
      deliveryAddress: orderForm.deliveryAddress,
      remark: orderForm.remark,
      items: cart.value.map((item) => ({
        menuItemId: item.id,
        quantity: item.quantity,
      })),
    }
    const { data } = await createCustomerOrder(payload)
    alert(`下单成功，订单号：${data.orderId}`)
    clearCart()
    orderForm.remark = ''
    closeCartDrawer()
    orderLookupOrderId.value = String(data.orderId)
    orderLookupPhone.value = orderForm.customerPhone
    orderLookupOpen.value = true
    queryOrder()
  } catch (error) {
    alert(error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="meituan-layout" :class="{ 'has-cart-bar': cartCount }">
    <header class="meituan-header">
      <h1 class="logo">点餐</h1>
      <button type="button" class="header-order-lookup" @click="toggleOrderLookup">
        {{ orderLookupOpen ? '收起查询' : '查订单' }}
      </button>
      <div class="header-auth">
        <template v-if="!isLoggedIn">
          <router-link to="/login" class="header-link">登录</router-link>
          <router-link to="/login" class="header-link">注册</router-link>
        </template>
        <div v-else class="my-dropdown-wrap">
        <button type="button" class="header-my" @click="myDropdownOpen = !myDropdownOpen">
          我的 {{ customerProfile?.username }}
        </button>
        <Transition name="dropdown">
          <div v-show="myDropdownOpen" class="my-dropdown" @click.stop>
            <button type="button" class="my-dropdown-item" @click="openAddressDrawer">地址管理</button>
            <button type="button" class="my-dropdown-item" @click="logoutCustomer">退出登录</button>
          </div>
        </Transition>
        </div>
      </div>
      <div v-if="myDropdownOpen" class="dropdown-backdrop" @click="myDropdownOpen = false" />
    </header>

    <!-- 订单查询面板 -->
    <Transition name="slide">
      <section v-show="orderLookupOpen" class="order-lookup-panel">
        <div class="order-lookup-form">
          <input v-model="orderLookupOrderId" type="text" placeholder="订单号" class="lookup-input" />
          <input v-model="orderLookupPhone" type="text" placeholder="下单时填写的手机号" class="lookup-input" />
          <button type="button" class="lookup-btn" :disabled="orderLookupLoading" @click="queryOrder">
            {{ orderLookupLoading ? '查询中...' : '查询订单' }}
          </button>
        </div>
        <p v-if="orderLookupError" class="lookup-error">{{ orderLookupError }}</p>
        <div v-if="orderLookupResult" class="order-detail-card">
          <div class="order-detail-head">
            <span>订单 #{{ orderLookupResult.id }}</span>
            <span class="order-detail-date">{{ formatOrderDate(orderLookupResult.createdAt) }}</span>
            <span class="order-detail-status" :class="orderLookupResult.status">
              {{ statusLabels[orderLookupResult.status] || orderLookupResult.status }}
            </span>
          </div>
          <p class="order-detail-addr">
            {{ orderLookupResult.deliveryAddress }} · {{ orderLookupResult.customerName }} {{ orderLookupResult.customerPhone }}
          </p>
          <ul class="order-detail-items">
            <li v-for="item in orderLookupResult.items" :key="item.id">
              {{ item.menuItemName }} × {{ item.quantity }} = ￥{{ formatOrderMoney(item.subtotal) }}
            </li>
          </ul>
          <p class="order-detail-total">合计：￥{{ formatOrderMoney(orderLookupResult.totalAmount) }}</p>
        </div>
      </section>
    </Transition>

    <div class="meituan-main">
      <aside class="category-sidebar">
        <a
          v-for="category in categories"
          :key="category.id"
          href="javascript:;"
          class="category-item"
          :class="{ active: category.id === activeCategoryId }"
          @click.prevent="selectCategory(category.id)"
        >
          {{ category.name }}
        </a>
      </aside>

      <section class="menu-section">
        <div v-if="loading" class="loading-tip">加载中...</div>
        <div v-else class="menu-list">
          <article v-for="item in menuItems" :key="item.id" class="menu-item">
            <div v-if="item.imageUrl" class="menu-item-img">
              <img :src="getImageUrl(item.imageUrl)" :alt="item.name" />
            </div>
            <div v-else class="menu-item-img placeholder">暂无图片</div>
            <div class="menu-item-info">
              <h3>{{ item.name }}</h3>
              <p class="menu-desc">{{ item.description || '暂无描述' }}</p>
              <div class="menu-item-footer">
                <span class="menu-price">￥{{ Number(item.price).toFixed(2) }}</span>
                <button class="add-btn" @click="addToCart(item)">加入购物车</button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <!-- 底部购物车栏（有商品时才显示，参考美团；点击打开抽屉） -->
      <div v-if="cartCount" class="cart-bar">
        <div class="cart-bar-left" @click="toggleCartDrawer">
          <span class="cart-bar-icon-wrap">
            <span class="cart-bar-icon">🛒</span>
            <span v-if="cartCount" class="cart-bar-badge">{{ cartCount }}</span>
          </span>
          <div class="cart-bar-info">
            <span class="cart-bar-total">￥{{ cartTotal.toFixed(2) }}</span>
            <span class="cart-bar-hint">共{{ cartCount }}件</span>
          </div>
        </div>
        <button
          class="cart-bar-checkout"
          :class="{ disabled: !cartCount }"
          :disabled="!cartCount"
          @click="cartCount && toggleCartDrawer()"
        >
          去结算
        </button>
      </div>

      <Transition name="drawer">
        <div v-show="cartDrawerOpen" class="cart-drawer-overlay" @click="closeCartDrawer">
          <div class="cart-drawer" @click.stop>
            <div class="cart-drawer-header">
              <h3>购物车</h3>
              <button class="cart-drawer-close" @click="closeCartDrawer">✕</button>
            </div>
            <div class="cart-drawer-body">
              <div v-if="cart.length === 0" class="cart-empty">购物车是空的</div>
              <div v-else class="cart-list">
                <div v-for="item in cart" :key="item.id" class="cart-item">
                  <div class="cart-item-info">
                    <span class="cart-item-name">{{ item.name }}</span>
                    <span class="cart-item-price">￥{{ Number(item.price).toFixed(2) }}</span>
                  </div>
                  <div class="cart-item-qty">
                    <button class="qty-btn" @click="changeQuantity(item, -1)">－</button>
                    <span>{{ item.quantity }}</span>
                    <button class="qty-btn" @click="changeQuantity(item, 1)">＋</button>
                  </div>
                </div>
              </div>

              <div class="cart-total">
                <span>合计</span>
                <span class="total-amount">￥{{ cartTotal.toFixed(2) }}</span>
              </div>

              <div v-if="isLoggedIn && addresses.length > 0" class="address-select-wrap">
                <label>收货地址</label>
                <select v-model="selectedAddressId" class="address-select" @change="onSelectAddress">
                  <option v-for="a in addresses" :key="a.id" :value="a.id">
                    {{ a.receiverName }} {{ a.phone }} {{ a.address }}{{ a.defaultAddress ? ' [默认]' : '' }}
                  </option>
                </select>
              </div>
              <div class="order-form">
                <input v-model="orderForm.customerName" placeholder="联系人" />
                <input v-model="orderForm.customerPhone" placeholder="联系电话" />
                <input v-model="orderForm.deliveryAddress" placeholder="配送地址" />
                <textarea v-model="orderForm.remark" rows="2" placeholder="备注（可选）" />
              </div>

              <div class="cart-actions">
                <button class="btn-clear" @click="clearCart">清空</button>
                <button class="btn-submit" :disabled="submitting || cartCount === 0 || !isLoggedIn" @click="submitOrder">
                  {{ submitting ? '提交中...' : isLoggedIn ? '去结算' : '请先登录' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 地址管理抽屉（美团风格） -->
      <Transition name="drawer">
        <div v-show="addressDrawerOpen" class="address-drawer-overlay" @click="closeAddressDrawer">
          <div class="address-drawer" @click.stop>
            <header class="address-drawer-header">
              <span class="address-drawer-title">收货地址</span>
              <button type="button" class="address-drawer-close" aria-label="关闭" @click="closeAddressDrawer">×</button>
            </header>
            <div class="address-drawer-body">
              <section class="address-section">
                <div v-if="addresses.length === 0 && !editingAddressId" class="address-empty">
                  <span class="address-empty-icon">📍</span>
                  <p>暂无收货地址</p>
                  <p class="address-empty-hint">点击下方「新增地址」添加</p>
                </div>
                <div v-else class="address-list">
                  <div
                    v-for="a in addresses"
                    :key="a.id"
                    class="address-card"
                    :class="{ 'is-default': a.defaultAddress }"
                  >
                    <div class="address-card-left">
                      <span class="address-card-name">{{ a.receiverName }}</span>
                      <span class="address-card-phone">{{ a.phone }}</span>
                      <p class="address-card-addr">{{ a.address }}</p>
                      <span v-if="a.defaultAddress" class="address-card-default-tag">默认</span>
                    </div>
                    <div class="address-card-actions">
                      <button type="button" class="addr-link" @click="editAddress(a)">编辑</button>
                      <template v-if="!a.defaultAddress">
                        <span class="addr-divider">|</span>
                        <button type="button" class="addr-link" @click="doSetDefault(a)">设为默认</button>
                      </template>
                      <span class="addr-divider">|</span>
                      <button type="button" class="addr-link addr-link-danger" @click="doDeleteAddress(a)">删除</button>
                    </div>
                  </div>
                </div>
              </section>
              <section class="address-form-section">
                <h4 class="address-form-title">{{ editingAddressId ? '编辑地址' : '新增地址' }}</h4>
                <div class="address-form-row">
                  <input v-model="addressForm.receiverName" type="text" placeholder="收货人姓名" class="address-input" />
                </div>
                <div class="address-form-row">
                  <input v-model="addressForm.phone" type="tel" placeholder="手机号" class="address-input" />
                </div>
                <div class="address-form-row">
                  <input v-model="addressForm.address" type="text" placeholder="小区/楼栋/门牌号等详细地址" class="address-input" />
                </div>
                <label class="address-form-check">
                  <input v-model="addressForm.defaultAddress" type="checkbox" class="address-checkbox" />
                  <span>设为默认地址</span>
                </label>
                <button type="button" class="address-save-btn" :disabled="addressSaving" @click="saveAddress">
                  {{ addressSaving ? '保存中...' : (editingAddressId ? '保存' : '新增地址') }}
                </button>
              </section>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
/* 美团外卖风格 - 主色 #FFD100 */
.meituan-layout {
  min-height: 100vh;
  background: #f5f5f5;
  overflow-x: hidden;
}

.meituan-header {
  position: relative;
  z-index: 110;
  background: #fff;
  padding: 12px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.header-order-lookup {
  padding: 6px 14px;
  font-size: 13px;
  background: #fff;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
}

.header-order-lookup:hover {
  background: #fffbf0;
  border-color: #ffc107;
  color: #333;
}

.header-auth {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-link {
  padding: 6px 12px;
  font-size: 13px;
  color: #666;
  text-decoration: none;
  border-radius: 6px;
}

.header-link:hover {
  background: #fffbf0;
  color: #333;
}

.my-dropdown-wrap {
  position: relative;
}

.header-my {
  padding: 6px 14px;
  font-size: 13px;
  background: #fff;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
}

.header-my:hover {
  background: #fffbf0;
  border-color: #ffc107;
  color: #333;
}

.my-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  min-width: 120px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 111;
  overflow: hidden;
}

.my-dropdown-item {
  display: block;
  width: 100%;
  padding: 10px 14px;
  text-align: left;
  font-size: 14px;
  color: #333;
  background: none;
  border: none;
  cursor: pointer;
}

.my-dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-backdrop {
  position: fixed;
  inset: 0;
  z-index: 109;
}

.address-select-wrap {
  margin-bottom: 10px;
}

.address-select-wrap label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.address-select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ---------- 地址管理抽屉（美团风格） ---------- */
.address-drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.address-drawer {
  position: relative;
  width: 100%;
  max-width: 420px;
  height: 100%;
  background: #f5f5f5;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
}

.address-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.address-drawer-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
}

.address-drawer-close {
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  color: #999;
  cursor: pointer;
  border-radius: 50%;
}

.address-drawer-close:hover {
  background: #f5f5f5;
  color: #333;
}

.address-drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px 24px;
}

.address-section {
  margin-bottom: 16px;
}

.address-empty {
  padding: 40px 20px;
  text-align: center;
  background: #fff;
  border-radius: 12px;
}

.address-empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
  opacity: 0.6;
}

.address-empty p {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #666;
}

.address-empty-hint {
  font-size: 13px !important;
  color: #999 !important;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 12px;
  border-left: 3px solid transparent;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.address-card.is-default {
  border-left-color: #ffc107;
}

.address-card-left {
  flex: 1;
  min-width: 0;
}

.address-card-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-right: 8px;
}

.address-card-phone {
  font-size: 14px;
  color: #666;
}

.address-card-addr {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #666;
  line-height: 1.45;
}

.address-card-default-tag {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 8px;
  font-size: 11px;
  background: #fff8e6;
  color: #e6a800;
  border-radius: 4px;
}

.address-card-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}

.addr-link {
  padding: 4px 6px;
  font-size: 13px;
  background: none;
  border: none;
  color: #ffc107;
  cursor: pointer;
}

.addr-link:hover {
  color: #e6ac00;
  text-decoration: underline;
}

.addr-link-danger {
  color: #999;
}

.addr-link-danger:hover {
  color: #e64545;
}

.addr-divider {
  font-size: 12px;
  color: #ddd;
  user-select: none;
}

.address-form-section {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.address-form-title {
  margin: 0 0 14px 0;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.address-form-row {
  margin-bottom: 12px;
}

.address-input {
  width: 100%;
  box-sizing: border-box;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background: #fff;
  transition: border-color 0.2s;
}

.address-input::placeholder {
  color: #bbb;
}

.address-input:focus {
  outline: none;
  border-color: #ffc107;
}

.address-form-check {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0 18px 0;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.address-checkbox {
  width: 18px;
  height: 18px;
  accent-color: #ffc107;
}

.address-save-btn {
  width: 100%;
  height: 46px;
  padding: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  background: #ffc107;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.address-save-btn:hover:not(:disabled) {
  background: #ffcd38;
}

.address-save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.order-lookup-panel {
  background: #fff;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.order-lookup-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.lookup-input {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  width: 140px;
}

.lookup-input:focus {
  outline: none;
  border-color: #ffc107;
}

.lookup-btn {
  padding: 8px 16px;
  font-size: 14px;
  background: #ffc107;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.lookup-btn:hover:not(:disabled) {
  background: #e6ac00;
}

.lookup-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.lookup-error {
  margin: 0 0 12px 0;
  color: #c62828;
  font-size: 13px;
}

.order-detail-card {
  margin-top: 12px;
  padding: 14px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #eee;
}

.order-detail-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
}

.order-detail-date {
  font-size: 13px;
  font-weight: normal;
  color: #666;
}

.order-detail-status {
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #fff3e0;
  color: #e65100;
}

.order-detail-status.ACCEPTED {
  background: #e3f2fd;
  color: #1565c0;
}

.order-detail-status.COMPLETED {
  background: #e8f5e9;
  color: #2e7d32;
}

.order-detail-status.CANCELLED {
  background: #ffebee;
  color: #c62828;
}

.order-detail-addr {
  margin: 0 0 10px 0;
  font-size: 13px;
  color: #666;
}

.order-detail-items {
  margin: 0 0 10px 0;
  padding-left: 20px;
  font-size: 14px;
  color: #333;
}

.order-detail-total {
  margin: 0;
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.25s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.meituan-main {
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 52px);
}

/* 左侧分类 - 美团风格 */
.category-sidebar {
  width: 90px;
  flex-shrink: 0;
  background: #fff;
  padding: 8px 0;
}

.category-item {
  display: block;
  padding: 14px 8px;
  text-align: center;
  font-size: 13px;
  color: #333;
  background: transparent;
  border: none;
  border-left: 3px solid transparent;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s;
}

.category-item:hover {
  background: #fffbf0;
  color: #333;
}

.category-item.active {
  background: #fffbf0;
  color: #333;
  font-weight: 600;
  border-left-color: #ffc107;
}

/* 中间菜品列表 - 美团风格横向卡片 */
.menu-section {
  flex: 1;
  padding: 12px 16px;
  overflow-y: auto;
}

.loading-tip {
  text-align: center;
  padding: 40px;
  color: #999;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.menu-item {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.menu-item-img {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #f0f0f0;
}

.menu-item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.menu-item-img.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #bbb;
}

.menu-item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.menu-item-info h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.menu-desc {
  margin: 0;
  font-size: 12px;
  color: #999;
  line-height: 1.4;
  flex: 1;
}

.menu-item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.menu-price {
  font-size: 16px;
  font-weight: 600;
  color: #ff6b35;
}

.add-btn {
  padding: 6px 14px;
  font-size: 13px;
  background: #ffc107;
  color: #333;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
}

.add-btn:hover {
  background: #ffb300;
}

/* 右侧购物车 */
.cart-sidebar {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  padding: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.04);
}

.cart-header {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cart-badge {
  background: #ff6b35;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.cart-list {
  flex: 1;
  overflow-y: auto;
  max-height: 200px;
  margin-bottom: 12px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.cart-item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cart-item-name {
  font-size: 14px;
  color: #333;
}

.cart-item-price {
  font-size: 12px;
  color: #ff6b35;
}

.cart-item-qty {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-btn {
  width: 24px;
  height: 24px;
  padding: 0;
  border: 1px solid #ddd;
  background: #fff;
  color: #666;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  line-height: 1;
}

.qty-btn:hover {
  border-color: #ffc107;
  color: #ffc107;
}

.cart-total {
  padding: 12px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.total-amount {
  color: #ff6b35;
  font-size: 18px;
}

.order-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.order-form input,
.order-form textarea {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 14px;
  font: inherit;
}

.order-form input:focus,
.order-form textarea:focus {
  outline: none;
  border-color: #ffc107;
}

.cart-actions {
  display: flex;
  gap: 10px;
}

.btn-clear {
  flex: 1;
  padding: 12px;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.btn-submit {
  flex: 2;
  padding: 12px;
  background: #ffc107;
  color: #333;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.btn-submit:hover:not(:disabled) {
  background: #ffb300;
}

.btn-submit:disabled {
  background: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

/* 平板：分类横排、整体竖排（仅 901-1100px 显示右侧购物车） */
@media (max-width: 1100px) and (min-width: 901px) {
  .meituan-main {
    flex-direction: column;
  }

  .category-sidebar {
    width: 100%;
    display: flex;
    overflow-x: auto;
    padding: 8px;
    gap: 8px;
    -webkit-overflow-scrolling: touch;
  }

  .category-item {
    flex-shrink: 0;
    padding: 8px 16px;
    border-left: none;
    border-bottom: 3px solid transparent;
  }

  .category-item.active {
    border-left: none;
    border-bottom-color: #ffc107;
  }

  .cart-sidebar {
    width: 100%;
  }
}

/* 手机/小平板：左侧分类 + 右侧菜单，底部购物车图标（≤900px 均使用此布局） */
@media (max-width: 900px) {
  .meituan-layout {
    padding-bottom: env(safe-area-inset-bottom, 0);
    display: flex;
    flex-direction: column;
    height: 100vh;
    height: 100dvh; /* 移动端地址栏变化时更准确 */
  }

  .meituan-header {
    padding: 10px 12px;
    padding-top: calc(10px + env(safe-area-inset-top, 0));
    flex-shrink: 0;
  }

  .logo {
    font-size: 18px;
  }

  /* 主内容区铺满左右：抵消 body 水平 padding，分类贴左、菜单填满右侧 */
  .meituan-main {
    flex: 1;
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    min-height: 0;
    max-width: 100%;
    margin-left: calc(-1 * env(safe-area-inset-left, 0));
    margin-right: calc(-1 * env(safe-area-inset-right, 0));
    width: calc(100% + env(safe-area-inset-left, 0) + env(safe-area-inset-right, 0));
  }

  /* 左侧分类：放大，参考美团，贴最左 */
  .category-sidebar {
    width: 72px;
    flex-shrink: 0;
    display: block;
    padding: 6px 0 6px env(safe-area-inset-left, 0);
    background: #fff;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    border-right: 1px solid #eee;
  }

  .category-item {
    display: block;
    padding: 14px 4px 14px 0;
    text-align: center;
    font-size: 15px;
    font-weight: 500;
    color: #333;
    background: transparent;
    border-left: 4px solid transparent;
    border-bottom: none;
    box-sizing: border-box;
    line-height: 1.3;
  }

  .category-item.active {
    background: #fffbf0;
    color: #333;
    font-weight: 600;
    border-left-color: #ffc107;
  }

  /* 右侧菜单：大卡片，一屏约 4 个，参考美团 */
  .menu-section {
    flex: 1;
    min-width: 0;
    padding: 10px 8px;
    padding-right: calc(8px + env(safe-area-inset-right, 0));
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  .menu-list {
    gap: 14px;
  }

  .menu-item {
    padding: 14px 10px 14px 12px;
    gap: 14px;
    border-radius: 10px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }

  .menu-item-img {
    width: 100px;
    height: 100px;
    flex-shrink: 0;
    border-radius: 8px;
  }

  .menu-item-info h3 {
    font-size: 16px;
    font-weight: 600;
  }

  .menu-desc {
    font-size: 13px;
    color: #666;
    line-height: 1.4;
  }

  .menu-price {
    font-size: 16px;
    font-weight: 600;
    color: #ff6b35;
  }

  .add-btn {
    min-height: 38px;
    padding: 8px 16px;
    font-size: 14px;
    font-weight: 500;
    border-radius: 20px;
  }

}

/* 有购物车时为主内容留出底部空间（桌面/移动端统一） */
.meituan-layout.has-cart-bar {
  padding-bottom: calc(52px + env(safe-area-inset-bottom, 0));
}

/* 底部购物车栏与抽屉：全局生效（桌面端也使用底部栏 + 点击打开抽屉） */
.cart-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 52px;
  padding-bottom: env(safe-area-inset-bottom, 0);
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: stretch;
  z-index: 100;
}

.cart-bar-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  cursor: pointer;
  min-width: 0;
}

.cart-bar-icon-wrap {
  position: relative;
  flex-shrink: 0;
}

.cart-bar-icon {
  font-size: 28px;
  display: block;
}

.cart-bar-badge {
  position: absolute;
  top: -4px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
}

.cart-bar-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.cart-bar-total {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.cart-bar-hint {
  font-size: 12px;
  color: #999;
}

.cart-bar-checkout {
  width: 110px;
  flex-shrink: 0;
  background: #ffc107;
  color: #333;
  border: none;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}

.cart-bar-checkout:not(.disabled):active {
  background: #ffb300;
}

.cart-bar-checkout.disabled {
  background: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.cart-drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}

.cart-drawer {
  width: 100%;
  max-height: 75vh;
  background: #fff;
  border-radius: 16px 16px 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cart-drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.cart-drawer-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.cart-drawer-close {
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  color: #666;
}

.cart-drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom, 0));
}

.cart-empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.cart-drawer .cart-list {
  max-height: 180px;
  margin-bottom: 12px;
}

.cart-drawer .cart-total {
  padding: 12px 0;
}

.cart-drawer .order-form {
  margin-bottom: 12px;
}

.cart-drawer .order-form input,
.cart-drawer .order-form textarea {
  padding: 12px 14px;
  font-size: 16px;
}

.cart-drawer .cart-actions {
  gap: 12px;
}

.cart-drawer .btn-clear,
.cart-drawer .btn-submit {
  min-height: 48px;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.25s ease;
}

.drawer-enter-active .cart-drawer,
.drawer-leave-active .cart-drawer {
  transition: transform 0.25s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .cart-drawer,
.drawer-leave-to .cart-drawer {
  transform: translateY(100%);
}

/* 超窄屏（小屏安卓 360px 等）仍保持大卡片风格 */
@media (max-width: 360px) {
  .category-sidebar {
    width: 64px;
  }

  .category-item {
    padding: 12px 4px 12px 0;
    font-size: 14px;
  }

  .menu-item-img {
    width: 88px;
    height: 88px;
  }

  .menu-item {
    padding: 12px 8px;
    gap: 12px;
  }
}
</style>
