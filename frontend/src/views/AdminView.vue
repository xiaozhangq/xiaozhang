<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createAdminCategory,
  createAdminMenuItem,
  deleteAdminCategory,
  deleteAdminMenuItem,
  getAdminProfile,
  getAdminCategories,
  getAdminMenuItems,
  getAdminOrders,
  updateAdminCategory,
  updateAdminMenuItem,
  updateOrderStatus,
  uploadImage,
} from '../api'
import { clearAdminToken } from '../utils/auth'

const loading = ref(false)
const adminName = ref('')
const categories = ref([])
const menuItems = ref([])
const orders = ref([])
const router = useRouter()
const lastOrderIds = ref(new Set())
let orderPollTimer = null
const newOrderToast = ref(false)

const categoryForm = reactive({
  id: null,
  name: '',
  sortOrder: 0,
  active: true,
})

const menuForm = reactive({
  id: null,
  categoryId: '',
  name: '',
  description: '',
  price: '',
  imageUrl: '',
  available: true,
})
const imageUploading = ref(false)
const imageBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

const statusOptions = [
  { value: 'PENDING', label: '待接单' },
  { value: 'ACCEPTED', label: '已接单' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

function playNewOrderSound() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.frequency.value = 880
    osc.type = 'sine'
    gain.gain.setValueAtTime(0.3, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.15)
    osc.start(ctx.currentTime)
    osc.stop(ctx.currentTime + 0.15)
    setTimeout(() => {
      osc.frequency.value = 1100
      const osc2 = ctx.createOscillator()
      const g2 = ctx.createGain()
      osc2.connect(g2)
      g2.connect(ctx.destination)
      osc2.frequency.value = 1100
      osc2.type = 'sine'
      g2.gain.setValueAtTime(0.25, ctx.currentTime)
      g2.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.2)
      osc2.start(ctx.currentTime)
      osc2.stop(ctx.currentTime + 0.2)
    }, 150)
  } catch (_) {}
}

function showNewOrderNotification() {
  newOrderToast.value = true
  playNewOrderSound()
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('新订单提醒', { body: '您有新的订单，请及时处理！' })
  }
  setTimeout(() => {
    newOrderToast.value = false
  }, 4000)
}

async function pollOrders() {
  try {
    const { data } = await getAdminOrders()
    const currentIds = new Set(data.map((o) => o.id))
    const hadOrders = lastOrderIds.value.size > 0
    const hasNew = [...currentIds].some((id) => !lastOrderIds.value.has(id))
    if (hadOrders && hasNew) {
      const newOrders = data.filter((o) => !lastOrderIds.value.has(o.id))
      if (newOrders.some((o) => o.status === 'PENDING')) {
        showNewOrderNotification()
      }
    }
    lastOrderIds.value = currentIds
    orders.value = data
  } catch (_) {}
}

onMounted(async () => {
  document.title = '后台管理'
  await loadProfile()
  await refreshAll()
  lastOrderIds.value = new Set(orders.value.map((o) => o.id))
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
  orderPollTimer = setInterval(pollOrders, 5000)
})

onUnmounted(() => {
  if (orderPollTimer) clearInterval(orderPollTimer)
})

async function loadProfile() {
  try {
    const { data } = await getAdminProfile()
    adminName.value = data.username
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      logout(false)
      return
    }
    alert(error.message)
  }
}

async function refreshAll() {
  loading.value = true
  try {
    await Promise.all([loadCategories(), loadMenuItems(), loadOrders()])
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      alert('登录已过期，请重新登录')
      logout(false)
      return
    }
    alert(error.message)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const { data } = await getAdminCategories()
  categories.value = data
}

async function loadMenuItems() {
  const { data } = await getAdminMenuItems()
  menuItems.value = data
}

async function loadOrders() {
  const { data } = await getAdminOrders()
  orders.value = data
}

function editCategory(category) {
  categoryForm.id = category.id
  categoryForm.name = category.name
  categoryForm.sortOrder = category.sortOrder
  categoryForm.active = category.active
}

function resetCategoryForm() {
  categoryForm.id = null
  categoryForm.name = ''
  categoryForm.sortOrder = 0
  categoryForm.active = true
}

