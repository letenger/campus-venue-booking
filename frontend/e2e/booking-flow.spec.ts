import { test, expect, type Page } from '@playwright/test';

/**
 * End-to-end coverage of the core story against the in-memory demo API
 * (the VITE_DEMO=1 build served by `vite preview`):
 *
 *   student signs in → reserves a club-training slot → it waits for a teacher →
 *   teacher approves it → the student sees the booking as Approved.
 *
 * The demo build uses hash routes, so every navigation targets /#/<path>.
 */

const STUDENT = { username: 'student', password: 'student123' };
const TEACHER = { username: 'teacher', password: 'teacher123' };

async function signIn(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/#/login');
  await page.locator('input[placeholder="student"]').fill(username);
  await page.locator('input[type="password"]').fill(password);
  await page.locator('button.submit').click();
  // login redirects to the role home; wait for it so a later hash navigation
  // cannot race the redirect and get overridden. waitForFunction (not waitForURL)
  // because the redirect is a same-document hash change.
  await page.waitForFunction(() => /#\/(venues|approvals|admin\/venues)/.test(window.location.hash));
}

/** Drops the stored JWT and reloads, so the next sign-in is a different user. */
async function switchUser(page: Page): Promise<void> {
  // only the auth keys: the demo's own persisted state must survive the switch
  await page.evaluate(() => {
    window.localStorage.removeItem('sems_token');
    window.localStorage.removeItem('sems_user');
  });
  // a hash-only navigation would keep the in-memory auth state, so force a full reload
  await page.reload();
}

test('a club-training booking is approved by a teacher before it is confirmed', async ({ page }) => {
  // --- student signs in and lands on the venue catalogue -------------------
  await signIn(page, STUDENT.username, STUDENT.password);
  await expect(page).toHaveURL(/#\/venues/);

  // --- pick the first venue and the first free hour ------------------------
  await page.locator('button.venue').first().click();
  await expect(page).toHaveURL(/#\/venues\/\d+/);
  const venueName = ((await page.locator('.head h2').textContent()) ?? '').trim();
  expect(venueName.length).toBeGreaterThan(0);

  const freeCell = page.locator('.cell:not([disabled])').first();
  await expect(freeCell).toBeVisible();
  await freeCell.click();

  // --- club training is exactly the kind of request that waits -------------
  await page.locator('.panel .field', { hasText: 'Purpose' }).locator('.el-select').click();
  await page.locator('.el-select-dropdown__item', { hasText: 'Club training' }).click();
  await expect(page.locator('.panel .hint')).toHaveText('A teacher has to approve this request');

  await page.locator('button.confirm').click();

  // a successful submit redirects to "My bookings", where the request waits
  await expect(page).toHaveURL(/#\/bookings/);
  const pendingRow = page
    .locator('.trow')
    .filter({ hasText: venueName })
    .filter({ hasText: 'Club training' })
    .first();
  await expect(pendingRow).toContainText('Pending');

  // --- the teacher decides --------------------------------------------------
  await switchUser(page);
  await signIn(page, TEACHER.username, TEACHER.password);
  await expect(page).toHaveURL(/#\/approvals/);

  const request = page
    .locator('.req')
    .filter({ hasText: venueName })
    .filter({ hasText: 'Club training' })
    .first();
  await expect(request).toContainText('Pending');
  await request.getByRole('button', { name: 'Approve' }).click();

  // decided requests leave the waiting list
  await expect(request).toHaveCount(0);

  // --- the student sees the decision ----------------------------------------
  await switchUser(page);
  await signIn(page, STUDENT.username, STUDENT.password);
  await page.goto('/#/bookings');

  const approvedRow = page
    .locator('.trow')
    .filter({ hasText: venueName })
    .filter({ hasText: 'Club training' })
    .first();
  await expect(approvedRow).toContainText('Approved');
});
