import { Post } from "../../types/blog";
import { Link } from "react-router-dom";
import { format } from "date-fns";
import { motion } from "motion/react";
import { cn } from "../../lib/utils";
import { ArrowUpRight } from "lucide-react";

interface PostCardProps {
  post: Post;
  featured?: boolean;
}

export function PostCard({ post, featured }: PostCardProps) {
  const coverImage = post.coverImage || "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop";

  return (
    <motion.article 
      initial={{ opacity: 0, y: 30 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      className={cn(
        "group h-full flex flex-col bg-white rounded-[32px] border border-slate-100 transition-all duration-500 hover:shadow-[0_32px_64px_-16px_rgba(0,0,0,0.08)]",
        featured ? "lg:grid lg:grid-cols-[1.2fr_1fr] lg:min-h-[480px]" : ""
      )}
    >
      <Link 
        to={`/post/${post.id}`} 
        className={cn(
          "relative block overflow-hidden rounded-[24px] m-4", 
          featured ? "lg:m-6" : "aspect-[16/10]"
        )}
      >
        <img 
          src={coverImage}
          alt={post.title}
          className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-slate-900/10 group-hover:bg-slate-900/0 transition-colors duration-500" />
      </Link>

      <div className={cn(
        "flex flex-1 flex-col p-6 pt-2 pb-8",
        featured ? "lg:p-12 lg:pl-4 lg:justify-center" : ""
      )}>
        <div className="flex items-center space-x-2 text-xs font-bold uppercase tracking-widest text-blue-600 mb-4">
          <span>{post.category}</span>
          <span className="text-slate-300">•</span>
          <span className="text-slate-400">{format(new Date(post.date), "MMM d, yyyy")}</span>
        </div>

        <h3 className={cn(
          "font-bold text-slate-900 leading-tight group-hover:text-blue-700 transition-colors mb-4",
          featured ? "text-3xl md:text-5xl" : "text-xl md:text-2xl"
        )}>
          <Link to={`/post/${post.id}`} className="flex items-start justify-between">
            <span className="line-clamp-2">{post.title}</span>
            {featured && <ArrowUpRight className="w-8 h-8 opacity-0 group-hover:opacity-100 transition-all duration-500" />}
          </Link>
        </h3>

        <p className={cn(
          "text-slate-500 leading-relaxed mb-8 line-clamp-3",
          featured ? "text-lg md:text-xl" : "text-sm md:text-base"
        )}>
          {post.excerpt}
        </p>

        <div className="flex items-center justify-between mt-auto pt-6 border-t border-slate-50">
          <div className="flex items-center space-x-3">
            <div className="relative">
              {post.author.avatar ? (
                <img
                  src={post.author.avatar}
                  alt={post.author.name}
                  className="h-10 w-10 rounded-full border-2 border-white ring-1 ring-slate-100 object-cover"
                />
              ) : (
                <div className="h-10 w-10 rounded-full border-2 border-white ring-1 ring-slate-100 bg-slate-900 text-white flex items-center justify-center text-sm font-bold">
                  {post.author.name.charAt(0)}
                </div>
              )}
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-slate-900">{post.author.name}</span>
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400">{post.author.role}</span>
            </div>
          </div>
          
          <Link 
            to={`/post/${post.id}`}
            className="text-xs font-bold text-slate-900 uppercase tracking-widest hover:text-blue-600 transition-colors flex items-center md:hidden lg:flex"
          >
            Read More
          </Link>
        </div>
      </div>
    </motion.article>
  );
}
