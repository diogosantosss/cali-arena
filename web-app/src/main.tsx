import './index.css'
import React from "react";
import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { router } from "./router";

createRoot(document.getElementById("container")!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);