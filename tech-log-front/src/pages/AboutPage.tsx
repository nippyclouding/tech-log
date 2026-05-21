import { motion } from "motion/react";
import { Github } from "lucide-react";

export function AboutPage() {
  return (
    <div className="container max-w-4xl mx-auto px-4 py-24">
      <header className="mb-16 text-center">
        <h1 className="text-5xl font-extrabold text-slate-900 mb-4 tracking-tight">About Me: 이상원</h1>
        <div className="h-1.5 w-24 bg-blue-600 mx-auto rounded-full"></div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
        <div className="space-y-12">
          <section>
            <h2 className="text-2xl font-bold text-slate-900 mb-8 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" /> Education & Training
            </h2>
            <div className="space-y-8 border-l-2 border-slate-100 ml-3 pl-8 relative">
              <div className="relative">
                <div className="absolute -left-[35px] top-1.5 w-3 h-3 bg-blue-600 rounded-full ring-4 ring-white" />
                <p className="text-xs font-bold text-blue-600 uppercase mb-1">2019.03 ~ 2025.02</p>
                <h4 className="font-bold text-slate-900">영남대학교 경제금융학부 졸업</h4>
                <p className="text-sm text-slate-500">컴퓨터공학부 부전공 이수</p>
              </div>
              <div className="relative">
                <div className="absolute -left-[35px] top-1.5 w-3 h-3 bg-slate-200 rounded-full ring-4 ring-white" />
                <p className="text-xs font-bold text-slate-400 uppercase mb-1">2025.11 ~ 2026.04</p>
                <h4 className="font-bold text-slate-900">신한 DS 금융 SW 아카데미 6기</h4>
                <p className="text-sm text-slate-500">STO 프로젝트 - 우수상 (2위)</p>
              </div>
              <div className="relative">
                <div className="absolute -left-[35px] top-1.5 w-3 h-3 bg-slate-200 rounded-full ring-4 ring-white" />
                <p className="text-xs font-bold text-slate-400 uppercase mb-1">2026.04 ~ 2026.06</p>
                <h4 className="font-bold text-slate-900">오픈소스 컨트리뷰션 아카데미</h4>
                <p className="text-sm text-slate-500">egovframe VSCode Initializr 팀</p>
              </div>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold text-slate-900 mb-8 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" /> Certificate
            </h2>
            <ul className="space-y-4">
              {[
                { date: '2025.09', name: '정보처리기사' },
                { date: '2025.03', name: '리눅스마스터 2급' },
                { date: '2024.12', name: 'SQLD' },
                { date: '2024.09', name: 'ADsP' },
              ].map(cert => (
                <li key={cert.name} className="flex items-center justify-between p-4 rounded-2xl bg-white border border-slate-50">
                  <span className="font-bold text-slate-900">{cert.name}</span>
                  <span className="text-sm text-slate-400 font-medium">{cert.date}</span>
                </li>
              ))}
            </ul>
          </section>
        </div>

        <div className="space-y-12">
          <section>
            <h2 className="text-2xl font-bold text-slate-900 mb-8 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" /> Tech Stack
            </h2>
            <div className="space-y-6">
              {[
                { cat: 'Backend', tags: ['Java', 'Spring Boot', 'JPA', 'MyBatis'] },
                { cat: 'Cloud', tags: ['AWS EC2', 'RDS', 'S3', 'CloudFront'] },
                { cat: 'Infra', tags: ['Docker', 'Nginx', 'Redis', 'Kafka'] },
                { cat: 'Database', tags: ['MySQL', 'PostgreSQL', 'MariaDB'] },
              ].map(stack => (
                <div key={stack.cat} className="space-y-3">
                  <h5 className="text-xs font-bold text-slate-400 uppercase tracking-widest">{stack.cat}</h5>
                  <div className="flex flex-wrap gap-2">
                    {stack.tags.map(tag => (
                      <span key={tag} className="px-3 py-1.5 rounded-xl bg-slate-100 text-slate-700 text-sm font-semibold border border-slate-200">
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
