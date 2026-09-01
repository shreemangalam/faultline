const surveyApi = process.env.SURVEY_API_URL ?? "http://localhost:8081";

const nextConfig = {
  async rewrites() {
    return [{ source: "/api/survey/:path*", destination: `${surveyApi}/api/:path*` }];
  }
};

export default nextConfig;