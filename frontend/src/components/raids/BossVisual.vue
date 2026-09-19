<script setup lang="ts">
withDefaults(defineProps<{ name: string; defeated?: boolean }>(), { defeated: false });
</script>

<template>
  <div
    class="boss-placeholder"
    :class="{ defeated }"
    role="img"
    :aria-label="`${name} ${defeated ? 'defeated' : 'active'} placeholder`"
  >
    <i /><i />
  </div>
</template>

<style scoped>
.boss-placeholder {
  position: relative;
  width: 208px;
  height: 153px;
  border-radius: 45% 45% 35% 35%;
  background: linear-gradient(#b65848, #7f3933);
  box-shadow: 0 24px 24px #813f3b30;
  transition:
    filter 180ms ease,
    opacity 180ms ease;
}
.boss-placeholder.defeated {
  filter: grayscale(1);
  opacity: 0.46;
}
.boss-placeholder::before,
.boss-placeholder::after {
  content: '';
  position: absolute;
  top: -29px;
  width: 60px;
  height: 85px;
  background: #894a3b;
}
.boss-placeholder::before {
  left: 10px;
  border-radius: 80% 15% 65% 45%;
  transform: rotate(-23deg);
}
.boss-placeholder::after {
  right: 10px;
  border-radius: 15% 80% 45% 65%;
  transform: rotate(23deg);
}
.boss-placeholder i {
  position: absolute;
  width: 13px;
  height: 8px;
  background: #eb6554;
  border-radius: 10px;
  top: 57px;
  left: 66px;
}
.boss-placeholder i + i {
  left: auto;
  right: 66px;
}
</style>
