# VibeSprint Hackathon — Product Brief

## 1. Context

We are a team of 3 experienced developers participating in the VibeSprint 24-hour hackathon.

The project must be built and demoed within the hackathon timeframe.

Existing starter repository already contains:

### Backend
- Java 21
- Spring Boot
- Maven
- PostgreSQL
- Liquibase
- conventional layered architecture

### Frontend
- Vue 3
- TypeScript
- Quasar
- Vite
- Axios
- Pinia available

### Infrastructure
- Docker Compose
- PostgreSQL
- GitHub repository

The starter project is intentionally minimal.

The goal is not to build production-complete software.
The goal is to build a polished, reliable and memorable hackathon demo with a believable path toward a real product.

---

# 2. Product idea

Working concept:

A gamified layer for software development where real development activity becomes character and team progression.

Developers should not have to perform artificial game actions.

Their normal work:

- receiving issues
- starting tasks
- opening pull requests
- reviewing code
- merging pull requests

should automatically produce game progression.

The product should feel closer to a game integrated into the developer workflow than to another project-management dashboard.

---

# 3. Problem

Development teams already have tools such as:

- GitHub
- GitLab
- Jira
- Linear

These tools are good at tracking work but usually provide little emotional feedback or sense of progression.

A completed issue often becomes nothing more than a card moving to another column.

We want to make real development progress:

- visible
- rewarding
- social
- fun
- memorable

without replacing the tools developers already use.

---

# 4. Core product principle

The user should continue working normally in GitHub.

Our application observes real development activity and turns it into game progression.

Conceptually:

GitHub activity
→ Quest progression
→ XP
→ Character progression
→ Team progression

The game should reward actual work rather than requiring users to maintain another task system manually.

---

# 5. Core loop

The desired core loop is:

1. A developer receives a Quest.
2. The Quest corresponds to real development work.
3. The developer works normally.
4. Development activity is detected.
5. The Quest progresses or completes.
6. XP is awarded.
7. The character reacts.
8. The character may level up or unlock a cosmetic.
9. The team's shared objective progresses.

Example:

Issue assigned
→ Quest appears

PR opened
→ Quest becomes IN PROGRESS

PR merged
→ Quest becomes DONE
→ +180 XP
→ character celebrates
→ level up
→ cosmetic unlocked
→ team boss receives damage

---

# 6. Main integration

GitHub should eventually be the primary external integration.

Potential GitHub events:

- issue created
- issue assigned
- pull request opened
- pull request merged
- review submitted

For the hackathon, we do NOT need to support every event.

The minimum useful GitHub integration is likely:

issue / quest association

and

PR merged
→ Quest completed

GitHub webhooks should be preferred over constant API polling where practical.

The architecture should allow real GitHub events, while the demo must also have a deterministic fallback trigger.

We must not depend on:
- CI finishing during the presentation
- network timing
- GitHub merge queues
- external services behaving perfectly

A demo event may trigger the same backend business logic as the real webhook.

---

# 7. MVP goal

The first milestone should be achievable in approximately 5 hours of focused development.

At that point we want a complete demoable vertical slice.

Minimum MVP:

## Quest system

Quest:
- id
- title
- status
- XP reward
- assignee
- optional GitHub association

Statuses can initially be:

- TODO
- IN_PROGRESS
- DONE

## Player

Player:
- name
- XP
- level
- character state

## Character

Character states:

- idle
- coding
- happy / XP gained
- level up

Character progression should be visible.

## Team progression

A shared raid/boss objective.

Completing work should deal damage or otherwise advance the team goal.

## Realtime-feeling UI

When a Quest completes:

- Quest moves to DONE
- XP increases
- XP bar animates
- character reacts
- level-up occurs if appropriate
- team progress changes

This should happen without a manual page refresh.

WebSocket is preferred if implementation remains simple.
A simpler reliable mechanism is acceptable for the first MVP.

---

# 8. Main demo moment

The product should be designed around one very strong demo sequence.

Example starting state:

Player:
- Level 4
- 920 / 1000 XP

Boss:
- 180 HP remaining

Quest:
- Fix payment validation
- Reward: 180 XP

Demo action:

PR merge event occurs
OR
a deterministic demo trigger simulates the same event.

Then:

Quest Complete
+180 XP

XP:
920 → 1100

LEVEL UP
Level 5

Rare cosmetic unlocked

Character changes appearance

Boss:
180 damage

BOSS DEFEATED

