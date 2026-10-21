<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-head">
        <span class="login-title">校园体育场馆预定系统</span>
      </div>
      <p class="login-sub">登录以预订场馆、查看审批与报表</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" :type="passwordVisible ? 'text' : 'password'" placeholder="密码"
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
          <el-button type="primary" class="login-button" :loading="loading" @click="onSubmit">登录</el-button>
        </el-form-item>
      </el-form>
      <el-collapse class="demo-box">
        <el-collapse-item title="演示账号">
          <div class="demo-row"><span>管理员</span><code>admin / admin123</code></div>
          <div class="demo-row"><span>教师</span><code>teacher / teacher123</code></div>
          <div class="demo-row"><span>学生</span><code>student / student123</code></div>
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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authLogin(form.username.trim(), form.password)
    setAuth(res.token, res.user)
    ElMessage.success(`欢迎，${res.user.realName || res.user.username}`)
    router.push(homeForRole(res.user.role))
  } catch {
    // 错误提示由 http 拦截器统一弹出
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
