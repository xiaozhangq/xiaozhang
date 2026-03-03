<script setup>
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { onMounted, onUnmounted, ref } from 'vue'
import { getAdminOrders, updateOrderStatus } from '../../api'

const loading = ref(false)
const orders = ref([])
const filterStatus = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')
const newOrderToast = ref(false)
let stompClient = null

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待接单' },
  { value: 'ACCEPTED', label: '已接单' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

function todayStr() {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

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

function buildParams() {
  const p = {}
  if (filterStatus.value) p.status = filterStatus.value
  if (filterStartDate.value) p.startDate = filterStartDate.value
  if (filterEndDate.value) p.endDate = filterEndDate.value
  return p
}

function openDatePicker(e) {
  const el = e?.currentTarget
  if (el?.showPicker) el.showPicker()
}

async function loadOrders() {
  loading.value = true
  try {
    const { data } = await getAdminOrders(buildParams())
    orders.value = data
  } catch (error) {
    if (error.status === 401 || error.status === 403) return
    alert(error.message)
  } finally {
    loading.value = false
  }
}

function connectWebSocket() {
  const base = window.location.origin
  const sock = new SockJS(base + '/ws/orders')
  stompClient = new Client({
    webSocketFactory: () => sock,
    onConnect: () => {
      stompClient.subscribe('/topic/orders', (msg) => {
        const body = JSON.parse(msg.body)
        if (body.type === 'NEW') {
          const order = body.order
          const idx = orders.value.findIndex((o) => o.id === order.id)
          if (idx === -1) {
            orders.value = [order, ...orders.value]
            showNewOrderNotification()
          }
        } else if (body.type === 'STATUS') {
          const order = body.order
          const idx = orders.value.findIndex((o) => o.id === order.id)
          if (idx !== -1) orders.value[idx] = { ...orders.value[idx], ...order }
        }
      })
    },
  })
  stompClient.activate()
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
  if (!value) return '-'
  return value.replace('T', ' ')
}

onMounted(() => {
  filterStartDate.value = todayStr()
  filterEndDate.value = todayStr()
  loadOrders()
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
  connectWebSocket()
})

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>

<template>
  <div class="orders-page">
    <Transition name="toast">
      <div v-if="newOrderToast" class="new-order-toast">
        🔔 您有新的订单，请及时处理！
      </div>
    </Transition>

    <section class="panel">
      <div class="order-header">
        <h2>订单管理</h2>
        <button class="btn-light" @click="loadOrders">刷新</button>
      </div>

      <div class="filters">
        <select v-model="filterStatus">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
        <input
          v-model="filterStartDate"
          type="date"
          class="filter-date"
          @click="openDatePicker"
        />
        <span>至</span>
        <input
          v-model="filterEndDate"
          type="date"
          class="filter-date"
          @click="openDatePicker"
        />
        <button @click="loadOrders">查询</button>
      </div>

      <p v-if="loading">加载中...</p>
      <div v-else class="order-list">
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
                <option v-for="s in statusOptions.filter((o) => o.value)" :key="s.value" :value="s.value">
                  {{ s.label }}
                </option>
              </select>
            </div>
          </div>
          <p>{{ order.customerName }} / {{ order.customerPhone }} / {{ order.deliveryAddress }}</p>
          <p>备注：{{ order.remark || '-' }}</p>
          <ul>
            <li v-for="item in order.items" :key="item.id">
              {{ item.menuItemName }} x {{ item.quantity }} = ￥{{ formatMoney(item.subtotal) }}
            </li>
          </ul>
          <p class="order-total">总计：￥{{ formatMoney(order.totalAmount) }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.orders-page {
  position: relative;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.filters select,
.filters input {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
}

.filter-date {
  cursor: pointer;
  min-width: 140px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
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
  min-width: 56px;
}

.order-total {
  font-weight: 700;
}

.btn-light {
  background: #f3f4f6;
  color: #111827;
}

/* 右下角弹窗 */
.new-order-toast {
  position: fixed;
  right: 20px;
  bottom: 80px;
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
  transform: translateX(20px);
}
</style>
