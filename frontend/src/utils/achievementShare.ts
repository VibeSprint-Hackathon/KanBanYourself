import type { Router } from 'vue-router';
import type { Player } from '@/api/demo.types';
import type { AchievementState } from '@/components/achievements/achievement.types';

const LINKEDIN_COMPOSER_URL = 'https://www.linkedin.com/feed/?shareActive=true';

export interface AchievementShareAttempt {
  shareUrl: string;
  text: string;
  linkedinOpened: boolean;
  copied: Promise<boolean>;
}

export function buildAchievementShareUrl(
  router: Router,
  player: Pick<Player, 'id'>,
  achievement: Pick<AchievementState, 'key'>,
): string {
  const route = router.resolve({
    path: `/achievements/share/${player.id}/${encodeURIComponent(achievement.key)}`,
  });
  return new URL(route.href, window.location.origin).toString();
}

export function buildAchievementShareText(
  player: Pick<Player, 'name'>,
  achievement: Pick<AchievementState, 'name' | 'description'>,
  shareUrl: string,
): string {
  return [
    `${player.name} unlocked “${achievement.name}” in VibeSprint 🏆`,
    achievement.description,
    shareUrl,
    '#VibeSprint #DeveloperExperience #Gamification',
  ].join('\n\n');
}

export function shareAchievementOnLinkedIn(
  router: Router,
  player: Pick<Player, 'id' | 'name'>,
  achievement: Pick<AchievementState, 'key' | 'name' | 'description'>,
): AchievementShareAttempt {
  const shareUrl = buildAchievementShareUrl(router, player, achievement);
  const text = buildAchievementShareText(player, achievement, shareUrl);
  const linkedinWindow = window.open(LINKEDIN_COMPOSER_URL, '_blank');
  if (linkedinWindow) linkedinWindow.opener = null;

  return {
    shareUrl,
    text,
    linkedinOpened: linkedinWindow !== null,
    copied: copyText(text),
  };
}

export async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // Fall through to the synchronous browser fallback.
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  try {
    return document.execCommand('copy');
  } catch {
    return false;
  } finally {
    textarea.remove();
  }
}
