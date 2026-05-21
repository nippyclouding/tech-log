import { motion } from "motion/react";

export function ProjectsPage() {
  return (
    <div className="container max-w-6xl mx-auto px-4 py-24">
      <header className="mb-20 text-center">
        <h1 className="text-5xl font-extrabold text-slate-900 mb-4 tracking-tight">Project Timeline</h1>
        <div className="h-1.5 w-24 bg-blue-600 mx-auto rounded-full"></div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-24">
        {[
          { 
            title: 'SecondHandBooks', 
            date: '2026.01 ~ 2026.02', 
            desc: '안전 결제 기반 Escrow 중고책 거래 플랫폼',
            role: '6인 프로젝트',
            tags: ['Spring', 'MyBatis', 'AWS', 'Redis'],
            img: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800&auto=format&fit=crop'
          },
          { 
            title: 'TripToN', 
            date: '2025.08 ~ 2025.09', 
            desc: '사용자 고민 AI 상담 서비스 (Gemini API)',
            role: '2인 프로젝트',
            tags: ['Spring Boot', 'Gemini AI', 'JPA', 'Docker'],
            img: 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&auto=format&fit=crop'
          },
          { 
            title: 'STO Project', 
            date: '2026.03 ~ 2026.04', 
            desc: 'STO 조각 투자 토큰 증권 PoC (신한DS 우수상 수여)',
            role: '5인 프로젝트',
            tags: ['Blockchain', 'PostgreSQL', 'WebSocket'],
            img: 'https://images.unsplash.com/photo-1639762681485-074b7f938ba0?w=800&auto=format&fit=crop'
          },
        ].map(proj => (
          <motion.div 
            key={proj.title}
            whileHover={{ y: -10 }}
            className="group bg-white rounded-[32px] border border-slate-100 overflow-hidden hover:shadow-2xl transition-all duration-500"
          >
            <div className="aspect-video relative overflow-hidden">
              <img src={proj.img} alt={proj.title} className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" />
              <div className="absolute inset-0 bg-slate-900/10 group-hover:bg-slate-900/0 transition-colors" />
              <span className="absolute top-4 left-4 bg-white/90 backdrop-blur-sm px-3 py-1 rounded-full text-[10px] font-bold text-slate-900 uppercase tracking-widest leading-none">
                {proj.role}
              </span>
            </div>
            <div className="p-8">
              <p className="text-xs font-bold text-blue-600 uppercase tracking-widest mb-3">{proj.date}</p>
              <h3 className="text-2xl font-bold text-slate-900 mb-3 group-hover:text-blue-600 transition-colors">{proj.title}</h3>
              <p className="text-slate-500 text-sm leading-relaxed mb-6">{proj.desc}</p>
              <div className="flex flex-wrap gap-2">
                {proj.tags.map(t => (
                  <span key={t} className="px-2.5 py-1 rounded-lg bg-slate-100 text-slate-600 text-[10px] font-bold border border-slate-200">
                    {t}
                  </span>
                ))}
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
