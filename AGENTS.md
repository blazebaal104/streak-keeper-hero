<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.
<!-- LOVABLE:END -->

- Project Zero logic lives in `src/lib/sbo-zero.ts` (pure, shared by UI and server); the coach runs in the `agentTick` server function, which writes `agent_messages` with the admin client and is budget-capped (per-user weekly + global daily AI calls, auto-pause on 402/403/429) with a never-repeating phrase library as fallback — why: keeps AI credits bounded while messages stay unique per user.
