import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { StreakButton } from "@/components/sbo/streak-button";

export const Route = createFileRoute("/buttontest")({ component: Page });

function Page() {
  const [melting, setMelting] = useState(false);
  return (
    <div className="min-h-screen bg-background p-6">
      <div className="flex flex-wrap gap-8">
        <div data-t="fire"><StreakButton fireStreak={12} iceStreak={0} onPress={() => {}} /></div>
        <div data-t="ice"><StreakButton fireStreak={0} iceStreak={20} onPress={() => {}} melting={melting} /></div>
        <div data-t="neutral"><StreakButton fireStreak={0} iceStreak={0} onPress={() => {}} /></div>
      </div>
      <button id="boom" onClick={() => { setMelting(true); setTimeout(() => setMelting(false), 1400); }}>boom</button>
    </div>
  );
}
