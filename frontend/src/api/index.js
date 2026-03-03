import http from './http'

export const getPublicCategories = () => http.get('/api/public/categories')
export const getPublicMenuItems = (categoryId) =>
  http.get('/api/public/menu-items', {
    params: categoryId ? { categoryId } : undefined,
  })
export const getOrderByIdAndPhone = (orderId, phone) =>
  http.get(`/api/public/orders/${orderId}`, { params: { phone } })

export const customerRegister = (payload) => http.post('/api/public/register', payload)
export const customerLogin = (payload) => http.post('/api/public/auth/login', payload)

export const getCustomerMe = () => http.get('/api/customer/me')
export const getCustomerAddresses = () => http.get('/api/customer/addresses')
export const createCustomerAddress = (payload) => http.post('/api/customer/addresses', payload)
export const updateCustomerAddress = (id, payload) => http.put(`/api/customer/addresses/${id}`, payload)
export const deleteCustomerAddress = (id) => http.delete(`/api/customer/addresses/${id}`)
export const setDefaultAddress = (id) => http.patch(`/api/customer/addresses/${id}/default`)

/** 下单（需登录） */
export const createCustomerOrder = (payload) => http.post('/api/customer/orders', payload)

export const getAdminCustomers = (params) =>
  http.get('/api/admin/customers', { params: params || {} })
export const updateAdminCustomerStatus = (id, payload) =>
  http.patch(`/api/admin/customers/${id}/status`, payload)

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
export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/api/admin/upload/image', formData, {
    timeout: 30000,
  })
}
export const createAdminMenuItem = (payload) =>
  http.post('/api/admin/menu-items', payload)
export const updateAdminMenuItem = (id, payload) =>
  http.put(`/api/admin/menu-items/${id}`, payload)
export const deleteAdminMenuItem = (id) =>
  http.delete(`/api/admin/menu-items/${id}`)

export const getAdminOrders = (params) =>
  http.get('/api/admin/orders', { params: params || {} })
export const updateOrderStatus = (id, payload) =>
  http.put(`/api/admin/orders/${id}/status`, payload)
