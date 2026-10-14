<template>
  <view class="select-role-container">
    <!-- 顶部标题区 -->
    <view class="header-section">
      <view class="brand-title">
        <text class="brand-q">Q</text><text class="brand-uanta">uanta</text>
      </view>
      <text class="subtitle">请选择您的身份</text>
    </view>

    <!-- 中间按钮区 -->
    <view class="button-area">
      <!-- 新生按钮 -->
      <view
        class="role-button"
        :class="{
          'role-button--active': selectedRole === 'freshman',
          'role-button--default': selectedRole !== 'freshman'
        }"
        @click="handleFreshmanClick"
      >
        <view class="icon-wrapper user-icon-wrapper">
          <image class="user-icon-image" src="/static/icon/user.svg" mode="aspectFit" />
        </view>
        <text class="button-label">我是新生</text>
      </view>

      <!-- 塔员按钮 -->
      <view
        class="role-button"
        :class="{
          'role-button--active': selectedRole === 'tower',
          'role-button--default': selectedRole !== 'tower'
        }"
        @click="handleTowerClick"
      >
        <view class="icon-wrapper shield-icon-wrapper">
          <image class="member-icon-image" src="/static/icon/member.svg" mode="aspectFit" />
        </view>
        <text class="button-label tower-label">我是塔员</text>
      </view>
    </view>

    <!-- 底部版权区 -->
    <view class="footer-section">
      <text class="copyright">© 2026 Quanta Team</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const selectedRole = ref('')

const handleFreshmanClick = () => {
  selectedRole.value = 'freshman'
  setTimeout(() => {
    uni.navigateTo({
      url: '/pages/login/freshman'
    })
  }, 200)
  uni.setStorageSync('selectedRole', 'freshman')
}

const handleTowerClick = () => {
  selectedRole.value = 'tower'
  setTimeout(() => {
    uni.navigateTo({
      url: '/pages/login/tower'
    })
  }, 200)
  uni.setStorageSync('selectedRole', 'tower')
}
</script>

<style scoped>
.select-role-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #FFEDD480 0%, #FFFFFF 50%, #FFFFFF 50%, #FFEDD480 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 0;
}

/* 顶部标题区 */
.header-section {
  width: 100%;
  padding-top: 80rpx;
  padding-bottom: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-title {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 15rpx;
}

.brand-q {
  font-size: 64rpx;
  font-weight: 700;
  color: #FF6600;
  letter-spacing: -2rpx;
}

.brand-uanta {
  font-size: 64rpx;
  font-weight: 700;
  color: #1E2939;
  letter-spacing: -2rpx;
}

.subtitle {
  font-size: 28rpx;
  color: #6A7282;
  font-weight: 500;
  font-family: Inter, -apple-system, sans-serif;
  line-height: 40rpx;
  letter-spacing: 0;
}

/* 中间按钮区 */
.button-area {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 32rpx;
  padding: 0 16rpx;
}

.role-button {
  width: 688rpx;
  height: 352rpx;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}

/* 选中状态 */
.role-button--active {
  background: linear-gradient(135deg, #FDAF32 0%, #FEAA2F 7.14%, #FEA52D 14.29%, #FEA12A 21.43%, #FF9C27 28.57%, #FF9725 35.71%, #FF9222 42.86%, #FF8D1F 50%, #FF871B 57.14%, #FF8218 64.29%, #FF7D14 71.43%, #FF7710 78.57%, #FF720B 85.71%, #FF6C05 92.86%, #FF6600 100%);
  box-shadow: 0 8rpx 16rpx rgba(255, 102, 0, 0.15);
}

/* 未选中状态 */
.role-button--default {
  background: #F9FAFB80;
  border: 2rpx solid #E5E7EB;
}

/* 图标容器 */
.icon-wrapper {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}

.user-icon-wrapper {
  background: rgba(255, 255, 255, 0.35);
}

.role-button--default .user-icon-wrapper {
  background: #F3F4F6;
}

.shield-icon-wrapper {
  background: #EEF2F5;
}

.role-button--active .shield-icon-wrapper {
  background: rgba(255, 255, 255, 0.35);
}

.user-icon-image {
  width: 44rpx;
  height: 54rpx;
}

.member-icon-image {
  width: 48rpx;
  height: 60rpx;
}

/* 图标颜色控制 */
/* 未选中状态 */
.role-button--default .user-icon-image {
  filter: invert(1);
}

/* 选中状态：图标变白色 */
.role-button--active .user-icon-image,
.role-button--active .member-icon-image {
  filter: brightness(0) saturate(100%) invert(1);
}

.role-button--default .member-icon-image {
  filter: none;
}

/* 按钮文字 */
.button-label {
  margin-top: 17rpx;
  font-size: 48rpx;
  font-weight: 500;
  font-family: Inter, -apple-system, sans-serif;
  line-height: 64rpx;
  letter-spacing: 0;
  text-align: center;
}

.role-button--active .button-label {
  color: #FFFFFF;
}

.role-button--default .button-label {
  color: #1E2939;
}

.tower-label {
  color: #1E2939;
}

/* 底部版权区 */
.footer-section {
  width: 100%;
  padding-bottom: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.copyright {
  font-size: 24rpx;
  color: #99A1AF;
  font-weight: 400;
  font-family: Inter, -apple-system, sans-serif;
  line-height: 32rpx;
  letter-spacing: 0;
  text-align: center;
}
</style>
