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

onMounted(async () => {
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
  } catch (error) {
    alert(error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="customer-layout">
    <aside class="category-panel">
      <h2>菜单分类</h2>
      <button
        v-for="category in categories"
        :key="category.id"
        class="category-btn"
        :class="{ active: category.id === activeCategoryId }"
        @click="selectCategory(category.id)"
      >
        {{ category.name }}
      </button>
    </aside>

    <section class="menu-panel">
      <div class="panel-title">
        <h2>菜品列表</h2>
        <span v-if="loading">加载中...</span>
      </div>
      <div class="menu-grid">
        <article v-for="item in menuItems" :key="item.id" class="menu-card">
          <h3>{{ item.name }}</h3>
          <p class="desc">{{ item.description || '暂无描述' }}</p>
          <p class="price">￥{{ Number(item.price).toFixed(2) }}</p>
          <button @click="addToCart(item)">加入购物车</button>
        </article>
      </div>
    </section>

    <aside class="cart-panel">
      <h2>购物车（{{ cartCount }}）</h2>
      <div class="cart-list">
        <div v-for="item in cart" :key="item.id" class="cart-item">
          <div>
            <div class="name">{{ item.name }}</div>
            <div class="price">￥{{ Number(item.price).toFixed(2) }}</div>
          </div>
          <div class="qty-box">
            <button @click="changeQuantity(item, -1)">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="changeQuantity(item, 1)">+</button>
          </div>
        </div>
      </div>

      <p class="total">合计：￥{{ cartTotal.toFixed(2) }}</p>

      <div class="order-form">
        <input v-model="orderForm.customerName" placeholder="联系人" />
        <input v-model="orderForm.customerPhone" placeholder="联系电话" />
        <input v-model="orderForm.deliveryAddress" placeholder="配送地址" />
        <textarea v-model="orderForm.remark" rows="2" placeholder="备注（可选）" />
      </div>

      <div class="cart-actions">
        <button class="btn-light" @click="clearCart">清空</button>
        <button class="btn-primary" :disabled="submitting" @click="submitOrder">
          {{ submitting ? '提交中...' : '提交订单' }}
        </button>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.customer-layout {
  display: grid;
  grid-template-columns: 200px 1fr 320px;
  gap: 16px;
}

.category-panel,
.menu-panel,
.cart-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
}

.category-btn {
  width: 100%;
  text-align: left;
  margin-bottom: 8px;
  border: 1px solid #d1d5db;
  background: #f9fafb;
}

.category-btn.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.menu-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-card h3 {
  margin: 0;
  font-size: 16px;
}

.desc {
  margin: 0;
  color: #6b7280;
  min-height: 40px;
}

.price {
  margin: 0;
  color: #dc2626;
  font-weight: 700;
}

.cart-list {
  max-height: 300px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
}

.qty-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-box button {
  width: 28px;
  height: 28px;
  padding: 0;
}

.total {
  font-size: 18px;
  font-weight: 700;
}

.order-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-form input,
.order-form textarea {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  font: inherit;
}

.cart-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.btn-light {
  background: #f3f4f6;
  color: #111827;
}

.btn-primary {
  background: #2563eb;
  color: #fff;
}

@media (max-width: 1100px) {
  .customer-layout {
    grid-template-columns: 1fr;
  }
}
</style>
