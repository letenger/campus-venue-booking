<template>
  <div class="shell">
    <aside class="sidebar" :class="{ 'sidebar-open': mobileOpen }">
      <AppMenu @navigate="mobileOpen = false" />
    </aside>

    <div v-if="mobileOpen" class="scrim" @click="mobileOpen = false" />

    <div class="main">
      <AppHeader @open-menu="mobileOpen = true" />
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AppMenu from './AppMenu.vue'
import AppHeader from './AppHeader.vue'

const mobileOpen = ref(false)
</script>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  height: 100vh;
  background: var(--bg);
}

.sidebar {
  background: var(--surface);
  border-right: 1px solid var(--line);
  overflow-y: auto;
}

.main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 22px 30px 48px;
}

.scrim {
  display: none;
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: 264px;
    z-index: 30;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
    box-shadow: var(--shadow);
  }

  .sidebar-open {
    transform: none;
  }

  .scrim {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(20, 24, 31, 0.32);
    z-index: 20;
  }

  .content {
    padding: 16px 16px 40px;
  }
}
</style>
