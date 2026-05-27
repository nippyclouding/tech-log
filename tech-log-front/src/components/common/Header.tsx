import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Search, Menu, Github, LogOut } from "lucide-react";
import { useAuth } from "../../contexts/AuthContext";
import { motion, AnimatePresence } from "motion/react";
import { useState, useEffect } from "react";

export function Header() {
  const { user, login, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [showMenu, setShowMenu] = useState(false);
  const [showMobileSearch, setShowMobileSearch] = useState(false);
  const [searchQuery, setSearchQuery] = useState(searchParams.get("q") || "");

  useEffect(() => {
    setSearchQuery(searchParams.get("q") || "");
  }, [searchParams]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setShowMobileSearch(false);
    if (searchQuery.trim()) {
      navigate(`/?q=${encodeURIComponent(searchQuery.trim())}`);
    } else {
      navigate("/");
    }
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-gray-100 bg-white/80 backdrop-blur-md">
      <div className="container mx-auto flex h-16 items-center justify-between px-4">
        <div className="flex items-center space-x-6">
          <div className="flex flex-col">
            <Link to="/" className="flex items-center space-x-2">
              <div className="h-8 w-8 rounded-lg bg-black flex items-center justify-center">
                <span className="text-white font-bold italic">N</span>
              </div>
              <span className="text-xl font-bold tracking-tight text-gray-900 hidden sm:block">Tech Log</span>
              <span className="text-xl font-bold tracking-tight text-gray-900 sm:hidden">Tech Log</span>
            </Link>
          </div>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center space-x-8">
            <Link to="/" className="text-sm font-medium text-gray-600 hover:text-black transition-colors">Posts</Link>
          </nav>
        </div>

        {/* Global Search Center - Only for Posts page potentially, but user said "중앙으로 옮기고" */}
        <div className="flex-1 max-w-md mx-8 hidden lg:block">
          <form onSubmit={handleSearch} className="relative group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 group-focus-within:text-blue-500 transition-colors" />
            <input 
              type="text" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search posts..."
              className="w-full pl-10 pr-4 py-2 bg-slate-100 border-transparent rounded-xl text-sm focus:bg-white focus:border-blue-200 outline-none transition-all group-focus-within:ring-4 group-focus-within:ring-blue-50"
            />
          </form>
        </div>

        <div className="flex items-center space-x-4">
          <button
            type="button"
            aria-label="검색 열기"
            aria-expanded={showMobileSearch}
            onClick={() => setShowMobileSearch(current => !current)}
            className="lg:hidden p-2 text-gray-500 hover:text-black transition-colors"
          >
            <Search className="h-5 w-5" />
          </button>
          
          {!user && (
            <button 
              onClick={login}
              className="px-4 py-2 rounded-xl bg-slate-900 text-white text-xs font-bold flex items-center space-x-2 hover:bg-slate-800 transition-all shadow-lg shadow-slate-200"
            >
              <Github className="w-4 h-4" />
              <span className="hidden sm:inline">깃허브로 로그인</span>
              <span className="sm:hidden">로그인</span>
            </button>
          )}

          <div className="relative">
            {user && (
              <div className="flex items-center space-x-3">
                <button 
                  onClick={() => setShowMenu(!showMenu)}
                  className="flex items-center hover:opacity-80 transition-opacity"
                >
                  <img 
                    src={user.avatar || ""} 
                    className="h-8 w-8 rounded-full border border-gray-200 shadow-sm" 
                    alt="profile" 
                  />
                </button>

                <AnimatePresence>
                  {showMenu && (
                    <motion.div 
                      initial={{ opacity: 0, scale: 0.95, y: 10 }}
                      animate={{ opacity: 1, scale: 1, y: 0 }}
                      exit={{ opacity: 0, scale: 0.95, y: 10 }}
                      className="absolute right-0 top-full mt-2 w-48 rounded-xl bg-white border border-gray-100 shadow-xl p-2 z-50 text-sm"
                    >
                      <div className="px-3 py-2 border-b border-gray-50 mb-1">
                        <p className="font-semibold truncate">{user.name}</p>
                        <p className="text-xs text-gray-400 truncate">{user.provider}</p>
                      </div>
                      <button 
                        onClick={() => { logout(); setShowMenu(false); }}
                        className="w-full flex items-center space-x-2 px-3 py-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                      >
                        <LogOut className="w-4 h-4" />
                        <span>Sign Out</span>
                      </button>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            )}
          </div>

          <button className="md:hidden p-2 text-gray-500 hover:text-black transition-colors">
            <Menu className="h-5 w-5" />
          </button>
        </div>
      </div>
      {showMobileSearch && (
        <form onSubmit={handleSearch} className="border-t border-gray-100 px-4 py-3 lg:hidden">
          <label htmlFor="mobile-post-search" className="sr-only">게시글 검색</label>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              id="mobile-post-search"
              autoFocus
              type="search"
              value={searchQuery}
              onChange={event => setSearchQuery(event.target.value)}
              placeholder="게시글 검색"
              className="w-full rounded-xl bg-slate-100 py-2 pl-10 pr-4 text-sm outline-none focus:bg-white focus:ring-2 focus:ring-blue-100"
            />
          </div>
        </form>
      )}
    </header>
  );
}
