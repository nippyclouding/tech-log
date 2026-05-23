package com.nippyclouding.tech_log_back.admin.controller;

import java.util.Collection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminConsoleController {

    private final String frontendOrigin;

    public AdminConsoleController(@Value("${app.frontend-origin:http://localhost:3000}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @GetMapping(value = "/admin-console", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> console(
            Authentication authentication,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout
    ) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.ok(loginPage(error != null, logout != null));
        }
        return ResponseEntity.ok(consolePage(authentication.getName()));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String loginPage(boolean error, boolean logout) {
        String message = "";
        if (error) {
            message = "<p class=\"error\">ID 또는 PW가 올바르지 않습니다.</p>";
        }
        if (logout) {
            message = "<p class=\"success\">로그아웃되었습니다.</p>";
        }
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Admin Console</title>
                  <style>
                    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f8fafc; color: #0f172a; }
                    main { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
                    form { width: 100%; max-width: 380px; background: white; border: 1px solid #e2e8f0; border-radius: 12px; padding: 28px; box-shadow: 0 18px 45px rgba(15, 23, 42, .08); }
                    h1 { margin: 0 0 20px; font-size: 24px; }
                    label { display: block; margin: 14px 0 6px; font-size: 13px; font-weight: 700; color: #475569; }
                    input { width: 100%; box-sizing: border-box; border: 1px solid #cbd5e1; border-radius: 8px; padding: 12px; font-size: 14px; }
                    button { width: 100%; margin-top: 20px; border: 0; border-radius: 8px; padding: 12px; background: #0f172a; color: white; font-weight: 800; cursor: pointer; }
                    .error { color: #dc2626; font-size: 13px; font-weight: 700; }
                    .success { color: #16a34a; font-size: 13px; font-weight: 700; }
                  </style>
                </head>
                <body>
                  <main>
                    <form method="post" action="/admin-console/login">
                      <h1>Admin Console</h1>
                      {{MESSAGE}}
                      <label for="adminId">ID</label>
                      <input id="adminId" name="adminId" autocomplete="username" required>
                      <label for="adminPassword">Password</label>
                      <input id="adminPassword" name="adminPassword" type="password" autocomplete="current-password" required>
                      <button type="submit">Login</button>
                    </form>
                  </main>
                </body>
                </html>
                """.replace("{{MESSAGE}}", message);
    }

    private String consolePage(String adminId) {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Admin Console</title>
                  <style>
                    * { box-sizing: border-box; }
                    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f8fafc; color: #0f172a; }
                    .shell { min-height: 100vh; display: grid; grid-template-columns: 240px 1fr; }
                    .shell > aside { background: #0f172a; color: white; padding: 20px 14px; }
                    .brand { padding: 4px 10px 20px; border-bottom: 1px solid rgba(255,255,255,.12); margin-bottom: 16px; }
                    .brand strong { display: block; font-size: 18px; }
                    .brand span { color: #94a3b8; font-size: 12px; }
                    nav { display: grid; gap: 6px; }
                    nav button { width: 100%; text-align: left; border: 0; border-radius: 8px; padding: 11px 12px; background: transparent; color: #cbd5e1; font-weight: 800; cursor: pointer; }
                    nav button.active, nav button:hover { background: #1e293b; color: white; }
                    .logout { margin-top: 20px; }
                    .logout button { width: 100%; border: 1px solid rgba(255,255,255,.18); border-radius: 8px; padding: 10px; background: transparent; color: white; font-weight: 800; cursor: pointer; }
                    main { padding: 28px 32px 60px; min-width: 0; }
                    .topbar { display: flex; justify-content: space-between; gap: 16px; align-items: center; margin-bottom: 20px; }
                    .top-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; justify-content: flex-end; }
                    .section-subnav { display: none; gap: 6px; padding: 4px; border: 1px solid #e2e8f0; border-radius: 10px; background: white; }
                    .section-subnav.active { display: flex; }
                    .section-subnav button { background: transparent; color: #475569; border-radius: 7px; padding: 8px 11px; }
                    .section-subnav button.active, .section-subnav button:hover { background: #0f172a; color: white; }
                    .panel { background: white; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; box-shadow: 0 12px 30px rgba(15, 23, 42, .04); }
                    .composer-panel { max-width: 1280px; margin: 0 auto; padding: 0; overflow: hidden; }
                    .composer-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 24px 28px 16px; border-bottom: 1px solid #f1f5f9; }
                    .composer-header h2 { margin-bottom: 5px; font-size: 22px; }
                    .draft-state { flex-shrink: 0; padding: 8px 12px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-size: 12px; font-weight: 800; }
                    .composer-title { padding: 10px 28px 18px; border-bottom: 1px solid #f1f5f9; }
                    .composer-title input { border: 0; padding: 14px 0 10px; border-radius: 0; font-size: 32px; font-weight: 800; letter-spacing: -.04em; color: #0f172a; }
                    .composer-title input:focus { outline: 0; }
                    .composer-title input::placeholder { color: #cbd5e1; }
                    .composer-title .muted { text-align: right; }
                    .composer-body { display: grid; grid-template-columns: minmax(420px, 1fr) 300px; min-height: 620px; }
                    .writing-area { min-width: 0; border-right: 1px solid #f1f5f9; }
                    .format-toolbar { position: sticky; top: 0; z-index: 2; display: flex; align-items: center; flex-wrap: wrap; gap: 5px; padding: 11px 16px; border-bottom: 1px solid #e2e8f0; background: rgba(255,255,255,.95); }
                    .format-toolbar button { height: 34px; min-width: 35px; padding: 0 10px; background: white; border: 1px solid #e2e8f0; color: #334155; border-radius: 7px; font-size: 13px; }
                    .format-toolbar button:hover, .format-toolbar button.active { background: #eff6ff; border-color: #bfdbfe; color: #1d4ed8; }
                    .format-toolbar .separator { width: 1px; height: 22px; margin: 0 4px; background: #e2e8f0; }
                    .format-toolbar .view-switch { display: flex; gap: 4px; margin-left: auto; }
                    .editor-surface textarea { min-height: 522px; border: 0; border-radius: 0; resize: vertical; padding: 30px 34px; font: 16px/1.9 -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color: #1e293b; }
                    .editor-surface textarea:focus { outline: 0; }
                    .editor-surface textarea::placeholder { color: #94a3b8; }
                    .markdown-preview { display: none; min-height: 522px; padding: 28px 38px 52px; color: #334155; font-size: 16px; line-height: 1.85; }
                    .markdown-preview.active { display: block; }
                    .markdown-preview h1, .markdown-preview h2, .markdown-preview h3 { margin: 1.3em 0 .55em; letter-spacing: -.035em; color: #0f172a; }
                    .markdown-preview h1 { font-size: 30px; }
                    .markdown-preview h2 { font-size: 25px; }
                    .markdown-preview h3 { font-size: 21px; }
                    .markdown-preview p { margin: 0 0 1.05em; }
                    .markdown-preview blockquote { margin: 18px 0; padding: 2px 18px; border-left: 3px solid #60a5fa; color: #64748b; }
                    .markdown-preview pre { padding: 16px; border-radius: 10px; overflow: auto; background: #0f172a; color: #e2e8f0; }
                    .markdown-preview code { border-radius: 5px; padding: 2px 5px; background: #f1f5f9; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 14px; }
                    .markdown-preview pre code { padding: 0; background: transparent; }
                    .markdown-preview img { display: block; max-width: 100%; margin: 22px auto; border-radius: 13px; }
                    .markdown-preview a { color: #2563eb; }
                    .markdown-preview .image-placeholder { display: block; margin: 16px 0; border: 1px dashed #bfdbfe; border-radius: 9px; padding: 26px 16px; color: #2563eb; text-align: center; background: #eff6ff; }
                    .composer-meta { padding: 23px 20px; background: #fbfdff; color: #0f172a; }
                    .meta-block { padding-bottom: 20px; margin-bottom: 18px; border-bottom: 1px solid #e2e8f0; }
                    .meta-block:last-of-type { border-bottom: 0; }
                    .meta-block h3 { margin: 0 0 11px; font-size: 14px; }
                    .meta-block label { margin-top: 0; }
                    .meta-hint { margin: 8px 0 0; color: #64748b; font-size: 12px; line-height: 1.55; }
                    .file-picker { border: 1px dashed #cbd5e1; border-radius: 9px; padding: 12px; background: white; }
                    .file-picker input { padding: 0; border: 0; font-size: 12px; }
                    .composer-actions { display: grid; gap: 8px; }
                    .composer-actions button { width: 100%; min-height: 42px; }
                    .tab { display: none; }
                    .tab.active { display: grid; gap: 20px; }
                    .subtab { display: none; }
                    .subtab.active { display: grid; gap: 20px; }
                    h1, h2, h3 { margin-top: 0; }
                    label { display: block; margin: 12px 0 6px; font-size: 13px; font-weight: 800; color: #475569; }
                    input, textarea, select { width: 100%; border: 1px solid #cbd5e1; border-radius: 8px; padding: 10px 11px; font-size: 14px; background: white; }
                    select[multiple] { min-height: 112px; }
                    textarea { min-height: 220px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
                    button, a.button { display: inline-flex; align-items: center; justify-content: center; border: 0; border-radius: 8px; padding: 9px 13px; background: #0f172a; color: white; font-weight: 800; cursor: pointer; text-decoration: none; }
                    button.secondary { background: #e2e8f0; color: #0f172a; }
                    button.danger { background: #dc2626; }
                    button.ghost { background: transparent; color: #2563eb; padding: 0; }
                    .grid { display: grid; grid-template-columns: minmax(320px, 420px) 1fr; gap: 20px; align-items: start; }
                    .stack { display: grid; gap: 20px; }
                    .row { display: grid; grid-template-columns: 1fr auto; gap: 10px; align-items: center; padding: 12px 0; border-bottom: 1px solid #f1f5f9; }
                    .list { display: grid; gap: 0; }
                    .actions { display: flex; flex-wrap: wrap; gap: 8px; }
                    .editor-tools { display: flex; gap: 8px; align-items: center; }
                    .editor-tools input { flex: 1; }
                    .selected-categories { display: flex; flex-wrap: wrap; gap: 8px; min-height: 34px; padding: 8px; border: 1px dashed #cbd5e1; border-radius: 8px; background: #f8fafc; }
                    .chip { display: inline-flex; align-items: center; gap: 6px; padding: 6px 8px; border-radius: 999px; background: #dbeafe; color: #1e40af; font-size: 12px; font-weight: 800; }
                    .chip button { padding: 0; background: transparent; color: #1e40af; line-height: 1; }
                    .table { width: 100%; border-collapse: collapse; font-size: 13px; }
                    .table th, .table td { text-align: left; border-bottom: 1px solid #f1f5f9; padding: 10px; vertical-align: top; }
                    .table th { color: #64748b; font-size: 11px; text-transform: uppercase; letter-spacing: .05em; }
                    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
                    .muted { color: #64748b; font-size: 13px; }
                    .status { min-height: 20px; font-size: 13px; font-weight: 700; color: #2563eb; }
                    .pager { display: flex; gap: 8px; align-items: center; justify-content: flex-end; margin-top: 14px; }
                    @media (max-width: 900px) {
                      .shell { grid-template-columns: 1fr; }
                      .shell > aside { position: static; }
                      .grid { grid-template-columns: 1fr; }
                      main { padding: 18px 14px 42px; }
                      .composer-header, .composer-title { padding-left: 18px; padding-right: 18px; }
                      .composer-title input { font-size: 26px; }
                      .composer-body { grid-template-columns: 1fr; }
                      .writing-area { border-right: 0; }
                      .composer-meta { border-top: 1px solid #e2e8f0; }
                      .editor-surface textarea, .markdown-preview { min-height: 400px; padding: 22px 20px; }
                    }
                  </style>
                </head>
                <body>
                  <div class="shell">
                    <aside>
                      <div class="brand">
                        <strong>Admin Console</strong>
                        <span>{{ADMIN_ID}}</span>
                      </div>
                      <nav>
                        <button class="active" data-tab="posts">게시글</button>
                        <button data-tab="categories">카테고리</button>
                        <button data-tab="comments">댓글</button>
                        <button data-tab="logs">로그</button>
                      </nav>
                      <form class="logout" method="post" action="/admin-console/logout"><button type="submit">Logout</button></form>
                    </aside>
                    <main>
                      <div class="topbar">
                        <div>
                          <h1 id="pageTitle">게시글</h1>
                          <p class="muted">게시글, 카테고리, 댓글, 로그를 관리합니다.</p>
                        </div>
                        <div class="top-actions">
                          <div id="postSubnav" class="section-subnav active" aria-label="게시글 메뉴">
                            <button type="button" class="active" data-post-subtab="create">게시글 작성</button>
                            <button type="button" data-post-subtab="manage">게시글 편집</button>
                          </div>
                          <a class="button" href="{{FRONTEND_ORIGIN}}" target="_blank" rel="noopener noreferrer">사이트 보기</a>
                        </div>
                      </div>

                      <section id="tab-posts" class="tab active">
                        <div id="post-subtab-create" class="subtab active">
                          <div class="panel composer-panel">
                            <div class="composer-header">
                              <div>
                                <h2 id="postFormTitle">새 글 쓰기</h2>
                                <span class="muted">마크다운으로 작성하고 발행 전 모습을 미리 확인할 수 있습니다.</span>
                              </div>
                              <span id="createDraftState" class="draft-state">임시 저장 대기</span>
                            </div>
                            <form id="postCreateForm" enctype="multipart/form-data">
                              <div class="composer-title">
                                <input name="title" aria-label="제목" placeholder="제목을 입력하세요" required>
                                <p id="createTitleCount" class="muted">0 / 255</p>
                              </div>
                              <div class="composer-body">
                                <div class="writing-area">
                                  <div class="format-toolbar" aria-label="본문 서식 도구">
                                    <button type="button" title="제목" onclick="formatContent('create', 'heading')"><strong>H2</strong></button>
                                    <button type="button" title="굵게" onclick="formatContent('create', 'bold')"><strong>B</strong></button>
                                    <button type="button" title="인용문" onclick="formatContent('create', 'quote')">인용</button>
                                    <button type="button" title="목록" onclick="formatContent('create', 'list')">목록</button>
                                    <button type="button" title="코드 블록" onclick="formatContent('create', 'code')">코드</button>
                                    <button type="button" title="링크" onclick="formatContent('create', 'link')">링크</button>
                                    <button type="button" title="구분선" onclick="formatContent('create', 'divider')">-</button>
                                    <span class="separator"></span>
                                    <button type="button" onclick="insertSelectedImages('create')">이미지 삽입</button>
                                    <div class="view-switch">
                                      <button id="createWriteButton" class="active" type="button" onclick="setEditorView('create', 'write')">편집</button>
                                      <button id="createPreviewButton" type="button" onclick="setEditorView('create', 'preview')">미리보기</button>
                                    </div>
                                  </div>
                                  <div class="editor-surface">
                                    <textarea name="content" aria-label="본문" placeholder="내용을 입력하세요. 드래그한 문장에 툴바의 서식을 적용할 수 있습니다." required></textarea>
                                    <article id="createPreview" class="markdown-preview"></article>
                                  </div>
                                </div>
                                <aside class="composer-meta">
                                  <div class="meta-block">
                                    <h3>카테고리</h3>
                                    <div class="editor-tools">
                                      <select id="createCategoryPicker" aria-label="카테고리 선택"></select>
                                      <button type="button" class="secondary" onclick="addSelectedCategory('create')">추가</button>
                                    </div>
                                    <div id="createSelectedCategories" class="selected-categories">
                                      <span class="muted">선택된 카테고리가 없습니다.</span>
                                    </div>
                                  </div>
                                  <div class="meta-block">
                                    <h3>본문 이미지</h3>
                                    <div class="file-picker">
                                      <input id="createPostImages" name="images" type="file" accept="image/*" multiple>
                                    </div>
                                    <p class="meta-hint">파일을 고른 뒤 커서를 놓고 상단의 이미지 삽입을 누르면 본문에 배치됩니다.</p>
                                  </div>
                                  <div class="meta-block">
                                    <h3>작성 정보</h3>
                                    <p id="createContentCount" class="meta-hint">본문 0자 · 단어 0개</p>
                                    <p class="meta-hint">새 글은 이 브라우저에 자동으로 임시 저장됩니다.</p>
                                  </div>
                                  <p id="createPostStatus" class="status"></p>
                                  <div class="composer-actions">
                                    <button type="submit">게시글 발행</button>
                                    <button type="button" class="secondary" onclick="resetCreatePostForm(true)">내용 비우기</button>
                                  </div>
                                </aside>
                              </div>
                            </form>
                          </div>
                        </div>
                        <div id="post-subtab-manage" class="subtab">
                          <div id="postEditPanel" class="panel composer-panel" style="display: none;">
                            <div class="composer-header">
                              <div>
                                <h2>게시글 수정</h2>
                                <span class="muted">기존 글의 마크다운을 편집합니다. 새 이미지를 업로드하면 대표 이미지 목록이 교체됩니다.</span>
                              </div>
                            </div>
                            <form id="postEditForm" enctype="multipart/form-data">
                              <input type="hidden" name="postId">
                              <div class="composer-title">
                                <input name="title" aria-label="제목" placeholder="제목을 입력하세요" required>
                                <p id="editTitleCount" class="muted">0 / 255</p>
                              </div>
                              <div class="composer-body">
                                <div class="writing-area">
                                  <div class="format-toolbar" aria-label="본문 서식 도구">
                                    <button type="button" title="제목" onclick="formatContent('edit', 'heading')"><strong>H2</strong></button>
                                    <button type="button" title="굵게" onclick="formatContent('edit', 'bold')"><strong>B</strong></button>
                                    <button type="button" title="인용문" onclick="formatContent('edit', 'quote')">인용</button>
                                    <button type="button" title="목록" onclick="formatContent('edit', 'list')">목록</button>
                                    <button type="button" title="코드 블록" onclick="formatContent('edit', 'code')">코드</button>
                                    <button type="button" title="링크" onclick="formatContent('edit', 'link')">링크</button>
                                    <button type="button" title="구분선" onclick="formatContent('edit', 'divider')">-</button>
                                    <span class="separator"></span>
                                    <button type="button" onclick="insertSelectedImages('edit')">이미지 삽입</button>
                                    <div class="view-switch">
                                      <button id="editWriteButton" class="active" type="button" onclick="setEditorView('edit', 'write')">편집</button>
                                      <button id="editPreviewButton" type="button" onclick="setEditorView('edit', 'preview')">미리보기</button>
                                    </div>
                                  </div>
                                  <div class="editor-surface">
                                    <textarea name="content" aria-label="본문" required></textarea>
                                    <article id="editPreview" class="markdown-preview"></article>
                                  </div>
                                </div>
                                <aside class="composer-meta">
                                  <div class="meta-block">
                                    <h3>카테고리</h3>
                                    <div class="editor-tools">
                                      <select id="editCategoryPicker" aria-label="카테고리 선택"></select>
                                      <button type="button" class="secondary" onclick="addSelectedCategory('edit')">추가</button>
                                    </div>
                                    <div id="editSelectedCategories" class="selected-categories">
                                      <span class="muted">선택된 카테고리가 없습니다.</span>
                                    </div>
                                  </div>
                                  <div class="meta-block">
                                    <h3>새 본문 이미지</h3>
                                    <div class="file-picker">
                                      <input id="editPostImages" name="images" type="file" accept="image/*" multiple>
                                    </div>
                                    <p class="meta-hint">추가 파일을 본문에 배치하려면 상단 이미지 삽입을 사용하세요.</p>
                                  </div>
                                  <div class="meta-block">
                                    <h3>작성 정보</h3>
                                    <p id="editContentCount" class="meta-hint">본문 0자 · 단어 0개</p>
                                  </div>
                                  <p id="editPostStatus" class="status"></p>
                                  <div class="composer-actions">
                                    <button type="submit">변경 사항 저장</button>
                                    <button type="button" class="secondary" onclick="resetEditPostForm()">닫기</button>
                                  </div>
                                </aside>
                              </div>
                            </form>
                          </div>
                          <div class="panel">
                            <h2>게시글 편집</h2>
                            <div id="posts" class="list muted">Loading...</div>
                          </div>
                        </div>
                      </section>

                      <section id="tab-categories" class="tab">
                        <div class="grid">
                          <div class="panel">
                            <h2 id="categoryFormTitle">카테고리 생성</h2>
                            <form id="categoryForm">
                              <input type="hidden" name="categoryId">
                              <label>Name</label>
                              <input name="name" required>
                              <p id="categoryStatus" class="status"></p>
                              <div class="actions">
                                <button type="submit">저장</button>
                                <button type="button" class="secondary" onclick="resetCategoryForm()">초기화</button>
                              </div>
                            </form>
                          </div>
                          <div class="panel">
                            <h2>카테고리 목록</h2>
                            <div id="categories" class="list muted">Loading...</div>
                          </div>
                        </div>
                      </section>

                      <section id="tab-comments" class="tab">
                        <div class="panel">
                          <h2>댓글 목록</h2>
                          <p class="muted">생성 순으로 10개씩 조회합니다. 댓글을 누르면 해당 게시글의 댓글 위치로 이동합니다.</p>
                          <div id="comments">Loading...</div>
                          <div class="pager">
                            <button class="secondary" onclick="loadComments(commentPage - 1)">이전</button>
                            <span id="commentPageLabel" class="muted"></span>
                            <button class="secondary" onclick="loadComments(commentPage + 1)">다음</button>
                          </div>
                        </div>
                      </section>

                      <section id="tab-logs" class="tab">
                        <div class="panel">
                          <h2>접근 로그</h2>
                          <div id="accessLogs">Loading...</div>
                          <div class="pager">
                            <button class="secondary" onclick="loadAccessLogs(accessLogPage - 1)">이전</button>
                            <span id="accessLogPageLabel" class="muted"></span>
                            <button class="secondary" onclick="loadAccessLogs(accessLogPage + 1)">다음</button>
                          </div>
                        </div>
                        <div class="panel">
                          <h2>로그인 이력</h2>
                          <div id="loginLogs">Loading...</div>
                          <div class="pager">
                            <button class="secondary" onclick="loadLoginLogs(loginLogPage - 1)">이전</button>
                            <span id="loginLogPageLabel" class="muted"></span>
                            <button class="secondary" onclick="loadLoginLogs(loginLogPage + 1)">다음</button>
                          </div>
                        </div>
                      </section>
                    </main>
                  </div>
                  <script>
                    const jsonHeaders = { 'Content-Type': 'application/json' };
                    const createDraftKey = 'tech-log.admin.post-draft';
                    let categoriesCache = [];
                    let selectedCreateCategories = [];
                    let selectedEditCategories = [];
                    let commentPage = 0;
                    let accessLogPage = 0;
                    let loginLogPage = 0;

                    document.querySelectorAll('nav button').forEach(button => {
                      button.addEventListener('click', () => {
                        document.querySelectorAll('nav button').forEach(item => item.classList.remove('active'));
                        document.querySelectorAll('.tab').forEach(item => item.classList.remove('active'));
                        button.classList.add('active');
                        document.getElementById('tab-' + button.dataset.tab).classList.add('active');
                        document.getElementById('pageTitle').textContent = button.textContent;
                        document.getElementById('postSubnav').classList.toggle('active', button.dataset.tab === 'posts');
                        if (button.dataset.tab === 'posts') loadPosts();
                        if (button.dataset.tab === 'categories') loadCategories();
                        if (button.dataset.tab === 'comments') loadComments(0);
                        if (button.dataset.tab === 'logs') { loadAccessLogs(0); loadLoginLogs(0); }
                      });
                    });

                    document.querySelectorAll('[data-post-subtab]').forEach(button => {
                      button.addEventListener('click', () => showPostSubtab(button.dataset.postSubtab));
                    });

                    document.getElementById('postCreateForm').addEventListener('submit', async (event) => {
                      event.preventDefault();
                      await submitPostForm(event.target, null, selectedCreateCategories, 'createPostStatus');
                    });

                    document.getElementById('postEditForm').addEventListener('submit', async (event) => {
                      event.preventDefault();
                      const postId = event.target.elements.namedItem('postId').value;
                      await submitPostForm(event.target, postId, selectedEditCategories, 'editPostStatus');
                    });

                    async function submitPostForm(target, postId, selectedCategories, statusId) {
                      const form = new FormData(target);
                      form.delete('categories');
                      selectedCategories.forEach(category => form.append('categories', category));
                      if (selectedCategories.length === 0) {
                        document.getElementById(statusId).textContent = '카테고리를 최소 1개 선택하세요.';
                        return;
                      }
                      const method = postId ? 'PUT' : 'POST';
                      const url = postId ? '/api/admin/posts/' + postId : '/api/admin/posts';
                      const response = await fetch(url, { method, body: form });
                      if (response.ok) {
                        document.getElementById(statusId).textContent = '게시글이 저장되었습니다.';
                        if (postId) {
                          resetEditPostForm();
                          showPostSubtab('manage');
                        } else {
                          resetCreatePostForm(false);
                        }
                        await loadPosts();
                      } else {
                        document.getElementById(statusId).textContent = await errorMessage(response, '게시글 저장에 실패했습니다.');
                      }
                    }

                    document.getElementById('categoryForm').addEventListener('submit', async (event) => {
                      event.preventDefault();
                      const form = new FormData(event.target);
                      const categoryId = form.get('categoryId');
                      const response = await fetch(categoryId ? '/api/admin/categories/' + categoryId : '/api/admin/categories', {
                        method: categoryId ? 'PUT' : 'POST',
                        headers: jsonHeaders,
                        body: JSON.stringify({ name: form.get('name') })
                      });
                      if (response.ok) {
                        document.getElementById('categoryStatus').textContent = '카테고리가 저장되었습니다.';
                        resetCategoryForm();
                        loadCategories();
                      } else {
                        const message = await errorMessage(response, categoryId ? '존재하지 않는 카테고리입니다.' : '카테고리 저장에 실패했습니다.');
                        document.getElementById('categoryStatus').textContent = message;
                        alert(message);
                      }
                    });

                    async function loadPosts() {
                      const response = await fetch('/api/posts?size=100');
                      const data = await response.json();
                      document.getElementById('posts').innerHTML = data.content.map(post => `
                        <div class="row">
                          <div>
                            <strong>${escapeHtml(post.title)}</strong>
                            <div class="muted">${escapeHtml(post.category)} · ${post.date}</div>
                          </div>
                          <div class="actions">
                            <button class="secondary" onclick="editPost(${post.id})">수정</button>
                            <button class="danger" onclick="deletePost(${post.id})">삭제</button>
                          </div>
                        </div>`).join('') || '게시글이 없습니다.';
                    }

                    async function editPost(id) {
                      const response = await fetch('/api/posts/' + id);
                      const post = await response.json();
                      const form = document.getElementById('postEditForm');
                      form.elements.namedItem('postId').value = post.id;
                      form.elements.namedItem('title').value = post.title;
                      form.elements.namedItem('content').value = post.content;
                      selectedEditCategories = post.tags.filter(tag => categoriesCache.some(category => category.name === tag));
                      renderSelectedCategories('edit');
                      updateComposer('edit');
                      setEditorView('edit', 'write');
                      document.getElementById('postEditPanel').style.display = 'block';
                      document.getElementById('editPostStatus').textContent = '수정할 내용을 입력하세요.';
                      showPostSubtab('manage');
                      document.getElementById('postEditPanel').scrollIntoView({ behavior: 'smooth', block: 'start' });
                    }

                    async function deletePost(id) {
                      if (!confirm('삭제할까요?')) return;
                      const response = await fetch('/api/admin/posts/' + id, { method: 'DELETE' });
                      if (!response.ok) alert(await errorMessage(response, '게시글 삭제에 실패했습니다.'));
                      loadPosts();
                    }

                    async function loadCategories() {
                      const response = await fetch('/api/categories');
                      categoriesCache = await response.json();
                      selectedCreateCategories = selectedCreateCategories.filter(name => categoriesCache.some(category => category.name === name));
                      renderCategorySelect();
                      document.getElementById('categories').innerHTML = categoriesCache.map(category => `
                        <div class="row">
                          <div><strong>${escapeHtml(category.name)}</strong><div class="muted">/${escapeHtml(category.slug)}</div></div>
                          <div class="actions">
                            <button class="secondary" onclick="editCategory(${category.id})">수정</button>
                            <button class="danger" onclick="deleteCategory(${category.id})">삭제</button>
                          </div>
                        </div>`).join('') || '카테고리가 없습니다.';
                    }

                    function renderCategorySelect() {
                      const options = '<option value="">카테고리 선택</option>' + categoriesCache
                        .map(category => `<option value="${escapeAttr(category.name)}">${escapeHtml(category.name)}</option>`)
                        .join('');
                      document.getElementById('createCategoryPicker').innerHTML = options;
                      document.getElementById('editCategoryPicker').innerHTML = options;
                      renderSelectedCategories('create');
                      renderSelectedCategories('edit');
                    }

                    function addSelectedCategory(mode) {
                      const picker = document.getElementById(mode + 'CategoryPicker');
                      const value = picker.value;
                      if (!value) return;
                      const selectedCategories = postCategories(mode);
                      if (!selectedCategories.includes(value)) {
                        selectedCategories.push(value);
                        renderSelectedCategories(mode);
                        persistCreateDraft(mode);
                      }
                      picker.value = '';
                    }

                    function removeSelectedCategory(mode, categoryName) {
                      if (mode === 'create') {
                        selectedCreateCategories = selectedCreateCategories.filter(name => name !== categoryName);
                      } else {
                        selectedEditCategories = selectedEditCategories.filter(name => name !== categoryName);
                      }
                      renderSelectedCategories(mode);
                      persistCreateDraft(mode);
                    }

                    function renderSelectedCategories(mode) {
                      const box = document.getElementById(mode + 'SelectedCategories');
                      const selectedCategories = postCategories(mode);
                      if (!selectedCategories.length) {
                        box.innerHTML = '<span class="muted">선택된 카테고리가 없습니다.</span>';
                        return;
                      }
                      box.innerHTML = selectedCategories
                        .map(category => `<span class="chip">${escapeHtml(category)} <button type="button" onclick="removeSelectedCategory('${mode}', '${escapeAttr(category)}')">×</button></span>`)
                        .join('');
                    }

                    function postCategories(mode) {
                      return mode === 'create' ? selectedCreateCategories : selectedEditCategories;
                    }

                    function postForm(mode) {
                      return document.getElementById(mode === 'create' ? 'postCreateForm' : 'postEditForm');
                    }

                    function postField(mode, name) {
                      return postForm(mode).elements.namedItem(name);
                    }

                    function initializeComposer(mode) {
                      postField(mode, 'title').addEventListener('input', () => {
                        updateComposer(mode);
                        persistCreateDraft(mode);
                      });
                      postField(mode, 'content').addEventListener('input', () => {
                        updateComposer(mode);
                        persistCreateDraft(mode);
                      });
                      postField(mode, 'content').addEventListener('keydown', event => {
                        if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'b') {
                          event.preventDefault();
                          formatContent(mode, 'bold');
                        }
                      });
                      updateComposer(mode);
                    }

                    function updateComposer(mode) {
                      const titleLength = postField(mode, 'title').value.length;
                      const content = postField(mode, 'content').value;
                      const words = content.trim() ? content.trim().split(/\\s+/).length : 0;
                      document.getElementById(mode + 'TitleCount').textContent = titleLength + ' / 255';
                      document.getElementById(mode + 'ContentCount').textContent = '본문 ' + content.length + '자 · 단어 ' + words + '개';
                      document.getElementById(mode + 'Preview').innerHTML = renderMarkdown(content);
                    }

                    function setEditorView(mode, view) {
                      const preview = document.getElementById(mode + 'Preview');
                      const previewMode = view === 'preview';
                      postField(mode, 'content').style.display = previewMode ? 'none' : 'block';
                      preview.classList.toggle('active', previewMode);
                      document.getElementById(mode + 'WriteButton').classList.toggle('active', !previewMode);
                      document.getElementById(mode + 'PreviewButton').classList.toggle('active', previewMode);
                      if (previewMode) updateComposer(mode);
                    }

                    function formatContent(mode, action) {
                      setEditorView(mode, 'write');
                      const textarea = postField(mode, 'content');
                      const selected = textarea.value.slice(textarea.selectionStart, textarea.selectionEnd);
                      let replacement = selected;
                      if (action === 'heading') replacement = '## ' + (selected || '소제목');
                      if (action === 'bold') replacement = '**' + (selected || '강조할 내용') + '**';
                      if (action === 'quote') replacement = (selected || '인용문').split('\\n').map(line => '> ' + line).join('\\n');
                      if (action === 'list') replacement = (selected || '목록 항목').split('\\n').map(line => '- ' + line).join('\\n');
                      if (action === 'code') replacement = '\\n\\n```\\n' + (selected || '코드를 입력하세요') + '\\n```\\n\\n';
                      if (action === 'link') replacement = '[' + (selected || '링크 텍스트') + '](https://)';
                      if (action === 'divider') replacement = '\\n\\n---\\n\\n';
                      textarea.setRangeText(replacement, textarea.selectionStart, textarea.selectionEnd, 'end');
                      textarea.focus();
                      updateComposer(mode);
                      persistCreateDraft(mode);
                    }

                    function persistCreateDraft(mode) {
                      if (mode !== 'create') return;
                      const draft = {
                        title: postField('create', 'title').value,
                        content: postField('create', 'content').value,
                        categories: selectedCreateCategories
                      };
                      try {
                        localStorage.setItem(createDraftKey, JSON.stringify(draft));
                        document.getElementById('createDraftState').textContent = '임시 저장됨';
                      } catch (error) {
                        document.getElementById('createDraftState').textContent = '임시 저장 불가';
                      }
                    }

                    function restoreCreateDraft() {
                      try {
                        const stored = localStorage.getItem(createDraftKey);
                        if (!stored) return;
                        const draft = JSON.parse(stored);
                        postField('create', 'title').value = draft.title || '';
                        postField('create', 'content').value = draft.content || '';
                        selectedCreateCategories = Array.isArray(draft.categories) ? draft.categories : [];
                        document.getElementById('createDraftState').textContent = '임시 저장 복원됨';
                      } catch (error) {
                        document.getElementById('createDraftState').textContent = '임시 저장 불가';
                      }
                    }

                    function renderMarkdown(content) {
                      if (!content.trim()) {
                        return '<p class="muted">작성한 글의 미리보기가 여기에 표시됩니다.</p>';
                      }
                      const lines = escapeHtml(content).split('\\n');
                      const html = [];
                      let inCode = false;
                      let inList = false;
                      lines.forEach(line => {
                        if (line.startsWith('```')) {
                          if (inList) { html.push('</ul>'); inList = false; }
                          html.push(inCode ? '</code></pre>' : '<pre><code>');
                          inCode = !inCode;
                          return;
                        }
                        if (inCode) {
                          html.push(line + '\\n');
                          return;
                        }
                        if (/^- /.test(line)) {
                          if (!inList) { html.push('<ul>'); inList = true; }
                          html.push('<li>' + inlineMarkdown(line.slice(2)) + '</li>');
                          return;
                        }
                        if (inList) { html.push('</ul>'); inList = false; }
                        if (!line.trim()) return;
                        if (line === '---') { html.push('<hr>'); return; }
                        if (line.startsWith('### ')) { html.push('<h3>' + inlineMarkdown(line.slice(4)) + '</h3>'); return; }
                        if (line.startsWith('## ')) { html.push('<h2>' + inlineMarkdown(line.slice(3)) + '</h2>'); return; }
                        if (line.startsWith('# ')) { html.push('<h1>' + inlineMarkdown(line.slice(2)) + '</h1>'); return; }
                        if (line.startsWith('&gt; ')) { html.push('<blockquote>' + inlineMarkdown(line.slice(5)) + '</blockquote>'); return; }
                        html.push('<p>' + inlineMarkdown(line) + '</p>');
                      });
                      if (inList) html.push('</ul>');
                      if (inCode) html.push('</code></pre>');
                      return html.join('');
                    }

                    function inlineMarkdown(text) {
                      return text
                        .replace(/\\[이미지: ([^\\]]+)\\]\\((pending-image:[^)]+)\\)/g, (match, alt, url) => safeImage(url, alt))
                        .replace(/!\\[([^\\]]*)\\]\\(([^)]+)\\)/g, (match, alt, url) => safeImage(url, alt))
                        .replace(/\\[([^\\]]+)\\]\\((https?:\\/\\/[^)]+)\\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
                        .replace(/\\*\\*(.+?)\\*\\*/g, '<strong>$1</strong>')
                        .replace(/`([^`]+)`/g, '<code>$1</code>');
                    }

                    function safeImage(url, alt) {
                      if (url.startsWith('pending-image:')) {
                        return '<span class="image-placeholder">업로드 예정 이미지 · ' + alt + '</span>';
                      }
                      if (/^(https?:\\/\\/|\\/)/.test(url)) {
                        return '<img src="' + url + '" alt="' + alt + '">';
                      }
                      return '';
                    }

                    function insertSelectedImages(mode) {
                      const input = document.getElementById(mode + 'PostImages');
                      const textarea = postField(mode, 'content');
                      if (!input.files || input.files.length === 0) {
                        alert('먼저 이미지를 선택하세요.');
                        return;
                      }
                      const markdown = Array.from(input.files)
                        .map((file, index) => `\\n\\n[이미지: ${file.name}](pending-image:${index})\\n\\n`)
                        .join('');
                      const start = textarea.selectionStart;
                      const end = textarea.selectionEnd;
                      textarea.value = textarea.value.slice(0, start) + markdown + textarea.value.slice(end);
                      textarea.focus();
                      textarea.selectionStart = textarea.selectionEnd = start + markdown.length;
                      updateComposer(mode);
                      persistCreateDraft(mode);
                    }

                    function editCategory(id) {
                      const category = categoriesCache.find(item => item.id === id);
                      if (!category) {
                        alert('존재하지 않는 카테고리입니다.');
                        loadCategories();
                        return;
                      }
                      const form = document.getElementById('categoryForm');
                      form.categoryId.value = id;
                      form.name.value = category.name;
                      document.getElementById('categoryFormTitle').textContent = '카테고리 수정';
                    }

                    async function deleteCategory(id) {
                      if (!confirm('카테고리를 삭제할까요? 이미 사용 중이면 실패할 수 있습니다.')) return;
                      const response = await fetch('/api/admin/categories/' + id, { method: 'DELETE' });
                      if (response.ok) {
                        resetCategoryForm();
                        const deletedCategoryName = categoriesCache.find(category => category.id === id)?.name;
                        selectedCreateCategories = selectedCreateCategories.filter(name => name !== deletedCategoryName);
                        selectedEditCategories = selectedEditCategories.filter(name => name !== deletedCategoryName);
                        await loadCategories();
                        document.getElementById('categoryStatus').textContent = '카테고리가 삭제되었습니다.';
                      } else {
                        const message = await errorMessage(response, '존재하지 않는 카테고리입니다.');
                        document.getElementById('categoryStatus').textContent = message;
                        alert(message);
                      }
                    }

                    function resetCreatePostForm(userInitiated) {
                      const form = document.getElementById('postCreateForm');
                      if (userInitiated && (postField('create', 'title').value || postField('create', 'content').value) && !confirm('작성 중인 내용을 모두 비울까요?')) {
                        return;
                      }
                      form.reset();
                      selectedCreateCategories = [];
                      renderSelectedCategories('create');
                      try {
                        localStorage.removeItem(createDraftKey);
                      } catch (error) {
                        document.getElementById('createDraftState').textContent = '임시 저장 불가';
                      }
                      if (userInitiated) document.getElementById('createPostStatus').textContent = '';
                      document.getElementById('createDraftState').textContent = '임시 저장 대기';
                      updateComposer('create');
                      setEditorView('create', 'write');
                    }

                    function resetEditPostForm() {
                      document.getElementById('postEditForm').reset();
                      postField('edit', 'postId').value = '';
                      selectedEditCategories = [];
                      renderSelectedCategories('edit');
                      document.getElementById('postEditPanel').style.display = 'none';
                      document.getElementById('editPostStatus').textContent = '';
                      updateComposer('edit');
                      setEditorView('edit', 'write');
                    }

                    function showPostSubtab(name) {
                      document.querySelectorAll('[data-post-subtab]').forEach(button => {
                        button.classList.toggle('active', button.dataset.postSubtab === name);
                      });
                      document.querySelectorAll('#tab-posts .subtab').forEach(item => item.classList.remove('active'));
                      document.getElementById('post-subtab-' + name).classList.add('active');
                      if (name === 'manage') loadPosts();
                    }

                    function resetCategoryForm() {
                      document.getElementById('categoryForm').reset();
                      document.getElementById('categoryForm').categoryId.value = '';
                      document.getElementById('categoryFormTitle').textContent = '카테고리 생성';
                    }

                    async function loadComments(page) {
                      if (page < 0) return;
                      const response = await fetch('/api/admin/comments?page=' + page + '&size=10');
                      const data = await response.json();
                      commentPage = data.page;
                      document.getElementById('commentPageLabel').textContent = `${data.page + 1} / ${Math.max(data.totalPages, 1)}`;
                      document.getElementById('comments').innerHTML = table(
                        ['게시글', '작성자', '내용', 'IP', '생성일', '이동'],
                        data.content.map(comment => [
                          escapeHtml(comment.postTitle),
                          escapeHtml(comment.authorName),
                          escapeHtml(comment.content),
                          `<span class="mono">${escapeHtml(comment.accessIp)}</span>`,
                          comment.date,
                          `<a href="/post/${comment.postId}#comment-${comment.id}" target="_blank">열기</a>`
                        ])
                      );
                    }

                    async function loadAccessLogs(page) {
                      if (page < 0) return;
                      const response = await fetch('/api/admin/access-logs?page=' + page + '&size=20');
                      const data = await response.json();
                      accessLogPage = data.page;
                      document.getElementById('accessLogPageLabel').textContent = `${data.page + 1} / ${Math.max(data.totalPages, 1)}`;
                      document.getElementById('accessLogs').innerHTML = table(
                        ['IP', 'Method', 'Path', 'Status', 'Time'],
                        data.content.map(log => [
                          `<span class="mono">${escapeHtml(log.ip)}</span>`,
                          log.method,
                          escapeHtml(log.path),
                          log.statusCode,
                          log.timestamp
                        ])
                      );
                    }

                    async function loadLoginLogs(page) {
                      if (page < 0) return;
                      const response = await fetch('/api/admin/login-logs?page=' + page + '&size=20');
                      const data = await response.json();
                      loginLogPage = data.page;
                      document.getElementById('loginLogPageLabel').textContent = `${data.page + 1} / ${Math.max(data.totalPages, 1)}`;
                      document.getElementById('loginLogs').innerHTML = table(
                        ['Provider', 'Login ID', 'IP', 'Time'],
                        data.content.map(log => [
                          log.provider,
                          escapeHtml(log.loginId),
                          `<span class="mono">${escapeHtml(log.ip)}</span>`,
                          log.timestamp
                        ])
                      );
                    }

                    function table(headers, rows) {
                      if (rows.length === 0) return '<p class="muted">데이터가 없습니다.</p>';
                      return `<table class="table"><thead><tr>${headers.map(header => `<th>${header}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${row.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table>`;
                    }

                    function escapeHtml(value) {
                      return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
                    }

                    function escapeAttr(value) {
                      return escapeHtml(value).replace(/`/g, '&#96;');
                    }

                    async function errorMessage(response, fallback) {
                      try {
                        const text = await response.text();
                        const json = JSON.parse(text);
                        return json.message || fallback;
                      } catch (error) {
                        return fallback;
                      }
                    }

                    restoreCreateDraft();
                    initializeComposer('create');
                    initializeComposer('edit');
                    loadCategories();
                    loadPosts();
                  </script>
                </body>
                </html>
                """.replace("{{ADMIN_ID}}", adminId)
                .replace("{{FRONTEND_ORIGIN}}", frontendOrigin);
    }
}
