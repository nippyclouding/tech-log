import { BrowserRouter as Router, Navigate, Routes, Route } from "react-router-dom";
import { Header } from "./components/common/Header";
import { Footer } from "./components/common/Footer";
import { HomePage } from "./pages/HomePage";
import { PostPage } from "./pages/PostPage";
import { AuthProvider } from "./contexts/AuthContext";
import { AdminPage } from "./pages/AdminPage";
import { AboutPage } from "./pages/AboutPage";
import { ProjectsPage } from "./pages/ProjectsPage";
import { PrivacyPage } from "./pages/PrivacyPage";

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/admin-console" element={<Navigate to="/admin" replace />} />
          <Route path="*" element={<PublicSite />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

function PublicSite() {
  return (
    <div className="min-h-screen flex flex-col font-sans bg-slate-50/30">
      <Header />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/post/:id" element={<PostPage />} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/privacy" element={<PrivacyPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}