async function submitCategory() {
  try {
    const payload = {
      name: categoryForm.name,
      sortOrder: Number(categoryForm.sortOrder),
      active: Boolean(categoryForm.active),
    }
    if (categoryForm.id) {
      await updateAdminCategory(categoryForm.id, payload)
    } else {
      await createAdminCategory(payload)
    }
    await loadCategories()
    await loadMenuItems()
    resetCategoryForm()
  } catch (error) {
    alert(error.message)
  }
}

async function removeCategory(id) {
  if (!confirm('确认删除该分类吗？')) {
    return
  }
  try {
    await deleteAdminCategory(id)
    await loadCategories()
  } catch (error) {
    alert(error.message)
  }
}

function editMenuItem(item) {
  menuForm.id = item.id
  menuForm.categoryId = item.categoryId
  menuForm.name = item.name
  menuForm.description = item.description || ''
  menuForm.price = item.price
  menuForm.imageUrl = item.imageUrl || ''
  menuForm.available = item.available
}

function resetMenuForm() {
  menuForm.id = null
  menuForm.categoryId = ''
  menuForm.name = ''
  menuForm.description = ''
  menuForm.price = ''
  menuForm.imageUrl = ''
  menuForm.available = true
}

async function submitMenuItem() {
  if (!menuForm.categoryId) {
    alert('请选择分类')
    return
  }
  try {
    const payload = {
      categoryId: Number(menuForm.categoryId),
      name: menuForm.name,
      description: menuForm.description,
      price: Number(menuForm.price),
      imageUrl: menuForm.imageUrl,
      available: Boolean(menuForm.available),
    }
    if (menuForm.id) {
      await updateAdminMenuItem(menuForm.id, payload)
    } else {
      await createAdminMenuItem(payload)
    }
    await loadMenuItems()
    resetMenuForm()
  } catch (error) {
    alert(error.message)
  }
}

async function removeMenuItem(id) {
  if (!confirm('确认删除该菜品吗？')) {
    return
  }
  try {
    await deleteAdminMenuItem(id)
    await loadMenuItems()
  } catch (error) {
    alert(error.message)
  }
}

async function onImageSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }
  imageUploading.value = true
  try {
    const { data } = await uploadImage(file)
    menuForm.imageUrl = data.url
  } catch (error) {
    alert(error.message || '上传失败')
  } finally {
    imageUploading.value = false
    e.target.value = ''
  }
}

function getImageUrl(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : imageBaseUrl + url
}

async function onChangeOrderStatus(order, nextStatus) {
  try {
    const { data } = await updateOrderStatus(order.id, { status: nextStatus })
    order.status = data.status
  } catch (error) {
    alert(error.message)
  }
}

function getStatusLabel(status) {
  return statusOptions.find((item) => item.value === status)?.label || status
}

function formatMoney(value) {
  return Number(value).toFixed(2)
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ')
}

async function logout(showTip = true) {
  clearAdminToken()
  if (showTip) {
    alert('已退出登录')
  }
  await router.replace('/admin/login')
}
</script>

