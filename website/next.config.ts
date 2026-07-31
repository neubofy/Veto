import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  serverExternalPackages: ["firebase-admin", "jwks-rsa", "jose"],
};

export default nextConfig;
