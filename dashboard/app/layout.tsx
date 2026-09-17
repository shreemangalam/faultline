import type { Metadata } from "next";
import "./styles.css";

export const metadata: Metadata = {
  title: "Faultline Control Plane",
  description: "Integration impact analysis and durable recovery operations"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}