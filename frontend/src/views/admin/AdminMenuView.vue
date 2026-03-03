<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  createAdminMenuItem,
  deleteAdminMenuItem,
  getAdminCategories,
  getAdminMenuItems,
  updateAdminMenuItem,
  uploadImage,
} from '../../api'

const loading = ref(false)
const categories = ref([])
const menuItems = ref([])
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

onMounted(async () => {
  await loadCategories()
  await loadMenuItems()
})

async function loadCategories() {
  try {
    const { data } = await getAdminCategories()
    categories.value = data
  } catch (error) {
    if (error.status === 401 || error.status === 403) return
    alert(error.message)
  }
}

async function loadMenuItems() {
  loading.value = true
  try {
    const { data } = await getAdminMenuItems()
    menuItems.value = data
  } catch (error) {
    if (error.status === 401 || error.status === 403) return
    alert(error.message)
  } finally {
    loading.value = false
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
  if (!confirm('确认删除该菜品吗？')) return
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

function formatMoney(value) {
  return Number(value).toFixed(2)
}
</script>

<template>
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

    <p v-if="loading">加载中...</p>
    <table v-else>
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

.image-upload-inputs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  flex: 1;
}

.form-img-preview {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f4f6;
}

.form-img-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-grid input,
.form-grid select {
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

.img-cell .no-img {
  color: #9ca3af;
  font-size: 12px;
}

.menu-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
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
  .menu-form {
    grid-template-columns: 1fr;
  }

  .image-upload-cell {
    grid-column: 1;
  }
}
</style>
