// @ts-check
const { test, expect } = require('@playwright/test');

test('click Query Both and capture result', async ({ page }) => {
  await page.goto('http://localhost:3000');
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(2000);

  // Screenshot before
  await page.screenshot({ path: 'test-results/before-query.png', fullPage: true });

  // Click the Query Both button
  const queryButton = page.locator('button:has-text("Query Both")');
  await queryButton.click();

  // Wait for queries to complete
  await page.waitForTimeout(3000);

  // Screenshot after
  await page.screenshot({ path: 'test-results/after-query.png', fullPage: true });

  // Check if latency values updated
  const pageContent = await page.content();
  console.log('Page has latency data:', pageContent.includes('ms'));
});
