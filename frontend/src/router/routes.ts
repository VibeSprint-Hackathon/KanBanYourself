import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        component: () => import('@/pages/IndexPage.vue'),
      },
      {
        path: 'quests',
        component: () => import('@/pages/QuestsPage.vue'),
      },
      {
        path: 'raids',
        component: () => import('@/pages/RaidsPage.vue'),
      },
      {
        path: 'achievements',
        component: () => import('@/pages/AchievementsPage.vue'),
      },
    ],
  },

  {
    path: '/achievements/share/:playerId/:achievementKey',
    component: () => import('@/pages/AchievementSharePage.vue'),
  },

  {
    path: '/:catchAll(.*)*',
    component: () => import('@/pages/ErrorNotFound.vue'),
  },
];

export default routes;