The entire product idea should become understandable from this single sequence.

---

# 9. Character direction

A custom character is being created by an artist.

The character is an important part of the product identity.

Assets planned:

## Idle
3 PNG frames

Used:
- dashboard
- profile
- browser companion
- normal state

## Coding
3 PNG frames

Used when:
- Quest is IN_PROGRESS
- developer is actively working

## Happy / XP
3 PNG frames

Used when:
- Quest completes
- XP is received

## Level Up
3 PNG frames

Used for:
- level increase
- major achievement
- important unlock

All frames:
- PNG
- transparent background
- 1024x1024 canvas
- consistent character position

Additional progression assets may include:
- hoodie
- headset
- glow/effect

Progression should initially demonstrate only one meaningful cosmetic unlock.

We do NOT need a complete equipment system for MVP.

---

# 10. Frontend direction

Avoid creating many pages.

The application should initially have approximately three primary views.

## Dashboard

The most important and most polished screen.

Should contain:

- character
- player name
- level
- XP progress
- current title
- next unlock
- active Quest
- GitHub status/reference
- team raid/boss progress
- recent activity

This screen should communicate most of the product without requiring navigation.

## Quests

Simple Kanban:

- TODO
- IN PROGRESS
- DONE

Quest cards may show:

- title
- assignee
- XP reward
- GitHub association
- status

Quest details should preferably use a drawer/dialog rather than another page.

## Character / Profile

Large character presentation.

Potential content:

- level
- XP
- title
- equipment/cosmetics
- locked future rewards
- achievements later

This becomes the natural place for future progression features.

---

# 11. Visual direction

The product should feel:

- game-like
- modern
- polished
- energetic
- developer-oriented

It should NOT look like a generic corporate admin panel.

Possible visual direction:

- dark interface
- strong character artwork
- bright XP/progression accents
- large visual hierarchy
- animated progress
- cards with clear depth
- minimal but meaningful game effects

Game terminology may include:

- Quest
- XP
- Level
- Raid
- Boss
- Unlock
- Achievement

Copy should be short and energetic.

Example:

Instead of:

Task successfully completed

use:

Quest Complete
+180 XP

Instead of:

Sprint progress: 82%

use:

Boss HP: 18%

---

# 12. Design workflow

Lovable AI may be used for rapid design exploration.

Lovable is NOT the source of truth for application architecture.

Expected workflow:

Lovable
→ visual prototype
→ screenshot/reference
→ short design specification
→ implementation in Vue 3 + Quasar

React/Tailwind code generated by Lovable should only be treated as visual reference.

Do not introduce React or Tailwind into the main application.

Codex should implement approved designs using the existing Vue/Quasar stack.

---

# 13. Stretch features

These are NOT part of the initial MVP.

They should only be started after the core demo is stable.

Potential stretch features:

## Browser companion

A browser extension showing the character on normal websites.

Initial version could:

- appear in a page corner
- play idle animation
- react to XP events
- show Quest completion
- be draggable
- show current XP/level

Interesting demo:

Developer merges a PR on GitHub.

While still on GitHub, the companion reacts:

+180 XP
Level Up!

This demonstrates that the product can live alongside existing developer tools instead of requiring constant use of our dashboard.

## Character progression

Possible future additions:

- equipment
- cosmetics
- titles
- achievements
- progression tiers
- rare unlocks

Avoid creating a complex RPG system during MVP.

## Shareable achievement

Generate an attractive PNG such as:

ANDREI REACHED LEVEL 5

CODE RAIDER

This sprint:
- 8 Quests
- 4 PRs
- 1280 XP

This may be suitable for LinkedIn or other social sharing.

Actual LinkedIn API integration is not required.

## AI Quest evaluation

AI may suggest:

- difficulty
- XP reward
- Quest category

Human confirmation should remain possible.

n8n may potentially be used for this integration.

## More GitHub automation

Future:
- automatic Quest creation from assigned issue
- PR association
- reviews
- multiple repositories
- GitHub App installation

These are stretch goals.

---

# 14. n8n

n8n may be available during the hackathon.

Use it primarily for:

- integration glue
- webhook orchestration
- AI preprocessing
- external notifications
- demo automation

Do NOT move core business logic into n8n.

Correct ownership:

n8n:
integration/orchestration

Spring Boot:
source of truth and business logic

For example:

GitHub
→ n8n
→ Spring backend
→ XP / Quest logic
→ WebSocket
→ Vue

Direct GitHub → Spring is also acceptable if simpler.

