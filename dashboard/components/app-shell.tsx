"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";

export function AppShell({ children }: { children: React.ReactNode }) {
  const path = usePathname();
  const [theme, setTheme] = useState<"dark" | "light">("light");
  useEffect(() => {
    let next: "dark" | "light" = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    try {
      const saved = window.localStorage.getItem("faultline-theme");
      if (saved === "dark" || saved === "light") next = saved;
    } catch { /* Theme remains usable when storage is unavailable. */ }
    setTheme(next);
    document.documentElement.dataset.theme = next;
  }, []);
  function toggleTheme() {
    const next = theme === "dark" ? "light" : "dark";
    setTheme(next);
    document.documentElement.dataset.theme = next;
    try { window.localStorage.setItem("faultline-theme", next); } catch { /* Optional preference. */ }
  }
  return <>
    <a className="skip-link" href="#main-content">Skip to content</a>
    <header className="site-header">
      <div className="topbar">
        <Link href="/" className="brand" aria-label="Faultline home"><span className="brand-mark" aria-hidden="true">f<span> /</span></span><span>faultline<span className="brand-subtitle">Integration operations</span></span></Link>
        <nav className="module-nav" aria-label="Primary navigation">
          {[["/", "Survey"], ["/restore", "Restore"], ["/help", "Operator guide"]].map(([href, label]) =>
            <Link href={href} key={href} className={path === href ? "active" : ""} aria-current={path === href ? "page" : undefined}>{label}</Link>)}
        </nav>
        <div className="topbar-actions"><span className="environment"><span className="environment-dot" />Local demo<span className="environment-detail"> / Fictional systems</span></span><button className="theme-toggle" onClick={toggleTheme} aria-label={theme === "dark" ? "Use light theme" : "Use dark theme"} title={theme === "dark" ? "Use light theme" : "Use dark theme"}>{theme === "dark" ? "☼" : "◐"}</button></div>
      </div>
    </header>
    {children}
    <footer className="site-footer"><span>Faultline / Integration resilience control plane</span><span>Fictional enterprise landscape · Local environment</span></footer>
  </>;
}
