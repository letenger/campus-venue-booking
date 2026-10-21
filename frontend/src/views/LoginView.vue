<template>
  <div class="auth">
    <section class="pitch">
      <div class="brand">
        <span class="mark">CV</span>
        <span>Campus Venue</span>
      </div>

      <div class="pitch-copy">
        <h1>Book the court,<br />not the argument.</h1>
        <p>
          Six venues, one timetable, and a clear answer to "is it free on Thursday at four".
          Small games are confirmed on the spot. Anything that takes a venue from other people
          goes to a teacher.
        </p>

        <ul class="points">
          <li><span>01</span> Live availability for every court and pitch</li>
          <li><span>02</span> Approval in one click, with a reason when it says no</li>
          <li><span>03</span> Unused slots close themselves at the end of the day</li>
        </ul>
      </div>

      <p class="footnote">A school project built with Spring Boot and Vue.</p>
    </section>

    <section class="panel">
      <div class="form-wrap">
        <h2>Sign in</h2>
        <p class="muted">Use your school account.</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large"
          @submit.prevent="onSubmit">
          <el-form-item label="Username" prop="username">
            <el-input v-model="form.username" placeholder="student" :prefix-icon="User" autofocus />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input v-model="form.password" :type="visible ? 'text' : 'password'" placeholder="••••••••"
              :prefix-icon="Lock" @keyup.enter="onSubmit">
              <template #suffix>
                <el-icon class="toggle" @click="visible = !visible">
                  <View v-if="!visible" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <button class="btn btn-primary submit" type="submit" :disabled="loading">
            {{ loading ? 'Signing in…' : 'Sign in' }}
          </button>
        </el-form>

        <div class="demo">
          <p class="section-title">Demo accounts</p>
          <div class="demo-grid">
            <button v-for="d in demos" :key="d.username" class="demo-item" @click="fill(d)">
              <strong>{{ d.role }}</strong>
              <span>{{ d.username }} / {{ d.password }}</span>
            </button>
          </div>
        </div>
      </div>
    </section>
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
const visible = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: 'Enter your username', trigger: 'blur' }],
  password: [{ required: true, message: 'Enter your password', trigger: 'blur' }],
}

const demos = [
  { role: 'Student', username: 'student', password: 'student123' },
  { role: 'Teacher', username: 'teacher', password: 'teacher123' },
  { role: 'Admin', username: 'admin', password: 'admin123' },
]

function fill(d) {
  form.username = d.username
  form.password = d.password
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
    // the HTTP interceptor already showed the message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  min-height: 100vh;
  background: var(--surface);
}

/* ---------- left ---------- */

.pitch {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 40px 48px;
  color: #eaf0ff;
  background:
    radial-gradient(120% 90% at 10% 0%, #2b3f9e 0%, transparent 55%),
    radial-gradient(100% 80% at 100% 100%, #0d7f74 0%, transparent 50%),
    #18224a;
  overflow: hidden;
}

.pitch::after {
  content: '';
  position: absolute;
  inset: -10% -20% auto auto;
  width: 620px;
  height: 620px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 50%;
  box-shadow:
    0 0 0 90px rgba(255, 255, 255, 0.04),
    0 0 0 180px rgba(255, 255, 255, 0.03);
}

.brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  font-size: 12px;
  font-weight: 700;
}

.pitch-copy {
  position: relative;
  margin: auto 0;
  max-width: 460px;
}

.pitch-copy h1 {
  font-size: 40px;
  line-height: 1.12;
  letter-spacing: -0.03em;
  margin-bottom: 18px;
}

.pitch-copy p {
  margin: 0 0 26px;
  font-size: 14.5px;
  line-height: 1.65;
  color: rgba(234, 240, 255, 0.78);
}

.points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.points li {
  display: flex;
  gap: 12px;
  align-items: baseline;
  font-size: 13.5px;
  color: rgba(234, 240, 255, 0.9);
}

.points span {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(234, 240, 255, 0.45);
}

.footnote {
  position: relative;
  margin: 0;
  font-size: 12px;
  color: rgba(234, 240, 255, 0.45);
}

/* ---------- right ---------- */

.panel {
  display: grid;
  place-items: center;
  padding: 40px 32px;
}

.form-wrap {
  width: 100%;
  max-width: 360px;
}

.form-wrap h2 {
  font-size: 22px;
  margin-bottom: 4px;
}

.form-wrap > p {
  margin: 0 0 26px;
  font-size: 13.5px;
}

.submit {
  width: 100%;
  height: 42px;
  margin-top: 6px;
}

.toggle {
  cursor: pointer;
  color: var(--muted);
}

.demo {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.demo-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}

.demo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: var(--surface-2);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}

.demo-item:hover {
  border-color: var(--line-strong);
  background: #fff;
}

.demo-item strong {
  font-size: 12.5px;
  font-weight: 600;
}

.demo-item span {
  font-size: 12px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 900px) {
  .auth {
    grid-template-columns: 1fr;
  }

  .pitch {
    padding: 28px 24px 32px;
  }

  .pitch-copy h1 {
    font-size: 30px;
  }

  .points,
  .footnote {
    display: none;
  }

  .panel {
    padding: 28px 20px 40px;
  }
}
</style>
