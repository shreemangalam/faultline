const surveyApi = process.env.SURVEY_API_URL ?? "http://localhost:8081";
const restoreApi = process.env.RESTORE_API_URL ?? "http://localhost:8082";

const nextConfig = {
  async rewrites() {
    return [
      { source: "/api/survey/:path*", destination: `${surveyApi}/api/:path*` },
      { source: "/api/restore/:path*", destination: `${restoreApi}/api/:path*` }
    ];
  }
};

export default nextConfig;