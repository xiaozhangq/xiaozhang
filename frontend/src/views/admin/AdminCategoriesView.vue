<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  createAdminCategory,
  deleteAdminCategory,
  getAdminCategories,
  updateAdminCategory,
} from '../../api'

const loading = ref(false)
const categories = ref([])
const categoryForm = reactive({
  id: null,
  name: '',
  sortOrder: 0,
  active: true,
})

onMounted(() => loadCategories())

async function loadCategories() {
  loading.value = true
  try {
    const { data } = await getAdminCategories()
    categories.value = data
  } catch (error) {
    if (error.status === 401 || error.status === 403) return
    alert(error.message)
  } finally {
    loading.value = false
  }
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
    resetCategoryForm()
  } catch (error) {
    alert(error.message)
  }
}

async function removeCategory(id) {
  if (!confirm('确认删除该分类吗？')) return
  try {
    await deleteAdminCategory(id)
    await loadCategories()
  } catch (error) {
    alert(error.message)
  }
}
</script>

<template>
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
    <p v-if="loading">加载中...</p>
    <table v-else>
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
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.form-grid input {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
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

@media (max-width: 1000px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
