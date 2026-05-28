import { ChangeEvent, FormEvent, ReactNode, useEffect, useRef, useState } from "react";
import { Category, CurrentUser, fetchCategories, fetchCurrentUser, fetchPost, fetchPosts, PageResponse } from "../lib/api";
import { FormattedMarkdown } from "../components/blog/FormattedMarkdown";
import {
  AccessLog,
  AdminComment,
  LoginLog,
  adminLogin,
  adminLogout,
  deleteCategory,
  deleteComment,
  deletePost,
  fetchAccessLogs,
  fetchAdminComments,
  fetchLoginLogs,
  saveCategory,
  savePost,
} from "../lib/adminApi";
import { Post } from "../types/blog";

type Tab = "posts" | "categories" | "comments" | "logs";
type PostTab = "create" | "manage";

const EMPTY_PAGE = <T,>(): PageResponse<T> => ({
  content: [],
  page: 0,
  size: 0,
  totalElements: 0,
  totalPages: 0,
  last: true,
});

export function AdminPage() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [checking, setChecking] = useState(true);

  const refreshUser = async () => {
    const current = await fetchCurrentUser();
    setUser(current);
    return current;
  };

  useEffect(() => {
    refreshUser().finally(() => setChecking(false));
  }, []);

  if (checking) {
    return <CenteredMessage>관리자 인증을 확인하는 중입니다.</CenteredMessage>;
  }
  if (!user?.authenticated || !user.admin) {
    return <AdminLogin onLogin={async () => setUser(await refreshUser())} />;
  }
  return <AdminConsole adminId={user.name ?? "admin"} onLogout={() => setUser(null)} />;
}

function AdminLogin({ onLogin }: { onLogin: () => Promise<void> }) {
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSubmitting(true);
    setError("");
    try {
      await adminLogin(String(form.get("adminId") ?? ""), String(form.get("adminPassword") ?? ""));
      await onLogin();
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 grid place-items-center px-6">
      <form onSubmit={submit} className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/50">
        <h1 className="mb-6 text-2xl font-bold text-slate-900">Admin Console</h1>
        {error && <p className="mb-4 text-sm font-semibold text-red-600">{error}</p>}
        <FieldLabel htmlFor="adminId">ID</FieldLabel>
        <input id="adminId" name="adminId" required autoComplete="username" className={inputClass} />
        <FieldLabel htmlFor="adminPassword">Password</FieldLabel>
        <input id="adminPassword" name="adminPassword" type="password" required autoComplete="current-password" className={inputClass} />
        <button disabled={submitting} className="mt-6 w-full rounded-lg bg-slate-900 px-4 py-3 font-bold text-white disabled:opacity-50">
          {submitting ? "확인 중..." : "Login"}
        </button>
      </form>
    </div>
  );
}

