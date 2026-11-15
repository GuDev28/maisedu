import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "./estilo.css";
import { AuthProvider } from "./context/AuthContext";
import { InterfaceProvider } from "./context/InterfaceContext";
import App from "./App.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <InterfaceProvider>
          <App />
        </InterfaceProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>
);
