<script setup>
import { onMounted, ref } from 'vue'
import { getAdminCustomers, updateAdminCustomerStatus } from '../../api'

const loading = ref(false)
const customers = ref([])
const filterStatus = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'PENDING_APPROVAL', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
]

const statusLabels = {
  PENDING_APPROVAL: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
}

async function loadCustomers() {
  loading.value = true
  try {
    const params = filterStatus.value ? { status: filterStatus.value } : {}
    const { data } = await getAdminCustomers(params)
    customers.value = data
  } catch (e) {
    if (e.status !== 401 && e.status !== 403) alert(e.message)
  } finally {
    loading.value = false
  }
}

async function setStatus(customer, status) {
  try {
    await updateAdminCustomerStatus(customer.id, { status })
    customer.status = status
  } catch (e) {
    alert(e.message)
  }
}

function formatDate(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

onMounted(() => {
  loadCustomers()
})
</script>

<template>
  <section class="customers-page">
    <div class="panel">
      <div class="panel-header">
        <h2>用户审核</h2>
        <button class="btn-light" @click="loadCustomers">刷新</button>
      </div>
      <div class="filters">
        <select v-model="filterStatus" @change="loadCustomers">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>
      <p v-if="loading">加载中...</p>
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>手机号</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in customers" :key="c.id">
              <td>{{ c.id }}</td>
              <td>{{ c.username }}</td>
              <td>{{ c.phone || '-' }}</td>
              <td>{{ statusLabels[c.status] || c.status }}</td>
              <td>
                <template v-if="c.status === 'PENDING_APPROVAL'">
                  <button type="button" class="btn-approve" @click="setStatus(c, 'APPROVED')">通过</button>
                  <button type="button" class="btn-reject" @click="setStatus(c, 'REJECTED')">拒绝</button>
                </template>
                <span v-else class="muted">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!loading && customers.length === 0" class="empty">暂无用户</p>
    </div>
  </section>
</template>

<style scoped>
.customers-page {
  position: relative;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 20px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.filters {
  margin-bottom: 12px;
}

.filters select {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
}

.btn-light {
  padding: 8px 16px;
  background: #f1f5f9;
  border: 1px solid #94a3b8;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  text-align: left;
}

.data-table th {
  background: #f8fafc;
  font-weight: 600;
  color: #334155;
}

.btn-approve {
  padding: 4px 10px;
  margin-right: 8px;
  font-size: 13px;
  background: #22c55e;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.btn-reject {
  padding: 4px 10px;
  font-size: 13px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.muted {
  color: #94a3b8;
}

.empty {
  margin: 16px 0 0 0;
  color: #94a3b8;
  font-size: 14px;
}
</style>