function AdminConsole({ adminId, onLogout }: { adminId: string; onLogout: () => void }) {
  const [tab, setTab] = useState<Tab>("posts");
  const [postTab, setPostTab] = useState<PostTab>("create");
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryError, setCategoryError] = useState("");

  const loadCategories = async () => {
    try {
      setCategories(await fetchCategories());
      setCategoryError("");
    } catch (error) {
      setCategoryError(errorText(error));
    }
  };

  useEffect(() => {
    loadCategories();
  }, []);

  const logout = async () => {
    await adminLogout();
    onLogout();
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 lg:grid lg:grid-cols-[230px_1fr]">
      <aside className="bg-slate-900 p-5 text-white">
        <div className="mb-6 border-b border-slate-700 pb-5">
          <strong className="block text-lg">Tech Log</strong>
          <span className="text-xs text-slate-400">{adminId}</span>
        </div>
        <nav className="flex gap-2 lg:block lg:space-y-1">
          {([["posts", "게시글"], ["categories", "카테고리"], ["comments", "댓글"], ["logs", "로그"]] as [Tab, string][]).map(([key, label]) => (
            <button key={key} onClick={() => setTab(key)} className={navClass(tab === key)}>{label}</button>
          ))}
        </nav>
        <button onClick={logout} className="mt-7 rounded-lg border border-slate-700 px-4 py-2 text-sm font-bold hover:bg-slate-800 lg:w-full">Logout</button>
      </aside>
      <main className="min-w-0 p-5 md:p-8">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">{tabLabel(tab)}</h1>
            <p className="mt-1 text-sm text-slate-500">게시글, 카테고리, 댓글, 로그를 관리합니다.</p>
          </div>
          <div className="flex items-center gap-3">
            {tab === "posts" && (
              <div className="flex rounded-lg border border-slate-200 bg-white p-1">
                <SubnavButton active={postTab === "create"} onClick={() => setPostTab("create")}>게시글 작성</SubnavButton>
                <SubnavButton active={postTab === "manage"} onClick={() => setPostTab("manage")}>게시글 편집</SubnavButton>
              </div>
            )}
            <a href="/" target="_blank" rel="noreferrer" className={buttonClass}>사이트 보기</a>
          </div>
        </div>
        {tab === "posts" && postTab === "create" && <PostEditor categories={categories} onSaved={() => setPostTab("manage")} draft />}
        {tab === "posts" && postTab === "manage" && <PostManager categories={categories} />}
        {tab === "categories" && <CategoryManager categories={categories} error={categoryError} reload={loadCategories} />}
        {tab === "comments" && <CommentManager />}
        {tab === "logs" && <LogManager />}
      </main>
    </div>
  );
}

