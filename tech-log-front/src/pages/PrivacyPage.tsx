import { motion } from "motion/react";

export function PrivacyPage() {
  return (
    <div className="container max-w-4xl mx-auto px-4 py-24">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white rounded-[40px] border border-slate-100 p-8 md:p-16 shadow-sm"
      >
        <h1 className="text-4xl font-extrabold text-slate-900 mb-12 text-center tracking-tight">개인정보 처리방침</h1>
        
        <div className="space-y-12 text-slate-700 leading-relaxed">
          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              1. 개인정보의 수집 및 이용 목적
            </h2>
            <p className="mb-4">[Nippycloud’s Tech Log]은(는) 다음의 목적을 위하여 개인정보를 처리합니다. 처리하고 있는 개인정보는 다음의 목적 이외의 용도로는 이용되지 않으며, 이용 목적이 변경되는 경우에는 관련 법령에 따라 별도의 동의를 받는 등 필요한 조치를 이행할 예정입니다.</p>
            <ul className="list-disc pl-5 space-y-2">
              <li className="font-medium text-slate-800">회원 식별 및 서비스 이용: GitHub 로그인을 통한 사용자 식별, 댓글 작성 및 관리, 서비스 부정 이용 방지</li>
              <li className="font-medium text-slate-800">신규 콘텐츠 알림 (선택): 이메일 구독 서비스 신청자에 한해 블로그의 새로운 글, 뉴스레터 발송</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              2. 수집하는 개인정보의 항목
            </h2>
            <p className="mb-4">[Nippycloud’s Tech Log]은(는) 서비스 제공을 위해 아래와 같은 최소한의 개인정보를 수집하고 있습니다.</p>
            <ul className="list-disc pl-5 space-y-2 text-sm">
              <li><span className="font-bold">GitHub 로그인 시 (필수)</span>: GitHub 고유 ID, 이메일 주소, 프로필 이름, 프로필 사진 URL</li>
              <li><span className="font-bold">이메일 구독 신청 시 (선택)</span>: 이메일 주소</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              3. 개인정보의 보유 및 이용 기간
            </h2>
            <p className="mb-4">이용자의 개인정보는 원칙적으로 개인정보의 수집 및 이용 목적이 달성되면 지체 없이 파기합니다. 단, 다음의 정보에 대해서는 아래의 이유로 명시한 기간 동안 보존합니다.</p>
            <ul className="list-disc pl-5 space-y-2 mb-4">
              <li>블로그 회원 정보: 사용자가 GitHub 연동 해제(회원 탈퇴)를 요청할 때까지</li>
              <li>이메일 구독 정보: 사용자가 이메일 수신 거부(구독 취소)를 요청할 때까지</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              4. 개인정보의 파기절차 및 방법
            </h2>
            <p className="mb-4">개인정보 파기절차 및 방법은 다음과 같습니다.</p>
            <ul className="list-disc pl-5 space-y-2">
              <li>파기절차: 이용 목적이 달성된 개인정보는 내부 방침 및 기타 관련 법령에 따라 지체 없이 파기됩니다.</li>
              <li>파기방법: 전자적 파일 형태로 저장된 개인정보는 기록을 재생할 수 없는 기술적 방법을 사용하여 영구 삭제합니다.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              5. 개인정보의 제3자 제공
            </h2>
            <p>[Nippycloud’s Tech Log]은(는) 이용자의 개인정보를 원칙적으로 외부에 제공하지 않습니다. 다만, 법령의 규정에 의거하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요구가 있는 경우에는 예외로 합니다.</p>
          </section>

          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              6. 이용자의 권리와 그 행사 방법
            </h2>
            <p className="mb-4">이용자는 언제든지 등록되어 있는 자신의 개인정보를 조회하거나 수정할 수 있으며, 가입 해지(연동 해제) 및 수신 거부를 요청할 수 있습니다.</p>
            <ul className="list-disc pl-5 space-y-2">
              <li>이메일 수신 거부는 발송된 이메일 하단의 '수신 거부(Unsubscribe)' 링크를 통해 언제든 쉽게 처리할 수 있습니다.</li>
              <li>그 외 개인정보 관련 요청은 아래의 개인정보 보호책임자에게 이메일로 연락해 주시면 지체 없이 조치하겠습니다.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              7. 개인정보 보호책임자 및 연락처
            </h2>
            <p className="mb-4">[Nippycloud’s Tech Log]은(는) 개인정보 처리에 관한 업무를 총괄해서 책임지고, 개인정보 처리와 관련한 이용자의 불만 처리 및 피해 구제 등을 위하여 아래와 같이 개인정보 보호책임자를 지정하고 있습니다.</p>
            <div className="bg-slate-50 p-6 rounded-2xl border border-slate-100">
              <p><span className="font-bold">책임자 (운영자)</span>: 이상원</p>
              <p><span className="font-bold">이메일</span>: nippyclouding@gmail.com</p>
            </div>
          </section>

          <section>
            <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center">
              <span className="w-1.5 h-6 bg-blue-600 rounded-full mr-3" />
              8. 개인정보 처리방침 변경
            </h2>
            <p>이 개인정보 처리방침은 [2026년 6월 1일]부터 적용됩니다.</p>
          </section>
        </div>
      </motion.div>
    </div>
  );
}
