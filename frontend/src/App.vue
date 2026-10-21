<template>
  <router-view />
</template>

<script setup>
// App is a thin shell; the authenticated layout lives in AppLayout rendered
// by each non-public view's parent. Kept minimal on purpose.
</script>

<style>
:root {
  --sems-primary: #2f6fed;
}
html,
body,
#app {
  margin: 0;
  height: 100%;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  background: #f5f7fa;
  color: #1f2329;
}
</style>
