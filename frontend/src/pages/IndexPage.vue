<template>
  <q-page class="column flex-center q-gutter-md">
    <div class="text-h4">VibeSprint Starter</div>

    <q-btn
      label="Check backend"
      color="primary"
      :loading="loading"
      @click="checkBackend"
    />

    <div v-if="status">
      Backend status: {{ status }}
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '@/api/http';

const status = ref('');
const loading = ref(false);

async function checkBackend() {
  loading.value = true;

  try {
    const response = await api.get<string>('/health');
    status.value = response.data;
  } catch {
    status.value = 'ERROR';
  } finally {
    loading.value = false;
  }
}
</script>
