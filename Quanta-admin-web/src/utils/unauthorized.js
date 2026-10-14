let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === 'function' ? handler : null
}

export async function notifyUnauthorized(error) {
  if (unauthorizedHandler) {
    await unauthorizedHandler(error)
  }
}

