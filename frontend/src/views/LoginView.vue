<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-head">
        <span class="login-title">Campus Venue Booking</span>
      </div>
      <p class="login-sub">Sign in to book a venue, approve requests and read reports</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="Username" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" :type="passwordVisible ? 'text' : 'password'" placeholder="Password"
            :prefix-icon="Lock" @keyup.enter="onSubmit">
            <template #suffix>
              <el-icon class="password-toggle" @click="passwordVisible = !passwordVisible">
                <View v-if="!passwordVisible" />
                <Hide v-if="passwordVisible" />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-button" :loading="loading" @click="onSubmit">Sign in</el-button>
        </el-form-item>
      </el-form>
      <el-collapse class="demo-box">
        <el-collapse-item title="Demo accounts">
          <div class="demo-row"><span>Admin</span><code>admin / admin123</code></div>
          <div class="demo-row"><span>Teacher</span><code>teacher / teacher123</code></div>
          <div class="demo-row"><span>Student</span><code>student / student123</code></div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide } from '@element-plus/icons-vue'
import { authLogin } from '../api'
import { setAuth, homeForRole } from '../store/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const passwordVisible = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: 'Enter your username', trigger: 'blur' }],
  password: [{ required: true, message: 'Enter your password', trigger: 'blur' }],
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authLogin(form.username.trim(), form.password)
    setAuth(res.token, res.user)
    ElMessage.success(`Welcome, ${res.user.realName || res.user.username}`)
    router.push(homeForRole(res.user.role))
  } catch {
    // Errors surface through the HTTP interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #eef3ff 0%, #f5f7fa 100%);
}
.login-card {
  width: 380px;
  max-width: 100%;
  background: #fff;
  border: 1px solid #ebedf0;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
}
.login-head {
  margin-bottom: 4px;
}
.login-title {
  font-size: 18px;
  font-weight: 600;
  color: #2f6fed;
}
.login-sub {
  margin: 0 0 20px;
  font-size: 13px;
  color: #8a9099;
}
.password-toggle {
  cursor: pointer;
  color: #8f8d86;
}
.login-button {
  width: 100%;
}
.demo-box {
  margin-top: 8px;
  border: none;
}
.demo-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #646a73;
  padding: 2px 0;
}
.demo-row code {
  background: #f1f2f3;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
