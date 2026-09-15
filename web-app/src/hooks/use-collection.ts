import { useCallback, useEffect, useReducer } from "react";
import { ApiError } from "@/api/client";

interface CollectionState<T> {
  items: T[];
  loading: boolean;
  error: string | null;
}

type CollectionAction<T> =
  | { type: "loading" }
  | { type: "loaded"; items: T[] }
  | { type: "error"; message: string }
  | { type: "patch"; update: (items: T[]) => T[] };

function collectionReducer<T>(
  state: CollectionState<T>,
  action: CollectionAction<T>
): CollectionState<T> {
  switch (action.type) {
    case "loading":
      return { ...state, loading: true, error: null };
    case "loaded":
      return { items: action.items, loading: false, error: null };
    case "error":
      return { ...state, loading: false, error: action.message };
    case "patch":
      return { ...state, items: action.update(state.items) };
  }
}

/**
 * Loads a list of items from a service function and exposes reload().
 * The `load` function must be stable (module-level or wrapped in useCallback).
 */
export function useCollection<T>(load: () => Promise<T[]>, errorLabel: string) {
  const [state, dispatch] = useReducer(collectionReducer<T>, {
    items: [],
    loading: false,
    error: null,
  } as CollectionState<T>);

  const reload = useCallback(async () => {
    dispatch({ type: "loading" });
    try {
      const items = await load();
      dispatch({ type: "loaded", items });
    } catch (err) {
      dispatch({
        type: "error",
        message: err instanceof ApiError ? err.message : errorLabel,
      });
    }
  }, [load, errorLabel]);

  const patchItems = useCallback((update: (items: T[]) => T[]) => {
    dispatch({ type: "patch", update });
  }, []);

  useEffect(() => {
    void reload();
    // initial fetch only; refreshes go through reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { ...state, reload, patchItems };
}
