import "./assets/styles/index.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { loadTheme } from "./theme/theme";

loadTheme();

createRoot(document.getElementById("container")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);