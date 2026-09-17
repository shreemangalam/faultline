import type { Metadata } from "next";
import "./styles.css";
import { AppShell } from "../components/app-shell";

export const metadata: Metadata = {
  title: "Faultline Control Plane",
  description: "Integration impact analysis and durable recovery operations"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><AppShell>{children}</AppShell></body></html>;
}
