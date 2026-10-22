import { createFileRoute, Outlet, redirect } from "@tanstack/react-router";
import { supabase } from "@/integrations/supabase/client";
import { IS_DEV_SITE, DEV_LOGIN } from "@/lib/sbo-env";

export const Route = createFileRoute("/_authenticated")({
  ssr: false,
  beforeLoad: async () => {
    let { data, error } = await supabase.auth.getUser();
    if ((error || !data.user) && IS_DEV_SITE) {
      // Dev copy: log straight into the shared test account, no email or password.
      await supabase.auth.signInWithPassword(DEV_LOGIN);
      ({ data, error } = await supabase.auth.getUser());
    }
    if (error || !data.user) throw redirect({ to: "/auth" });
    return { user: data.user };
  },
  component: () => <Outlet />,
});
