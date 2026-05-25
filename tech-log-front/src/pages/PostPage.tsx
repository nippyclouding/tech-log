import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import { format } from "date-fns";
import { ArrowLeft, Loader2 } from "lucide-react";
import { motion, useScroll, useSpring } from "motion/react";
import { Post } from "../types/blog";
import { CommentSection } from "../components/blog/CommentSection";
import { fetchPost, fetchPosts } from "../lib/api";

export function PostPage() {
  const { id } = useParams();
  const [post, setPost] = useState<Post | null>(null);
  const [relatedPosts, setRelatedPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  const { scrollYProgress } = useScroll();
  const scaleX = useSpring(scrollYProgress, {
    stiffness: 100,
    damping: 30,
    restDelta: 0.001
  });

  useEffect(() => {
    window.scrollTo(0, 0);
    if (!id) return;

    const loadPost = async () => {
      setLoading(true);
      try {
        const data = await fetchPost(id);
        setPost(data);

        const related = await fetchPosts({ page: 0, size: 4, category: data.category });
        setRelatedPosts(related.content.filter(item => String(item.id) !== String(id)).slice(0, 3));
      } catch (err) {
        setPost(null);
      } finally {
        setLoading(false);
      }
    };

    loadPost();
  }, [id]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-[70vh] text-slate-400">
        <Loader2 className="w-10 h-10 animate-spin mb-4" />
        <p className="font-semibold">게시글을 불러오는 중입니다.</p>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="container mx-auto px-4 py-24 text-center">
        <h1 className="text-3xl font-bold text-slate-900 mb-4">Post Not Found</h1>
        <p className="text-slate-500 mb-8">찾으시는 포스트가 존재하지 않거나 삭제되었을 수 있습니다.</p>
        <Link to="/" className="text-blue-600 font-bold hover:underline">홈으로 돌아가기</Link>
      </div>
    );
  }

  const coverImage = post.coverImage || "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop";

  return (
    <article className="pb-24 bg-white">
      <motion.div
        className="fixed top-16 left-0 right-0 h-1 bg-blue-600 origin-left z-50 shadow-[0_0_10px_rgba(37,99,235,0.5)]"
        style={{ scaleX }}
      />

      <header className="relative h-[60vh] min-h-[460px] w-full overflow-hidden">
        <img
          src={coverImage}
          alt={post.title}
          className="h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-900/40 to-transparent" />
        <div className="absolute inset-0 flex items-end justify-center pb-16 px-4">
          <div className="container max-w-4xl">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
            >
              <div className="flex items-center space-x-3 mb-6">
                <span className="rounded-full bg-blue-600 px-3 py-1 text-xs font-semibold text-white uppercase tracking-wider">
                  {post.category}
                </span>
              </div>
              <h1 className="text-4xl md:text-6xl font-bold text-white tracking-tight leading-tight mb-8">
                {post.title}
              </h1>
              <div className="flex items-center space-x-4">
                {post.author.avatar ? (
                  <img
                    src={post.author.avatar}
                    alt={post.author.name}
                    className="h-14 w-14 rounded-full border-2 border-white/20 object-cover"
                  />
                ) : (
                  <div className="h-14 w-14 rounded-full border-2 border-white/20 bg-white/10 flex items-center justify-center text-white font-bold">
                    {post.author.name.charAt(0)}
                  </div>
                )}
                <div className="flex flex-col">
                  <span className="text-white font-semibold text-lg">{post.author.name}</span>
                  <span className="text-slate-400 text-sm font-medium">{format(new Date(post.date), "yyyy.MM.dd")}</span>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </header>

      <div className="container max-w-6xl mx-auto px-4 mt-16 flex flex-col lg:grid lg:grid-cols-[1fr_280px] gap-16">
        <div className="max-w-3xl">
          <div className="prose prose-lg prose-slate max-w-none prose-headings:text-slate-900 prose-p:text-slate-700 prose-a:text-blue-600 prose-img:rounded-3xl prose-pre:bg-slate-900 prose-pre:shadow-2xl prose-pre:rounded-2xl">
            <div className="markdown-body">
              <ReactMarkdown>{post.content}</ReactMarkdown>
            </div>
          </div>

          <div className="mt-16 pt-8 border-t border-slate-100 flex flex-wrap gap-2">
            {post.tags.map(tag => (
              <span key={tag} className="px-4 py-2 rounded-full bg-slate-100 text-slate-600 text-sm font-bold">
                #{tag}
              </span>
            ))}
          </div>

          <CommentSection postId={post.id} />
        </div>

        <aside className="hidden lg:block">
          <div className="sticky top-32 space-y-12">
            {relatedPosts.length > 0 && (
              <div className="space-y-6">
                <h5 className="text-[10px] font-bold text-slate-400 uppercase tracking-[0.2em]">Related Posts</h5>
                <div className="space-y-6">
                  {relatedPosts.map(p => (
                    <Link key={p.id} to={`/post/${p.id}`} className="group block">
                      <p className="text-sm font-bold text-slate-900 group-hover:text-blue-600 leading-snug line-clamp-2 transition-colors mb-2">
                        {p.title}
                      </p>
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] text-blue-600 font-bold uppercase tracking-wider">{p.category}</span>
                        <span className="text-[10px] text-slate-400 font-medium">{format(new Date(p.date), "MMM d, yyyy")}</span>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            )}
          </div>
        </aside>
      </div>

      <div className="container max-w-4xl mx-auto px-4 mt-24 text-center">
        <Link
          to="/"
          className="inline-flex items-center text-slate-500 hover:text-slate-900 font-bold transition-colors group"
        >
          <ArrowLeft className="mr-2 h-4 w-4 transition-transform group-hover:-translate-x-1" /> View all articles
        </Link>
      </div>
    </article>
  );
}
