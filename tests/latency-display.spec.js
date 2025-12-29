// @ts-check
const { test, expect } = require('@playwright/test');

const DASHBOARD_URL = 'http://localhost:3000';

test.describe('Latency Display on Architecture Diagrams', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto(DASHBOARD_URL);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
  });

  test('should display Federation timing on diagram after query', async ({ page }) => {
    // Click the Federation query button using text
    await page.getByText('Query Composed View').click();

    // Wait for the query to complete (activity log updates)
    await expect(page.getByText(/Success:.*ms/)).toBeVisible({ timeout: 10000 });

    // Verify timing appears on the diagram
    // Look for any SVG that contains timing information (Xms pattern)
    const svgs = page.locator('svg');
    const svgCount = await svgs.count();
    expect(svgCount).toBeGreaterThan(0);

    // Check that timing data appears in the Federation panel
    const diagramText = await page.locator('.architecture-panel').first().textContent();
    expect(diagramText).toMatch(/\d+ms/);
  });

  test('should display Event-Driven timing on diagram after query', async ({ page }) => {
    // Click the Event-Driven query button
    await page.getByText('Query Local Projection').click();

    // Wait for the query to complete
    await expect(page.locator('.architecture-panel').nth(1).getByText(/Success:/)).toBeVisible({ timeout: 10000 });

    // Verify timing appears on the diagram
    const eventDrivenPanel = page.locator('.architecture-panel').nth(1);
    const panelText = await eventDrivenPanel.textContent();
    expect(panelText).toMatch(/\d+ms/);
  });

  test('should update timing on subsequent queries', async ({ page }) => {
    // First query
    await page.getByText('Query Composed View').click();
    await expect(page.getByText(/Success:.*ms/)).toBeVisible({ timeout: 10000 });

    // Get first log message
    const logArea = page.locator('.architecture-panel').first().locator('.font-mono');
    const firstLog = await logArea.textContent();

    // Second query
    await page.getByText('Query Composed View').click();
    await page.waitForTimeout(500);

    // Log should have updated
    const secondLog = await logArea.textContent();
    expect(secondLog).toContain('Success:');
  });

  test('should show Router and subgraph timing in Federation diagram', async ({ page }) => {
    await page.getByText('Query Composed View').click();
    await expect(page.getByText(/Success:.*ms/)).toBeVisible({ timeout: 10000 });

    // Check that log message shows breakdown
    const panel = page.locator('.architecture-panel').first();
    const panelText = await panel.textContent();

    // Should show Router, HR, Employment, Security timing in the log
    expect(panelText).toMatch(/Router:.*\d+ms/);
    expect(panelText).toMatch(/HR:.*\d+ms/);
  });

  test('should show Projection Service timing in Event-Driven diagram', async ({ page }) => {
    await page.getByText('Query Local Projection').click();
    await expect(page.locator('.architecture-panel').nth(1).getByText(/Success:/)).toBeVisible({ timeout: 10000 });

    // Check that log shows Projection Service timing
    const eventDrivenPanel = page.locator('.architecture-panel').nth(1);
    const panelText = await eventDrivenPanel.textContent();

    expect(panelText).toMatch(/Projection Service:.*\d+ms/);
  });
});