function PostEditor({ categories, post, onSaved, onCancel, draft = false }: {
  categories: Category[];
  post?: Post;
  onSaved: () => void;
  onCancel?: () => void;
  draft?: boolean;
}) {
  const draftKey = "tech-log.admin.post-draft";
  const [title, setTitle] = useState(post?.title ?? "");
  const [content, setContent] = useState(post?.content ?? "");
  const [selectedCategories, setSelectedCategories] = useState<string[]>(post?.tags ?? []);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [preview, setPreview] = useState(false);
  const [status, setStatus] = useState("");
  const [draftState, setDraftState] = useState(draft ? "임시 저장 대기" : "");
  const textarea = useRef<HTMLTextAreaElement>(null);
  const imageInput = useRef<HTMLInputElement>(null);
  const selection = useRef({ start: content.length, end: content.length });

  useEffect(() => {
    if (!draft || post) return;
    try {
      const stored = localStorage.getItem(draftKey);
      if (!stored) return;
      const saved = JSON.parse(stored);
      const savedContent = saved.content ?? "";
      setTitle(saved.title ?? "");
      setContent(savedContent);
      setSelectedCategories(Array.isArray(saved.categories) ? saved.categories : []);
      selection.current = { start: savedContent.length, end: savedContent.length };
      setDraftState("임시 저장 복원됨");
    } catch {
      setDraftState("임시 저장 불가");
    }
  }, [draft, post]);

  useEffect(() => {
    if (!categories.length) return;
    setSelectedCategories(current => current.filter(name => categories.some(category => category.name === name)));
  }, [categories]);

  const saveDraft = (nextTitle: string, nextContent: string, nextCategories: string[]) => {
    if (!draft) return;
    try {
      localStorage.setItem(draftKey, JSON.stringify({ title: nextTitle, content: nextContent, categories: nextCategories }));
      setDraftState("임시 저장됨");
    } catch {
      setDraftState("임시 저장 불가");
    }
  };

  const updateTitle = (value: string) => {
    setTitle(value);
    saveDraft(value, content, selectedCategories);
  };
  const updateContent = (value: string) => {
    setContent(value);
    saveDraft(title, value, selectedCategories);
  };
  const addCategory = () => {
    if (!selectedCategory || selectedCategories.includes(selectedCategory)) return;
    const next = [...selectedCategories, selectedCategory];
    setSelectedCategories(next);
    setSelectedCategory("");
    saveDraft(title, content, next);
  };
  const removeCategory = (category: string) => {
    const next = selectedCategories.filter(item => item !== category);
    setSelectedCategories(next);
    saveDraft(title, content, next);
  };

  const rememberSelection = (element: HTMLTextAreaElement) => {
    selection.current = { start: element.selectionStart, end: element.selectionEnd };
  };

  const focusTextareaAt = (position: number) => {
    requestAnimationFrame(() => {
      const element = textarea.current;
      if (!element) return;
      element.focus();
      element.setSelectionRange(position, position);
      selection.current = { start: position, end: position };
    });
  };

  const insert = (format: string) => {
    const { start, end } = selection.current;
    const selected = content.slice(start, end);
    const replacements: Record<string, string> = {
      heading: `## ${selected || "소제목"}`,
      bold: `**${selected || "강조할 내용"}**`,
      italic: `*${selected || "기울임 텍스트"}*`,
      underline: `[${selected || "밑줄 텍스트"}](underline:)`,
      quote: (selected || "인용문").split("\n").map(line => `> ${line}`).join("\n"),
      list: (selected || "목록 항목").split("\n").map(line => `- ${line}`).join("\n"),
      code: `\n\n\`\`\`\n${selected || "코드를 입력하세요"}\n\`\`\`\n\n`,
      link: `[${selected || "링크 텍스트"}](https://)`,
      divider: "\n\n---\n\n",
      alignLeft: `\n\n[align=left]\n${selected || "왼쪽 정렬할 문단"}\n[/align]\n\n`,
      alignCenter: `\n\n[align=center]\n${selected || "가운데 정렬할 문단"}\n[/align]\n\n`,
      alignRight: `\n\n[align=right]\n${selected || "오른쪽 정렬할 문단"}\n[/align]\n\n`,
      lineBreak: `${selected}  \n`,
    };
    const replacement = replacements[format];
    const next = content.slice(0, start) + replacement + content.slice(end);
    updateContent(next);
    setPreview(false);
    focusTextareaAt(start + replacement.length);
  };

  const insertImages = () => {
    const nextIndex = files.findIndex((_, index) => !hasPendingImage(content, index));
    if (nextIndex < 0) {
      setStatus("먼저 이미지를 선택하세요.");
      return;
    }
    insertImage(nextIndex);
  };

  const insertImage = (index: number) => {
    const file = files[index];
    if (!file) {
      setStatus("삽입할 이미지 파일을 찾지 못했습니다.");
      return;
    }
    if (hasPendingImage(content, index)) {
      setStatus("이미 본문에 삽입된 이미지입니다.");
      return;
    }
    const placeholder = `\n\n![${safeImageAlt(file.name, index)}](pending-image:${index})\n\n`;
    const position = selection.current.start;
    const nextContent = content.slice(0, position) + placeholder + content.slice(position);
    updateContent(nextContent);
    setPreview(false);
    setStatus(`${file.name} 이미지 위치를 본문에 삽입했습니다.`);
    focusTextareaAt(position + placeholder.length);
  };

  const selectImages = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(event.target.files ?? []);
    if (!selected.length) return;
    setFiles(current => [...current, ...selected]);
    setStatus(`${selected.length}개 파일을 추가했습니다. Image 버튼을 눌러 본문에 배치하세요.`);
    event.target.value = "";
  };

  const reset = () => {
    setTitle("");
    setContent("");
    setSelectedCategories([]);
    setFiles([]);
    setPreview(false);
    setStatus("");
    if (imageInput.current) imageInput.current.value = "";
    if (draft) {
      localStorage.removeItem(draftKey);
      setDraftState("임시 저장 대기");
    }
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedCategories.length) {
      setStatus("카테고리를 최소 1개 선택하세요.");
      return;
    }
    const form = new FormData();
    const preparedImages = prepareImagesForSubmit(content, files);
    if (preparedImages.error) {
      setStatus(preparedImages.error);
      return;
    }
    form.set("title", title);
    form.set("content", preparedImages.content);
    selectedCategories.forEach(category => form.append("categories", category));
    preparedImages.files.forEach(file => form.append("images", file));
    try {
      await savePost(post?.id ?? null, form);
      setStatus("게시글이 저장되었습니다.");
      if (draft) reset();
      onSaved();
    } catch (error) {
      setStatus(errorText(error));
    }
  };

  const words = content.trim() ? content.trim().split(/\s+/).length : 0;

  return (
    <form onSubmit={submit} className="mx-auto max-w-7xl overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-slate-100 px-7 py-5">
        <div>
          <h2 className="text-xl font-bold">{post ? "게시글 수정" : "새 글 쓰기"}</h2>
          <p className="text-sm text-slate-500">마크다운으로 작성하고 발행 전 모습을 확인할 수 있습니다.</p>
        </div>
        {draft && <span className="rounded-full bg-blue-50 px-3 py-2 text-xs font-bold text-blue-600">{draftState}</span>}
      </div>
      <div className="border-b border-slate-100 px-7 py-4">
        <input value={title} onChange={event => updateTitle(event.target.value)} maxLength={255} required placeholder="제목을 입력하세요" className="w-full border-0 py-3 text-3xl font-bold outline-none placeholder:text-slate-300" />
        <p className="text-right text-xs text-slate-500">{title.length} / 255</p>
      </div>
      <div className="grid md:grid-cols-[1fr_300px]">
        <div className="min-w-0 border-r border-slate-100">
          <div className="flex flex-wrap gap-1 border-b border-slate-200 p-3">
            {[["heading", "H2"], ["bold", "Bold"], ["italic", "Italic"], ["underline", "Underline"], ["quote", "Quote"], ["list", "List"], ["code", "Code"], ["link", "Link"], ["divider", "HR"], ["alignLeft", "Left"], ["alignCenter", "Center"], ["alignRight", "Right"], ["lineBreak", "BR"]].map(([key, label]) => (
              <button type="button" key={key} onMouseDown={event => event.preventDefault()} onClick={() => insert(key)} className={toolButtonClass}>{label}</button>
            ))}
            <button type="button" onMouseDown={event => event.preventDefault()} onClick={insertImages} className={toolButtonClass}>Image</button>
            <div className="ml-auto flex gap-1">
              <SubnavButton active={!preview} onClick={() => setPreview(false)}>편집</SubnavButton>
              <SubnavButton active={preview} onClick={() => setPreview(true)}>미리보기</SubnavButton>
            </div>
          </div>
          {!preview ? (
            <textarea
              ref={textarea}
              value={content}
              onChange={event => {
                updateContent(event.target.value);
                rememberSelection(event.target);
              }}
              onSelect={event => rememberSelection(event.currentTarget)}
              required
              placeholder="본문을 마크다운으로 입력하세요."
              className="min-h-[520px] w-full resize-y border-0 p-8 text-base leading-8 outline-none"
            />
          ) : (
            <article className="markdown-body min-h-[520px] p-8">
              {content ? <FormattedMarkdown content={content} allowPendingImages imageComponent={PreviewImage} /> : <p>작성한 글의 미리보기가 여기에 표시됩니다.</p>}
            </article>
          )}
        </div>
        <aside className="bg-slate-50/50 p-5">
          <h3 className="mb-3 font-bold">카테고리</h3>
          <div className="flex gap-2">
            <select value={selectedCategory} onChange={event => setSelectedCategory(event.target.value)} className={inputClass}>
              <option value="">카테고리 선택</option>
              {categories.map(category => <option key={category.id} value={category.name}>{category.name}</option>)}
            </select>
            <button type="button" onClick={addCategory} className={secondaryButtonClass}>선택</button>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {selectedCategories.length ? selectedCategories.map(category => (
              <span key={category} className="rounded-full bg-blue-50 px-3 py-1 text-sm text-blue-700">
                {category} <button type="button" onClick={() => removeCategory(category)} className="ml-1 font-bold">x</button>
              </span>
            )) : <p className="text-sm text-slate-500">선택된 카테고리가 없습니다.</p>}
          </div>
          <hr className="my-6 border-slate-200" />
          <h3 className="mb-3 font-bold">이미지</h3>
          <input ref={imageInput} type="file" accept="image/*" multiple onChange={selectImages} className="w-full rounded-lg border border-dashed border-slate-300 bg-white p-3 text-xs" />
          <p className="mt-2 text-xs leading-5 text-slate-500">파일을 추가한 뒤 Image 버튼으로 커서 위치에 배치하세요. 여러 번 선택해도 앞선 파일은 유지됩니다.</p>
          {files.length > 0 && (
            <ul className="mt-3 space-y-2 text-xs text-slate-600">
              {files.map((file, index) => {
                const inserted = hasPendingImage(content, index);
                return (
                  <li key={`${file.name}-${index}`} className="rounded-lg border border-slate-200 bg-white p-2">
                    <div className="font-semibold text-slate-700">{index + 1}. {file.name}</div>
                    <div className="mt-2 flex items-center justify-between gap-2">
                      <span className={inserted ? "text-blue-600" : "text-slate-400"}>{inserted ? "본문 삽입됨" : "미삽입"}</span>
                      <button type="button" onClick={() => insertImage(index)} disabled={inserted} className="rounded-md border border-slate-200 px-2 py-1 font-bold disabled:cursor-not-allowed disabled:opacity-40">
                        삽입
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
          <hr className="my-6 border-slate-200" />
          <p className="text-xs text-slate-500">본문 {content.length}자 · 단어 {words}개</p>
          {status && <p className="mt-4 text-sm font-semibold text-blue-700">{status}</p>}
          <div className="mt-5 grid gap-2">
            <button type="submit" className={buttonClass}>{post ? "수정 저장" : "게시글 발행"}</button>
            <button type="button" onClick={post ? onCancel : reset} className={secondaryButtonClass}>{post ? "닫기" : "내용 비우기"}</button>
          </div>
        </aside>
      </div>
    </form>
  );
}

function PreviewImage({ src, alt }: { src?: string; alt?: string }) {
  if (src?.startsWith("pending-image:")) {
    return <span className="block rounded-lg border border-dashed border-blue-200 bg-blue-50 p-6 text-center text-blue-600">업로드 예정 이미지 · {alt}</span>;
  }
  return <img src={src} alt={alt ?? ""} className="my-5 max-w-full rounded-xl" />;
}

function hasPendingImage(content: string, index: number) {
  return new RegExp(`pending-image:${index}(?!\\d)`).test(content);
}

function safeImageAlt(filename: string, index: number) {
  const extensionRemoved = filename.replace(/\.[^.]+$/, "").trim();
  const normalized = extensionRemoved.replace(/[[\]()]/g, "").replace(/\s+/g, " ").trim();
  return normalized || `이미지 ${index + 1}`;
}

function prepareImagesForSubmit(content: string, files: File[]) {
  const references = [...content.matchAll(/pending-image:(\d+)(?!\d)/g)].map(match => Number(match[1]));
  const orderedReferences = Array.from(new Set(references));
  const missing = orderedReferences.find(index => !Number.isInteger(index) || index < 0 || index >= files.length);
  if (missing !== undefined) {
    return {
      content,
      files: [],
      error: "본문에 배치한 이미지 파일을 다시 선택하세요.",
    };
  }

  const indexMap = new Map<number, number>();
  orderedReferences.forEach((oldIndex, newIndex) => indexMap.set(oldIndex, newIndex));
  const remappedContent = content.replace(/pending-image:(\d+)(?!\d)/g, (_match, indexText: string) => {
    const nextIndex = indexMap.get(Number(indexText));
    return `pending-image:${nextIndex}`;
  });

  return {
    content: remappedContent,
    files: orderedReferences.map(index => files[index]),
    error: "",
  };
}

function PostManager({ categories }: { categories: Category[] }) {
  const [data, setData] = useState<PageResponse<Post>>(EMPTY_PAGE);
  const [editing, setEditing] = useState<Post | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const load = async (page = 0) => {
    if (page < 0) return;
    setLoading(true);
    try {
      setData(await fetchPosts({ page, size: 20 }));
      setError("");
    } catch (loadError) {
      setError(errorText(loadError));
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  const edit = async (id: string | number) => {
    try {
      setEditing(await fetchPost(id));
    } catch (loadError) {
      setError(errorText(loadError));
    }
  };
  const remove = async (id: string | number) => {
    if (!window.confirm("게시글을 삭제할까요?")) return;
    try {
      await deletePost(id);
      await load(data.page);
    } catch (deleteError) {
      setError(errorText(deleteError));
    }
  };

  return (
    <div className="space-y-5">
      {editing && <PostEditor key={String(editing.id)} categories={categories} post={editing} onSaved={() => { setEditing(null); load(data.page); }} onCancel={() => setEditing(null)} />}
      <Panel title="게시글 편집">
        {error && <StatusError>{error}</StatusError>}
        {loading ? <EmptyText>게시글 목록을 불러오는 중입니다.</EmptyText> : data.content.length ? data.content.map(post => (
          <ListRow key={post.id}>
            <div><strong>{post.title}</strong><p className="text-sm text-slate-500">{post.category} · {post.date}</p></div>
            <div className="flex gap-2">
              <button onClick={() => edit(post.id)} className={secondaryButtonClass}>수정</button>
              <button onClick={() => remove(post.id)} className={dangerButtonClass}>삭제</button>
            </div>
          </ListRow>
        )) : <EmptyText>게시글이 없습니다.</EmptyText>}
        {!loading && !error && <Pager data={data} onMove={load} />}
      </Panel>
    </div>
  );
}

function CategoryManager({ categories, error, reload }: { categories: Category[]; error: string; reload: () => Promise<void> }) {
  const [editing, setEditing] = useState<Category | null>(null);
  const [name, setName] = useState("");
  const [status, setStatus] = useState(error);

  useEffect(() => setStatus(error), [error]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await saveCategory(editing?.id ?? null, name);
      setStatus("카테고리가 저장되었습니다.");
      setEditing(null);
      setName("");
      await reload();
    } catch (saveError) {
      setStatus(errorText(saveError));
    }
  };
  const remove = async (id: number) => {
    if (!window.confirm("카테고리를 삭제할까요? 이미 사용 중이면 실패할 수 있습니다.")) return;
    try {
      await deleteCategory(id);
      setStatus("카테고리가 삭제되었습니다.");
      await reload();
    } catch (deleteError) {
      setStatus(errorText(deleteError));
    }
  };

  return (
    <div className="grid gap-5 md:grid-cols-[360px_1fr]">
      <Panel title={editing ? "카테고리 수정" : "카테고리 생성"}>
        <form onSubmit={submit}>
          <FieldLabel htmlFor="categoryName">이름</FieldLabel>
          <input id="categoryName" value={name} onChange={event => setName(event.target.value)} required maxLength={100} className={inputClass} />
          {status && <p className="mt-3 text-sm text-blue-700">{status}</p>}
          <div className="mt-5 flex gap-2">
            <button type="submit" className={buttonClass}>저장</button>
            <button type="button" onClick={() => { setEditing(null); setName(""); }} className={secondaryButtonClass}>초기화</button>
          </div>
        </form>
      </Panel>
      <Panel title="카테고리 목록">
        {categories.map(category => (
          <ListRow key={category.id}>
            <div><strong>{category.name}</strong><p className="text-sm text-slate-500">/{category.slug}</p></div>
            <div className="flex gap-2">
              <button onClick={() => { setEditing(category); setName(category.name); }} className={secondaryButtonClass}>수정</button>
              <button onClick={() => remove(category.id)} className={dangerButtonClass}>삭제</button>
            </div>
          </ListRow>
        ))}
      </Panel>
    </div>
  );
}

function CommentManager() {
  const [data, setData] = useState<PageResponse<AdminComment>>(EMPTY_PAGE);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const load = async (page = 0) => {
    if (page < 0) return;
    setLoading(true);
    try {
      setData(await fetchAdminComments(page));
      setError("");
    } catch (loadError) {
      setError(errorText(loadError));
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  const remove = async (id: number) => {
    if (!window.confirm("댓글을 삭제 처리할까요?")) return;
    try {
      await deleteComment(id);
      await load(data.page);
    } catch (deleteError) {
      setError(errorText(deleteError));
    }
  };
  return (
    <Panel title="댓글 관리">
      {error && <StatusError>{error}</StatusError>}
      {loading ? <EmptyText>댓글 목록을 불러오는 중입니다.</EmptyText> : !error && <DataTable headers={["게시글", "작성자", "내용", "IP", "생성일", "작업"]}>
        {data.content.map(comment => (
          <tr key={comment.id}>
            <Cell>{comment.postTitle}</Cell><Cell>{comment.authorName}</Cell><Cell>{comment.content}</Cell><Cell mono>{comment.accessIp}</Cell><Cell>{comment.date}</Cell>
            <Cell><a className="mr-3 text-blue-600" href={`/post/${comment.postId}#comment-${comment.id}`} target="_blank" rel="noreferrer">열기</a><button onClick={() => remove(comment.id)} className="text-red-600">삭제</button></Cell>
          </tr>
        ))}
      </DataTable>}
      {!loading && !error && <Pager data={data} onMove={load} />}
    </Panel>
  );
}

function LogManager() {
  const [access, setAccess] = useState<PageResponse<AccessLog>>(EMPTY_PAGE);
  const [login, setLogin] = useState<PageResponse<LoginLog>>(EMPTY_PAGE);
  const [accessError, setAccessError] = useState("");
  const [loginError, setLoginError] = useState("");
  const [accessLoading, setAccessLoading] = useState(true);
  const [loginLoading, setLoginLoading] = useState(true);
  const loadAccess = async (page = 0) => {
    if (page < 0) return;
    setAccessLoading(true);
    try {
      setAccess(await fetchAccessLogs(page));
      setAccessError("");
    } catch (loadError) {
      setAccessError(errorText(loadError));
    } finally {
      setAccessLoading(false);
    }
  };
  const loadLogin = async (page = 0) => {
    if (page < 0) return;
    setLoginLoading(true);
    try {
      setLogin(await fetchLoginLogs(page));
      setLoginError("");
    } catch (loadError) {
      setLoginError(errorText(loadError));
    } finally {
      setLoginLoading(false);
    }
  };
  useEffect(() => { loadAccess(); loadLogin(); }, []);
  return (
    <div className="space-y-5">
      <Panel title="접근 로그" subtitle="서버 오류 요청은 request ID와 stack trace를 확인할 수 있습니다.">
        {accessError && <StatusError>{accessError}</StatusError>}
        {accessLoading ? <EmptyText>접근 로그를 불러오는 중입니다.</EmptyText> : !accessError && <DataTable headers={["IP", "Method", "Path", "Status", "Request ID", "Error", "Time"]}>
          {access.content.map(log => (
            <tr key={log.id}>
              <Cell mono>{log.ip}</Cell><Cell>{log.method}</Cell><Cell>{log.path}</Cell><Cell>{log.statusCode}</Cell><Cell mono>{log.requestId ?? "-"}</Cell>
              <Cell>{log.errorType ? <details><summary className="cursor-pointer text-red-600">{simpleType(log.errorType)}{log.errorMessage && `: ${log.errorMessage}`}</summary><pre className="mt-2 max-w-lg overflow-auto whitespace-pre-wrap rounded bg-slate-900 p-3 text-xs text-white">{log.stackTrace ?? "stack trace가 저장되지 않았습니다."}</pre></details> : "-"}</Cell>
              <Cell>{log.timestamp}</Cell>
            </tr>
          ))}
        </DataTable>}
        {!accessLoading && !accessError && <Pager data={access} onMove={loadAccess} />}
      </Panel>
      <Panel title="로그인 이력">
        {loginError && <StatusError>{loginError}</StatusError>}
        {loginLoading ? <EmptyText>로그인 이력을 불러오는 중입니다.</EmptyText> : !loginError && <DataTable headers={["Provider / Result", "Login ID", "IP", "Time"]}>
          {login.content.map(log => <tr key={log.id}><Cell>{log.provider}</Cell><Cell>{log.loginId}</Cell><Cell mono>{log.ip}</Cell><Cell>{log.timestamp}</Cell></tr>)}
        </DataTable>}
        {!loginLoading && !loginError && <Pager data={login} onMove={loadLogin} />}
      </Panel>
    </div>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="mb-1 text-lg font-bold">{title}</h2>{subtitle && <p className="mb-4 text-sm text-slate-500">{subtitle}</p>}{children}</section>;
}
function DataTable({ headers, children }: { headers: string[]; children: ReactNode }) {
  return <div className="overflow-x-auto"><table className="mt-4 w-full text-left text-sm"><thead><tr>{headers.map(header => <th key={header} className="border-b border-slate-100 p-3 text-xs uppercase text-slate-500">{header}</th>)}</tr></thead><tbody>{children}</tbody></table></div>;
}
function Cell({ children, mono = false }: { children: ReactNode; mono?: boolean }) {
  return <td className={`border-b border-slate-100 p-3 align-top ${mono ? "font-mono text-xs" : ""}`}>{children}</td>;
}
function Pager<T>({ data, onMove }: { data: PageResponse<T>; onMove: (page: number) => void }) {
  const currentPage = Number.isInteger(data.page) && data.page >= 0 ? data.page : 0;
  const totalPages = Number.isInteger(data.totalPages) && data.totalPages > 0 ? data.totalPages : 1;
  return <div className="mt-5 flex items-center justify-center gap-4"><button disabled={currentPage <= 0} onClick={() => onMove(currentPage - 1)} className={secondaryButtonClass}>이전</button><span className="text-sm text-slate-500">{currentPage + 1} / {totalPages}</span><button disabled={data.last || currentPage + 1 >= totalPages} onClick={() => onMove(currentPage + 1)} className={secondaryButtonClass}>다음</button></div>;
}
function ListRow({ children }: { children: ReactNode }) {
  return <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-4 last:border-0">{children}</div>;
}
function EmptyText({ children }: { children: ReactNode }) { return <p className="py-6 text-sm text-slate-500">{children}</p>; }
function StatusError({ children }: { children: ReactNode }) { return <p className="mb-4 text-sm font-semibold text-red-600">{children}</p>; }
function CenteredMessage({ children }: { children: ReactNode }) { return <div className="grid min-h-screen place-items-center bg-slate-50 text-slate-500">{children}</div>; }
function FieldLabel({ htmlFor, children }: { htmlFor: string; children: ReactNode }) { return <label htmlFor={htmlFor} className="mb-2 mt-4 block text-sm font-bold text-slate-600">{children}</label>; }
function SubnavButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) { return <button type="button" onClick={onClick} className={`rounded-md px-3 py-2 text-sm font-bold ${active ? "bg-slate-900 text-white" : "text-slate-500 hover:bg-slate-100"}`}>{children}</button>; }
function tabLabel(tab: Tab) { return ({ posts: "게시글", categories: "카테고리", comments: "댓글", logs: "로그" })[tab]; }
function navClass(active: boolean) { return `rounded-lg px-4 py-3 text-left text-sm font-bold lg:block lg:w-full ${active ? "bg-slate-800 text-white" : "text-slate-300 hover:bg-slate-800"}`; }
function simpleType(type: string) { return type.split(".").pop() ?? type; }
function errorText(error: unknown) { return error instanceof Error ? error.message : "요청 처리 중 오류가 발생했습니다."; }
const inputClass = "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-blue-500";
const buttonClass = "inline-flex items-center justify-center rounded-lg bg-slate-900 px-4 py-2 text-sm font-bold text-white hover:bg-slate-800 disabled:opacity-50";
const secondaryButtonClass = "inline-flex items-center justify-center rounded-lg bg-slate-200 px-4 py-2 text-sm font-bold text-slate-800 hover:bg-slate-300 disabled:opacity-40";
const dangerButtonClass = "inline-flex items-center justify-center rounded-lg bg-red-600 px-4 py-2 text-sm font-bold text-white hover:bg-red-700";
const toolButtonClass = "rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-600 hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700";
