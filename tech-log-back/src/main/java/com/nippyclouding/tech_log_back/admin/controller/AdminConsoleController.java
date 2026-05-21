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

    public AdminConsoleController(@Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin) {
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
                    aside { background: #0f172a; color: white; padding: 20px 14px; }
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
                    .panel { background: white; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; box-shadow: 0 12px 30px rgba(15, 23, 42, .04); }
                    .tab { display: none; }
                    .tab.active { display: grid; gap: 20px; }
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
                      aside { position: static; }
                      .grid { grid-template-columns: 1fr; }
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
                        <a class="button" href="{{FRONTEND_ORIGIN}}" target="_blank" rel="noopener noreferrer">사이트 보기</a>
                      </div>

                      <section id="tab-posts" class="tab active">
                        <div class="stack">
                          <div class="panel">
                            <h2 id="postFormTitle">새로운 게시글 작성</h2>
                            <p class="muted">카테고리는 DB에 저장된 값 중 하나씩 선택해 누적합니다. 이미지는 본문 원하는 위치에 삽입하면 글 중간에 렌더링됩니다.</p>
                            <form id="postForm" enctype="multipart/form-data">
                              <input type="hidden" name="postId">
                              <label>Title</label>
                              <input name="title" required>
                              <label>Categories</label>
                              <div class="editor-tools">
                                <select id="categoryPicker"></select>
                                <button type="button" class="secondary" onclick="addSelectedCategory()">선택 추가</button>
                              </div>
                              <div id="selectedCategories" class="selected-categories">
                                <span class="muted">선택된 카테고리가 없습니다.</span>
                              </div>
                              <label>Images</label>
                              <div class="editor-tools">
                                <input id="postImages" name="images" type="file" accept="image/*" multiple>
                                <button type="button" class="secondary" onclick="insertSelectedImages()">본문에 이미지 삽입</button>
                              </div>
                              <label>Content</label>
                              <textarea name="content" required></textarea>
                              <p id="postStatus" class="status"></p>
                              <div class="actions">
                                <button type="submit">저장</button>
                                <button type="button" class="secondary" onclick="resetPostForm()">새 글로 초기화</button>
                              </div>
                            </form>
                          </div>
                          <div class="panel">
                            <h2>기존 게시글 조회</h2>
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
                    let categoriesCache = [];
                    let selectedPostCategories = [];
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
                        if (button.dataset.tab === 'posts') loadPosts();
                        if (button.dataset.tab === 'categories') loadCategories();
                        if (button.dataset.tab === 'comments') loadComments(0);
                        if (button.dataset.tab === 'logs') { loadAccessLogs(0); loadLoginLogs(0); }
                      });
                    });

                    document.getElementById('postForm').addEventListener('submit', async (event) => {
                      event.preventDefault();
                      const form = new FormData(event.target);
                      form.delete('categories');
                      selectedPostCategories.forEach(category => form.append('categories', category));
                      if (selectedPostCategories.length === 0) {
                        document.getElementById('postStatus').textContent = '카테고리를 최소 1개 선택하세요.';
                        return;
                      }
                      const postId = form.get('postId');
                      const method = postId ? 'PUT' : 'POST';
                      const url = postId ? '/api/admin/posts/' + postId : '/api/admin/posts';
                      const response = await fetch(url, { method, body: form });
                      if (response.ok) {
                        document.getElementById('postStatus').textContent = '게시글이 저장되었습니다.';
                        resetPostForm();
                        loadPosts();
                      } else {
                        document.getElementById('postStatus').textContent = await errorMessage(response, '게시글 저장에 실패했습니다.');
                      }
                    });

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
                      const form = document.getElementById('postForm');
                      form.postId.value = post.id;
                      form.title.value = post.title;
                      form.content.value = post.content;
                      selectedPostCategories = post.tags.filter(tag => categoriesCache.some(category => category.name === tag));
                      renderSelectedCategories();
                      document.getElementById('postFormTitle').textContent = '게시글 수정';
                      document.getElementById('postStatus').textContent = '수정할 내용을 입력하세요. 새 이미지를 선택하면 기존 이미지가 교체됩니다.';
                      window.scrollTo({ top: 0, behavior: 'smooth' });
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
                      document.getElementById('categoryPicker').innerHTML = '<option value="">카테고리 선택</option>' + categoriesCache
                        .map(category => `<option value="${escapeAttr(category.name)}">${escapeHtml(category.name)}</option>`)
                        .join('');
                      renderSelectedCategories();
                    }

                    function addSelectedCategory() {
                      const picker = document.getElementById('categoryPicker');
                      const value = picker.value;
                      if (!value) return;
                      if (!selectedPostCategories.includes(value)) {
                        selectedPostCategories.push(value);
                        renderSelectedCategories();
                      }
                      picker.value = '';
                    }

                    function removeSelectedCategory(categoryName) {
                      selectedPostCategories = selectedPostCategories.filter(name => name !== categoryName);
                      renderSelectedCategories();
                    }

                    function renderSelectedCategories() {
                      const box = document.getElementById('selectedCategories');
                      if (!selectedPostCategories.length) {
                        box.innerHTML = '<span class="muted">선택된 카테고리가 없습니다.</span>';
                        return;
                      }
                      box.innerHTML = selectedPostCategories
                        .map(category => `<span class="chip">${escapeHtml(category)} <button type="button" onclick="removeSelectedCategory('${escapeAttr(category)}')">×</button></span>`)
                        .join('');
                    }

                    function insertSelectedImages() {
                      const input = document.getElementById('postImages');
                      const textarea = document.getElementById('postForm').content;
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
                        selectedPostCategories = selectedPostCategories.filter(name => categoriesCache.find(category => category.id === id)?.name !== name);
                        await loadCategories();
                        document.getElementById('categoryStatus').textContent = '카테고리가 삭제되었습니다.';
                      } else {
                        const message = await errorMessage(response, '존재하지 않는 카테고리입니다.');
                        document.getElementById('categoryStatus').textContent = message;
                        alert(message);
                      }
                    }

                    function resetPostForm() {
                      document.getElementById('postForm').reset();
                      document.getElementById('postForm').postId.value = '';
                      document.getElementById('postFormTitle').textContent = '새로운 게시글 작성';
                      selectedPostCategories = [];
                      renderSelectedCategories();
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

                    loadCategories();
                    loadPosts();
                  </script>
                </body>
                </html>
                """.replace("{{ADMIN_ID}}", adminId)
                .replace("{{FRONTEND_ORIGIN}}", frontendOrigin);
    }
}
