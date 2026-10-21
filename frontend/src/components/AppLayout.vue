<template>
  <el-container class="app-shell">
    <el-aside width="220px" class="app-aside">
      <AppMenu />
    </el-aside>
    <el-container>
      <AppHeader />
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import AppMenu from './AppMenu.vue'
import AppHeader from './AppHeader.vue'
</script>

<style scoped>
.app-shell {
  height: 100vh;
}
.app-aside {
  background: #fff;
  border-right: 1px solid #ebedf0;
}
.app-main {
  padding: 20px 24px;
  background: #f5f7fa;
}
@media (max-width: 768px) {
  .app-aside {
    display: none;
  }
}
</style>
