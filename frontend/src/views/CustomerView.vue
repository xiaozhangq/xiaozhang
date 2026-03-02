<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createOrder, getPublicCategories, getPublicMenuItems } from '../api'

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
function closeCartDrawer() {
  cartDrawerOpen.value = false
}

const imageBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
function getImageUrl(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : imageBaseUrl + url
}

onMounted(async () => {
  document.title = '点餐 - 前台'
  await loadCategories()
  await loadMenuItems(activeCategoryId.value)
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
    const { data } = await createOrder(payload)
    alert(`下单成功，订单号：${data.orderId}`)
    clearCart()
    orderForm.remark = ''
    closeCartDrawer()
  } catch (error) {
    alert(error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="meituan-layout">
    <header class="meituan-header">
      <h1 class="logo">点餐</h1>
    </header>

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

      <!-- 桌面端：右侧购物车 -->
      <aside class="cart-sidebar cart-sidebar-desktop">
        <div class="cart-header">
          <span>购物车</span>
          <span class="cart-badge" v-if="cartCount">{{ cartCount }}</span>
        </div>
        <div class="cart-list">
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

        <div class="order-form">
          <input v-model="orderForm.customerName" placeholder="联系人" />
          <input v-model="orderForm.customerPhone" placeholder="联系电话" />
          <input v-model="orderForm.deliveryAddress" placeholder="配送地址" />
          <textarea v-model="orderForm.remark" rows="2" placeholder="备注（可选）" />
        </div>

        <div class="cart-actions">
          <button class="btn-clear" @click="clearCart">清空</button>
          <button class="btn-submit" :disabled="submitting || cartCount === 0" @click="submitOrder">
            {{ submitting ? '提交中...' : '去结算' }}
          </button>
        </div>
      </aside>

      <!-- 移动端：底部购物车栏（参考美团外卖） -->
      <div class="cart-bar">
        <div class="cart-bar-left" @click="toggleCartDrawer">
          <span class="cart-bar-icon-wrap">
            <span class="cart-bar-icon">🛒</span>
            <span v-if="cartCount" class="cart-bar-badge">{{ cartCount }}</span>
          </span>
          <div class="cart-bar-info">
            <span class="cart-bar-total">￥{{ cartTotal.toFixed(2) }}</span>
            <span class="cart-bar-hint">{{ cartCount ? '共' + cartCount + '件' : '购物车是空的' }}</span>
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

              <div class="order-form">
                <input v-model="orderForm.customerName" placeholder="联系人" />
                <input v-model="orderForm.customerPhone" placeholder="联系电话" />
                <input v-model="orderForm.deliveryAddress" placeholder="配送地址" />
                <textarea v-model="orderForm.remark" rows="2" placeholder="备注（可选）" />
              </div>

              <div class="cart-actions">
                <button class="btn-clear" @click="clearCart">清空</button>
                <button class="btn-submit" :disabled="submitting || cartCount === 0" @click="submitOrder">
                  {{ submitting ? '提交中...' : '去结算' }}
                </button>
              </div>
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
  background: #fff;
  padding: 12px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.logo {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
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

  .meituan-main {
    flex: 1;
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    min-height: 0;
    max-width: 100%;
  }

  /* 左侧分类：保持纵向，深色文字 */
  .category-sidebar {
    width: 72px;
    flex-shrink: 0;
    display: block;
    padding: 6px 0;
    background: #fff;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    border-right: 1px solid #eee;
  }

  .category-item {
    display: block;
    padding: 12px 6px;
    text-align: center;
    font-size: 13px;
    color: #333;
    background: transparent;
    border-left: 3px solid transparent;
    border-bottom: none;
  }

  .category-item.active {
    background: #fffbf0;
    color: #333;
    font-weight: 600;
    border-left-color: #ffc107;
  }

  /* 右侧菜单：占满剩余宽度 */
  .menu-section {
    flex: 1;
    min-width: 0;
    padding: 8px 10px;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  .menu-item {
    padding: 10px;
    gap: 10px;
  }

  .menu-item-img {
    width: 72px;
    height: 72px;
    flex-shrink: 0;
  }

  .menu-item-info h3 {
    font-size: 14px;
  }

  .menu-desc {
    font-size: 12px;
  }

  .menu-price {
    font-size: 14px;
  }

  .add-btn {
    min-height: 32px;
    padding: 6px 12px;
    font-size: 12px;
  }

  /* 移动端隐藏右侧购物车，改用底部图标 */
  .cart-sidebar-desktop {
    display: none !important;
  }

  /* 底部购物车栏（参考美团外卖） */
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

  /* 购物车抽屉 */
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
}

/* 桌面端隐藏底部购物车栏和抽屉 */
.cart-bar {
  display: none;
}

@media (min-width: 901px) {
  .cart-bar,
  .cart-drawer-overlay {
    display: none !important;
  }
}

@media (max-width: 900px) {
  .cart-bar {
    display: flex;
  }

  .meituan-layout {
    padding-bottom: calc(52px + env(safe-area-inset-bottom, 0)); /* 为底部购物车栏留空 */
  }
}

/* 超窄屏（小屏安卓 360px 等） */
@media (max-width: 360px) {
  .category-sidebar {
    width: 64px;
  }

  .menu-item-img {
    width: 64px;
    height: 64px;
  }

  .category-item {
    padding: 10px 4px;
    font-size: 12px;
  }
}
</style>
