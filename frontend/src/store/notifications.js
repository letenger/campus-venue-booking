import { computed, reactive } from 'vue'
import {
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
} from '../api'

/**
 * One copy of the inbox for the whole shell. The bell in the header and the
 * notifications page read the same array, so marking something read updates the
 * badge without a reload.
 */
export const notificationState = reactive({
  items: [],
  loading: false,
})

export const unreadCount = computed(() => notificationState.items.filter((n) => !n.read).length)

export async function loadNotifications() {
  if (notificationState.loading) return notificationState.items
  notificationState.loading = true
  try {
    notificationState.items = (await listNotifications()) || []
    return notificationState.items
  } finally {
    notificationState.loading = false
  }
}

export async function readNotification(id) {
  const item = notificationState.items.find((n) => n.id === id)
  if (!item || item.read) return
  await markNotificationRead(id)
  item.read = true
}

export async function readAllNotifications() {
  await markAllNotificationsRead()
  notificationState.items.forEach((n) => {
    n.read = true
  })
}

/** Called when the signed-in user changes, so one account never sees another's inbox. */
export function resetNotifications() {
  notificationState.items = []
  notificationState.loading = false
}
