import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Loader2, FileText } from "lucide-react";
import { motion } from "motion/react";
import { PostCard } from "../components/blog/PostCard";
import { Category, fetchCategories, fetchPosts } from "../lib/api";
import { Post } from "../types/blog";

const POSTS_PER_PAGE = 5;

export function HomePage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [searchParams] = useSearchParams();
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const searchQuery = searchParams.get("q") || "";

  useEffect(() => {
    fetchCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  const loadPosts = useCallback(async (nextPage: number, replace = false) => {
    if (replace) {
      setLoading(true);
    } else {
      setLoadingMore(true);
    }
    setError("");

    try {
      const data = await fetchPosts({
        page: nextPage,
        size: POSTS_PER_PAGE,
        category: selectedCategory,
        q: searchQuery,
      });
      setPosts(prev => replace ? data.content : [...prev, ...data.content]);
      setPage(data.page);
      setLast(data.last);
    } catch (err) {
      setError("게시글을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [selectedCategory, searchQuery]);

  useEffect(() => {
    setPosts([]);
    setLast(false);
    loadPosts(0, true);
  }, [loadPosts]);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel || last || loading || loadingMore) return;

    const observer = new IntersectionObserver(entries => {
      if (entries[0]?.isIntersecting && !last && !loadingMore) {
        loadPosts(page + 1);
      }
    }, { rootMargin: "240px" });

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [last, loading, loadingMore, loadPosts, page]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-[70vh] text-slate-400">
        <Loader2 className="w-10 h-10 animate-spin mb-4" />
        <p className="font-semibold">게시글을 불러오는 중입니다.</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-12 max-w-5xl">
      <section className="mb-16 text-center">
        <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-slate-900 mb-4">
          Tech Insights & <span className="text-blue-600">Journey</span>
        </h1>
        <p className="text-slate-500 text-lg max-w-2xl mx-auto">
          개발 경험과 지식을 기록합니다. 스크롤하면 5개씩 이어서 불러옵니다.
        </p>
      </section>

      <section className="mb-12 flex flex-wrap justify-center gap-2 items-center">
        <button
          onClick={() => setSelectedCategory(null)}
          className={`px-5 py-2 rounded-full text-sm font-bold transition-all border ${
            selectedCategory === null
              ? "bg-slate-900 text-white border-slate-900 shadow-lg shadow-slate-200"
              : "bg-white text-slate-600 border-slate-100 hover:border-slate-300"
          }`}
        >
          전체
        </button>
        {categories.map((cat) => (
          <button
            key={cat.id}
            onClick={() => setSelectedCategory(cat.name)}
            className={`px-5 py-2 rounded-full text-sm font-bold transition-all border ${
              selectedCategory === cat.name
                ? "bg-slate-900 text-white border-slate-900 shadow-lg shadow-slate-200"
                : "bg-white text-slate-600 border-slate-100 hover:border-slate-300"
            }`}
          >
            {cat.name}
          </button>
        ))}
      </section>

      {error && <p className="mb-8 text-center text-sm font-bold text-red-500">{error}</p>}

      {posts.length > 0 ? (
        <div className="space-y-12">
          {posts.map((post, idx) => (
            <motion.div
              key={post.id}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: Math.min(idx, POSTS_PER_PAGE - 1) * 0.05 }}
            >
              <PostCard post={post} featured={idx === 0} />
            </motion.div>
          ))}

          <div ref={sentinelRef} className="h-12 flex items-center justify-center">
            {loadingMore && <Loader2 className="w-6 h-6 animate-spin text-slate-300" />}
            {last && posts.length > 0 && (
              <span className="text-xs font-bold uppercase tracking-widest text-slate-300">End of posts</span>
            )}
          </div>
        </div>
      ) : (
        <div className="py-24 text-center border-2 border-dashed border-slate-100 rounded-3xl">
          <FileText className="w-16 h-16 text-slate-200 mx-auto mb-4" />
          <p className="text-slate-400 font-medium">아직 작성된 포스트가 없습니다.</p>
          {selectedCategory !== null && (
            <button
              onClick={() => setSelectedCategory(null)}
              className="mt-4 text-blue-600 font-bold hover:underline"
            >
              모든 포스트 보기
            </button>
          )}
        </div>
      )}
    </div>
  );
}