<template>
  <div class="admin-page">
    <Transition name="toast">
      <div v-if="newOrderToast" class="new-order-toast">
        🔔 您有新的订单，请及时处理！
      </div>
    </Transition>
    <section class="panel admin-toolbar">
      <div>
        <strong>当前管理员：</strong>{{ adminName || '-' }}
      </div>
      <button class="btn-light" @click="logout()">退出登录</button>
    </section>

    <p v-if="loading">数据加载中...</p>

    <section class="panel">
      <h2>分类管理</h2>
      <div class="form-grid">
        <input v-model="categoryForm.name" placeholder="分类名称" />
        <input v-model="categoryForm.sortOrder" type="number" placeholder="排序" />
        <label class="switch">
          <input v-model="categoryForm.active" type="checkbox" />
          启用
        </label>
        <button @click="submitCategory">
          {{ categoryForm.id ? '更新分类' : '新增分类' }}
        </button>
        <button class="btn-light" @click="resetCategoryForm">重置</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>排序</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in categories" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.name }}</td>
            <td>{{ item.sortOrder }}</td>
            <td>{{ item.active ? '启用' : '停用' }}</td>
            <td class="actions">
              <button @click="editCategory(item)">编辑</button>
              <button class="btn-danger" @click="removeCategory(item.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel">
      <h2>菜单管理</h2>
      <div class="form-grid menu-form">
        <select v-model="menuForm.categoryId">
          <option value="">选择分类</option>
          <option v-for="item in categories" :key="item.id" :value="item.id">
            {{ item.name }}
          </option>
        </select>
        <input v-model="menuForm.name" placeholder="菜品名称" />
        <input v-model="menuForm.price" type="number" step="0.01" placeholder="价格" />
        <div class="image-upload-cell">
          <div v-if="menuForm.imageUrl" class="form-img-preview">
            <img :src="getImageUrl(menuForm.imageUrl)" alt="预览" />
          </div>
          <div class="image-upload-inputs">
            <input
              type="file"
              accept="image/*"
              :disabled="imageUploading"
              @change="onImageSelect"
            />
            <input v-model="menuForm.imageUrl" placeholder="或粘贴图片链接" />
            <span v-if="imageUploading">上传中...</span>
          </div>
        </div>
        <input v-model="menuForm.description" placeholder="描述（可选）" />
        <label class="switch">
          <input v-model="menuForm.available" type="checkbox" />
          上架
        </label>
        <button @click="submitMenuItem">
          {{ menuForm.id ? '更新菜品' : '新增菜品' }}
        </button>
        <button class="btn-light" @click="resetMenuForm">重置</button>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>图片</th>
            <th>分类</th>
            <th>名称</th>
            <th>价格</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in menuItems" :key="item.id">
            <td>{{ item.id }}</td>
            <td class="img-cell">
              <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" class="menu-thumb" :alt="item.name" />
              <span v-else class="no-img">暂无</span>
            </td>
            <td>{{ item.categoryName }}</td>
            <td>{{ item.name }}</td>
            <td>￥{{ formatMoney(item.price) }}</td>
            <td>{{ item.available ? '上架' : '下架' }}</td>
            <td class="actions">
              <button @click="editMenuItem(item)">编辑</button>
              <button class="btn-danger" @click="removeMenuItem(item.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel">
      <div class="order-title">
        <h2>订单管理</h2>
        <button class="btn-light" @click="loadOrders">刷新订单</button>
      </div>
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-head">
          <div>
            <strong>订单 #{{ order.id }}</strong>
            <span>（{{ formatDate(order.createdAt) }}）</span>
          </div>
          <div class="status-box">
            <span class="status">{{ getStatusLabel(order.status) }}</span>
            <select
              :value="order.status"
              @change="onChangeOrderStatus(order, $event.target.value)"
            >
              <option v-for="status in statusOptions" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
          </div>
        </div>

        <p>
          {{ order.customerName }} / {{ order.customerPhone }} / {{ order.deliveryAddress }}
        </p>
        <p>备注：{{ order.remark || '-' }}</p>
        <ul>
          <li v-for="item in order.items" :key="item.id">
            {{ item.menuItemName }} x {{ item.quantity }} = ￥{{ formatMoney(item.subtotal) }}
          </li>
        </ul>
        <p class="order-total">总计：￥{{ formatMoney(order.totalAmount) }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
}

.admin-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.menu-form {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.image-upload-cell {
  grid-column: span 2;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
}

.image-upload-cell input[type='file'] {
  flex: 0 0 auto;
}

.image-upload-cell input[type='text'] {
  flex: 1;
  min-width: 120px;
}

.img-cell {
  padding: 6px;
}

.img-cell .no-img {
  color: #9ca3af;
  font-size: 12px;
}

.menu-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
  display: block;
}

.form-img-preview {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f4f6;
  flex-shrink: 0;
}

.form-img-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-upload-inputs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  flex: 1;
}

.form-grid input,
.form-grid select {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  font: inherit;
}

.switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  text-align: left;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-light {
  background: #f3f4f6;
  color: #111827;
}

.btn-danger {
  background: #dc2626;
  color: #fff;
}

.order-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-top: 12px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.status-box {
  display: flex;
  gap: 8px;
  align-items: center;
}

.status {
  display: inline-block;
  min-width: 56px;
}

.order-total {
  font-weight: 700;
}

.new-order-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: #059669;
  color: #fff;
  padding: 16px 24px;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 9999;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px);
}

@media (max-width: 1000px) {
  .form-grid,
  .menu-form {
    grid-template-columns: 1fr;
  }
}
</style>
