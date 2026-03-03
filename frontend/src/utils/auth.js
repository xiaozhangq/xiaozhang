const ADMIN_TOKEN_KEY = 'admin_access_token'
const CUSTOMER_TOKEN_KEY = 'customer_access_token'

export function getAdminToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

export function setAdminToken(token) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
}

export function clearAdminToken() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
}

export function hasAdminToken() {
  return Boolean(getAdminToken())
}

export function getCustomerToken() {
  return localStorage.getItem(CUSTOMER_TOKEN_KEY) || ''
}

export function setCustomerToken(token) {
  localStorage.setItem(CUSTOMER_TOKEN_KEY, token)
}

export function clearCustomerToken() {
  localStorage.removeItem(CUSTOMER_TOKEN_KEY)
}

export function hasCustomerToken() {
  return Boolean(getCustomerToken())
}