---

# 15. Technical principles

The hackathon priority is:

1. working demo
2. complete core user flow
3. reliability
4. visual polish
5. impressive optional features
6. architecture perfection

Avoid:

- premature abstractions
- speculative scalability
- microservices
- unnecessary infrastructure
- large refactors
- new dependencies without real value

Backend should stay close to the existing architecture.

Prefer:

controller
→ service
→ repository

Use DTOs at API boundaries.

Database schema changes must use Liquibase.

Frontend:

- Vue 3
- Composition API
- TypeScript
- Quasar
- existing Axios setup
- Pinia when shared state is actually needed

---

# 16. Collaboration constraints

There are 3 developers.

All may use AI agents.

This creates a significant risk of:

- merge conflicts
- duplicated implementations
- inconsistent DTOs
- different naming conventions
- AI agents refactoring shared code differently

We should therefore optimize work decomposition for parallel development.

General rule:

one task
→ one owner
→ one branch/worktree
→ narrow scope

Avoid two developers or agents modifying the same shared core files simultaneously.

Potential ownership areas:

Developer A:
- backend core
- contracts
- Quest/player progression

Developer B:
- main Vue UI
- dashboard
- Kanban
- character presentation

Developer C:
- GitHub integration
- realtime
- demo tooling
- integration glue

This is only an initial suggestion and should be evaluated against the actual repository.

---

# 17. Integration strategy

Integrate frequently.

Target:

every 60–90 minutes

→ merge stable work
→ pull latest main
→ run the application
→ verify the primary demo flow

Do not let three large branches diverge for many hours.

`main` should remain the last reasonably demoable state.

Experimental stretch features should remain isolated until stable.

---

# 18. AI usage constraints

Codex usage is limited and relatively expensive.

Observed usage from initial experiments:

Full repository read-only analysis:
approximately 5% of 5-hour allowance
approximately 1% weekly allowance

Small coding task:
approximately 6% of 5-hour allowance
approximately 2% weekly allowance

Therefore:

Do not use Codex for trivial edits.

Use Codex primarily for:

- multi-file features
- unfamiliar APIs
- integrations
- complex bugs
- focused code review
- meaningful implementation tasks

Tasks given to Codex should specify:

- Goal
- Scope
- Requirements
- Allowed files/areas
- Forbidden changes
- Verification

Avoid repeatedly asking Codex to analyze the whole repository.

---

# 19. Current known product risks

We should explicitly plan around:

## Scope explosion

Character systems, browser extension, GitHub automation and game mechanics can easily exceed available time.

Core MVP must remain small.

## External dependency during demo

Do not make the presentation depend on:
- live CI
- actual PR merge latency
- GitHub availability
- external AI responses

Provide deterministic fallback paths.

## Design taking too long

Do not spend hours building a complete Figma design.

Prefer:

rapid prototype
→ approved reference
→ implementation
→ screenshot comparison
→ polish

## AI merge conflicts

Agents must receive narrow scopes and shared architecture rules.

---

# 20. Definition of MVP complete

The MVP is complete when the following can be demonstrated reliably:

1. User/player exists.
2. Character is visible.
3. Player has XP and level.
4. Quests appear on a board.
5. A Quest can progress to DONE.
6. Quest completion awards XP.
7. Character reacts.
8. Level-up can occur.
9. One cosmetic/progression change can be shown.
10. Shared team/boss progress changes.
11. The entire flow can be demonstrated without restarting or manually editing the database.

Anything beyond this is optional.

---

# 21. Definition of a strong final demo

A strong final demo should ideally demonstrate:

real developer activity
→ automatic Quest progression
→ immediate visual feedback
→ personal progression
→ team progression

and possibly one additional memorable extension:

- browser companion
  OR
- polished character progression
  OR
- social achievement export

We should prefer one excellent stretch feature over five incomplete ones.

---

# 22. What we need to decide next

Before implementation begins, we need a concrete project plan.

Important unresolved questions include:

- exact domain model
- exact MVP endpoints
- GitHub integration scope
- whether WebSocket is required immediately
- authentication approach
- how Quest ↔ GitHub issue/PR linking works
- exact team ownership boundaries
- first 5-hour implementation sequence
- frontend component structure
- database schema
- which existing starter code should remain or be removed
- what should be mocked initially
- what must be real for the final demo
- which stretch feature gives the best effect/time ratio

These should be resolved by inspecting the existing repository rather than designing architecture in isolation.