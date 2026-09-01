import type { Metadata } from "next";
import "./styles.css";

export const metadata: Metadata = {
  title: "Faultline Survey",
  description: "Integration landscape impact analysis"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}