// Which copy of SBO is this? The dev copy runs at sbo-dev.<subdomain>.workers.dev with its own
// throwaway database and a shared test account, so new features can be tried before release.
export const IS_DEV_SITE =
  typeof window !== "undefined" && /^sbo-dev\./.test(window.location.hostname);

// Public values of the dev Supabase project (publishable key, safe to ship).
export const DEV_SUPABASE = {
  url: "https://oiqxydvtxshdqwaxguud.supabase.co",
  key: "sb_publishable_2_vKQhEkg95lFg5O-fDxXw_GnnS0aAc",
};

// Test account of the dev database only. It holds fake data, nothing real.
export const DEV_LOGIN = { email: "dev@sbo.dev", password: "dev-Ad9h0GJG4eCseg3Z" };
