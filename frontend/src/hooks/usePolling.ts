import { useEffect } from "react";

export function usePolling(callback: () => void, intervalMs = 20000, enabled = true): void {
  useEffect(() => {
    if (!enabled) {
      return;
    }
    let timer = 0;
    const stop = () => {
      if (timer) {
        window.clearInterval(timer);
        timer = 0;
      }
    };
    const start = () => {
      stop();
      timer = window.setInterval(() => {
        if (document.visibilityState === "visible") {
          callback();
        }
      }, intervalMs);
    };
    const onVisibility = () => {
      if (document.visibilityState === "visible") {
        callback();
        start();
      } else {
        stop();
      }
    };
    if (document.visibilityState === "visible") {
      start();
    }
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      stop();
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [callback, intervalMs, enabled]);
}
