import http from './http'

export const getPublicCategories = () => http.get('/api/public/categories')
export const getPublicMenuItems = (categoryId) =>
  http.get('/api/public/menu-items', {
    params: categoryId ? { categoryId } : undefined,
  })
export const createOrder = (payload) => http.post('/api/public/orders', payload)

export const adminLogin = (payload) => http.post('/api/admin/auth/login', payload)
export const getAdminProfile = () => http.get('/api/admin/auth/me')

export const getAdminCategories = () => http.get('/api/admin/categories')
export const createAdminCategory = (payload) =>
  http.post('/api/admin/categories', payload)
export const updateAdminCategory = (id, payload) =>
  http.put(`/api/admin/categories/${id}`, payload)
export const deleteAdminCategory = (id) =>
  http.delete(`/api/admin/categories/${id}`)

export const getAdminMenuItems = () => http.get('/api/admin/menu-items')
export const createAdminMenuItem = (payload) =>
  http.post('/api/admin/menu-items', payload)
export const updateAdminMenuItem = (id, payload) =>
  http.put(`/api/admin/menu-items/${id}`, payload)
export const deleteAdminMenuItem = (id) =>
  http.delete(`/api/admin/menu-items/${id}`)

export const getAdminOrders = () => http.get('/api/admin/orders')
export const updateOrderStatus = (id, payload) =>
  http.put(`/api/admin/orders/${id}/status`, payload)
