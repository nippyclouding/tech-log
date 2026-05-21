import { useState } from "react";
import { Link } from "react-router-dom";
import { Github, Mail, X, Copy, Check } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

export function Footer() {
  const [showContact, setShowContact] = useState(false);
  const [copied, setCopied] = useState(false);
  const email = "nippyclouding@gmail.com";

  const copyEmail = () => {
    navigator.clipboard.writeText(email);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <footer className="border-t border-gray-100 bg-white py-6 text-slate-600">
      <div className="container mx-auto px-4">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex flex-wrap justify-center sm:justify-start gap-x-6 gap-y-2 text-sm font-medium">
            <Link to="/privacy" className="text-slate-500 hover:text-slate-900 transition-colors">Privacy Policy</Link>
            <button 
              onClick={() => setShowContact(true)}
              className="text-slate-500 hover:text-slate-900 transition-colors cursor-pointer outline-none"
            >
              Contact
            </button>
          </div>
          
          <div className="flex items-center space-x-3 text-slate-400 text-xs">
            <span>© 2026 Tech Log. All rights reserved.</span>
            <a href="https://github.com/nippyclouding" target="_blank" rel="noopener noreferrer" className="hover:text-slate-900 transition-colors">
              <Github className="h-4.5 w-4.5" />
            </a>
          </div>
        </div>
      </div>

      <AnimatePresence>
        {showContact && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowContact(false)}
              className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="relative w-full max-w-md bg-white rounded-[32px] p-8 shadow-2xl overflow-hidden"
            >
              <button 
                onClick={() => setShowContact(false)}
                className="absolute right-6 top-6 p-2 text-slate-400 hover:text-slate-900 transition-colors bg-slate-50 rounded-full"
              >
                <X className="w-4 h-4" />
              </button>

              <div className="text-center">
                <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
                  <Mail className="w-8 h-8 text-blue-600" />
                </div>
                <h2 className="text-2xl font-bold text-slate-900 mb-2 tracking-tight">Get in Touch</h2>
                <p className="text-slate-500 text-sm mb-8 leading-relaxed px-4">궁금한 점이나 협업 문의는 아래 이메일로 연락주세요.</p>
                
                <div className="bg-slate-50 rounded-2xl p-4 flex items-center justify-between border border-slate-100 mb-8 w-full">
                  <span className="text-slate-900 font-semibold truncate mr-4">{email}</span>
                  <div className="flex items-center space-x-1">
                    <button 
                      onClick={copyEmail}
                      className="p-2 text-slate-400 hover:text-blue-600 transition-colors"
                      title="Copy email"
                    >
                      {copied ? <Check className="w-4 h-4 text-green-500" /> : <Copy className="w-4 h-4" />}
                    </button>
                    <a 
                      href={`mailto:${email}`}
                      className="p-2 text-slate-400 hover:text-blue-600 transition-colors"
                      title="Open in mail client"
                    >
                      <Mail className="w-4 h-4" />
                    </a>
                  </div>
                </div>

                <a 
                  href={`mailto:${email}`}
                  className="inline-flex items-center justify-center w-full py-4 bg-slate-900 text-white rounded-2xl font-bold hover:bg-slate-800 transition-all shadow-lg shadow-slate-200 group"
                >
                  <Mail className="w-4 h-4 mr-2 group-hover:scale-110 transition-transform" />
                  메일 보내기
                </a>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </footer>
  );
}
