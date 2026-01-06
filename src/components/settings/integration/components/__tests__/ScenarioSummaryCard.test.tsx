import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ScenarioSummaryCard } from '../ScenarioSummaryCard';

describe('ScenarioSummaryCard', () => {
  it('should display scenario count', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={2}
        errorCount={0}
      />
    );
    expect(screen.getByText('场景')).toBeInTheDocument();
    expect(screen.getByText('8 个')).toBeInTheDocument();
  });

  it('should display running count when > 0', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={2}
        errorCount={0}
      />
    );
    expect(screen.getByText(/2.*运行中/)).toBeInTheDocument();
  });

  it('should display error count when > 0', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={0}
        errorCount={1}
      />
    );
    expect(screen.getByText(/1.*失败/)).toBeInTheDocument();
  });

  it('should display idle when no running or errors', () => {
    render(
      <ScenarioSummaryCard
        totalScenarios={8}
        runningCount={0}
        errorCount={0}
      />
    );
    expect(screen.getByText('全部空闲')).toBeInTheDocument();
  });
});
