export interface Env {
  ASSETS: Fetcher;
  // Publiczny URL backendu na Render, np. https://twoj-serwis.onrender.com
  BACKEND_URL?: string;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(request.url);

      // Proxy API do backendu (Render)
      if (url.pathname.startsWith("/api/")) {
        if (!env.BACKEND_URL) {
          return new Response(
            JSON.stringify({
              error: "Brak BACKEND_URL w Cloudflare Worker. Ustaw zmienną środowiskową BACKEND_URL."
            }),
            { status: 500, headers: { "content-type": "application/json; charset=utf-8" } }
          );
        }

        const backendBase = new URL(env.BACKEND_URL);
        const target = new URL(url.pathname + url.search, backendBase);
        return fetch(new Request(target.toString(), request));
      }

      // UI (statyczny index.html)
      if (url.pathname === "/") {
        const rewritten = new URL(request.url);
        rewritten.pathname = "/index.html";
        return env.ASSETS.fetch(new Request(rewritten.toString(), request));
      }

      return env.ASSETS.fetch(request);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      return new Response(`Worker error: ${msg}`,
        { status: 500, headers: { "content-type": "text/plain; charset=utf-8" } }
      );
    }
  }
};
